package com.trajectory.cloud.ai.mq;

import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.trajectory.cloud.ai.factory.AiClientFactory;
import com.trajectory.cloud.ai.model.entity.Chart;
import com.trajectory.cloud.ai.service.AiAssistant;
import com.trajectory.cloud.ai.service.ChartService;
import com.trajectory.cloud.api.ai.model.dto.AiChatRequest;
import com.trajectory.cloud.api.ai.model.enums.AiModelTypeEnum;
import com.trajectory.cloud.api.ai.model.enums.ChartStatusEnum;
import com.trajectory.cloud.common.rabbitmq.constants.RabbitMqConstant;
import com.trajectory.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.trajectory.cloud.common.rabbitmq.model.RabbitMessage;
import com.trajectory.cloud.common.rabbitmq.model.event.AnalysisEvent;
import com.trajectory.cloud.common.rabbitmq.utils.MqSender;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * BI 图表分析消息队列消费者
 * 优化：集成 MQ + 线程池处理
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class BIChartConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AiClientFactory aiClientFactory;

    @Resource(name = "biThreadPoolExecutor")
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private MqSender mqSender;

    /**
     * 监听 BI 图表分析队列
     *
     * @param rabbitMessage RabbitMessage 对象
     * @param channel       RabbitMQ 通道
     * @param msg           Spring AMQP 消息对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.BI_CHART_QUEUE, durable = "true"), exchange = @Exchange(value = RabbitMqConstant.BI_CHART_EXCHANGE, type = "direct"), key = RabbitMqConstant.BI_CHART_ROUTING_KEY), ackMode = "MANUAL")
    public void handleBIChart(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        long deliveryTag = msg.getMessageProperties().getDeliveryTag();

        if (rabbitMessage == null || rabbitMessage.getMsgId() == null) {
            log.error("[BIChartConsumer] 消息为空或缺少msgId，拒绝消费");
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String msgId = rabbitMessage.getMsgId();
        Long chartId = JSONUtil.toBean(rabbitMessage.getMsgText(), Long.class);
        if (chartId == null) {
            log.error("[BIChartConsumer] 消息解析失败, msgId: {}", msgId);
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        log.info("[BIChartConsumer] 收到图表分析任务, chartId: {}", chartId);

        // 提交到线程池执行
        threadPoolExecutor.execute(() -> {
            try {
                processAnalysis(chartId);
                // 确认消息
                channel.basicAck(deliveryTag, false);
            } catch (Exception e) {
                log.error("[BIChartConsumer] 线程池处理任务失败, chartId: {}", chartId, e);
                try {
                    // 失败不重试，或根据业务需求决定是否重试（设置 requeue 为 false）
                    channel.basicNack(deliveryTag, false, false);
                } catch (IOException ioException) {
                    log.error("[BIChartConsumer] basicNack 失败", ioException);
                }
            }
        });
    }

    private void processAnalysis(Long chartId) {
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            log.error("[BIChartConsumer] 图表不存在, chartId: {}", chartId);
            return;
        }

        // 修改图表状态为 "running"
        chart.setStatus(ChartStatusEnum.RUNNING.getValue());
        boolean updated = chartService.updateById(chart);
        if (!updated) {
            log.error("[BIChartConsumer] 更新图表状态失败, chartId: {}", chartId);
            handleUpdateError(chart, "更新运行状态失败");
            return;
        }

        try {
            // 构造 Prompt
            String prompt = constructPrompt(chart);

            // 调用 AI
            AiChatRequest aiRequest = AiChatRequest.builder()
                    .modelType(AiModelTypeEnum.DASHSCOPE.getValue())
                    .message(prompt)
                    .build();
            ChatLanguageModel chatModel = aiClientFactory.getChatModel(aiRequest);
            AiAssistant assistant = AiServices.builder(AiAssistant.class)
                    .chatLanguageModel(chatModel)
                    .build();

            String result = assistant.chat(prompt).content();
            String[] splits = result.split("!!!!!");
            if (splits.length < 3) {
                throw new RuntimeException("AI 生成格式错误");
            }

            String genChart = splits[1].trim();
            String genResult = splits[2].trim();

            // 更新图表结果 (采用新对象更新防止字段冲突)
            Chart finishChart = new Chart();
            finishChart.setId(chartId);
            finishChart.setGenChart(genChart);
            finishChart.setGenResult(genResult);
            finishChart.setStatus(ChartStatusEnum.SUCCEED.getValue());
            chartService.updateById(finishChart);

            // 发送成功通知
            sendNotification(chartId, chart.getUserId(), ChartStatusEnum.SUCCEED.getValue(), chart.getName(), null);

            log.info("[BIChartConsumer] 图表分析处理成功, chartId: {}", chartId);
        } catch (Exception e) {
            log.error("[BIChartConsumer] 图表分析处理失败, chartId: {}", chartId, e);
            handleUpdateError(chart, e.getMessage());
        }
    }

    private String constructPrompt(Chart chart) {
        String goal = chart.getGoal();
        String name = chart.getName();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        return "你是一个数据分析师和前端 Echarts 开发专家。请根据以下分析目标和原始数据，为我生成一个合法的 Echarts 配置。\n" +
                "分析目标：" + goal + "\n" +
                "图表名称：" + name + "\n" +
                "图表类型：" + chartType + "\n" +
                "原始数据：" + csvData + "\n" +
                "要求：\n" +
                "1. 仅返回 Echarts Option 配置对应的 JSON。\n" +
                "2. 同时给出一段不少于 100 字的数据分析结论。\n" +
                "3. 严格遵循以下输出格式，使用五个感叹号作为分隔：\n" +
                "!!!!!\n" +
                "{Echarts Option JSON}\n" +
                "!!!!!\n" +
                "{分析结论}";
    }

    private void handleUpdateError(Chart chart, String execMessage) {
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus(ChartStatusEnum.FAILED.getValue());
        updateChart.setExecMessage(execMessage);
        chartService.updateById(updateChart);

        // 发送失败通知
        sendNotification(chart.getId(), chart.getUserId(), ChartStatusEnum.FAILED.getValue(), chart.getName(),
                execMessage);
    }

    private void sendNotification(Long chartId, Long userId, String status, String chartName, String execMessage) {
        AnalysisEvent event = AnalysisEvent.builder()
                .chartId(chartId)
                .userId(userId)
                .status(status)
                .chartName(chartName)
                .execMessage(execMessage)
                .build();
        mqSender.send(MqBizTypeEnum.ANALYSIS_EVENT, event);
    }
}
