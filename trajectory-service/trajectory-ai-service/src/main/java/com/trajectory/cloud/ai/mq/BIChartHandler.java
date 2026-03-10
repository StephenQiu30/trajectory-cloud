package com.trajectory.cloud.ai.mq;

import com.trajectory.cloud.ai.factory.AiClientFactory;
import com.trajectory.cloud.ai.model.entity.Chart;
import com.trajectory.cloud.ai.service.AiAssistant;
import com.trajectory.cloud.ai.service.ChartService;
import com.trajectory.cloud.api.ai.model.dto.AiChatRequest;
import com.trajectory.cloud.api.ai.model.enums.AiModelTypeEnum;
import com.trajectory.cloud.api.ai.model.enums.ChartStatusEnum;
import com.trajectory.cloud.common.rabbitmq.consumer.MqHandler;
import com.trajectory.cloud.common.rabbitmq.consumer.MqIdempotent;
import com.trajectory.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.trajectory.cloud.common.rabbitmq.model.RabbitMessage;
import com.trajectory.cloud.common.rabbitmq.model.event.AnalysisEvent;
import com.trajectory.cloud.api.ai.model.dto.ChartAnalysisMessage;
import com.trajectory.cloud.common.rabbitmq.utils.MqSender;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * BI 图表分析业务处理器
 * <p>
 * 实现 {@link MqHandler} 接口，由
 * {@link com.trajectory.cloud.common.rabbitmq.consumer.MqConsumerDispatcher}
 * 自动路由分发。结合自定义线程池实现 MQ + 线程池异步处理模式：
 * <ul>
 * <li>MQ 负责消息投递、幂等去重与 ACK 确认</li>
 * <li>线程池负责 AI 分析任务的并发控制与资源收敛</li>
 * <li>图表状态机 (wait → running → succeed/failed) 通过 DB 驱动，确保任务最终状态可追溯</li>
 * </ul>
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
@MqIdempotent(prefix = "mq:bi:chart", expire = 86400)
public class BIChartHandler implements MqHandler<ChartAnalysisMessage> {

    @Resource
    private ChartService chartService;

    @Resource
    private AiClientFactory aiClientFactory;

    @Resource
    private MqSender mqSender;

    @Resource(name = "biThreadPoolExecutor")
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.BI_CHART.getValue();
    }

    /**
     * 处理 BI 图表分析消息
     * <p>
     * 采用 MQ + 线程池异步模式：
     * 1. MQ Dispatcher 调用本方法后立即 ACK，表示消息已被接管
     * 2. 实际的 AI 分析任务提交到 {@code biThreadPoolExecutor} 线程池异步执行
     * 3. 任务成功/失败通过 DB 图表状态机追踪，并发送分析事件通知
     * </p>
     *
     * @param message       图表分析消息
     * @param rabbitMessage 原始消息对象
     */
    @Override
    public void onMessage(ChartAnalysisMessage message, RabbitMessage rabbitMessage) {
        Long chartId = message.getChartId();
        if (chartId == null) {
            log.error("[BIChartHandler] 消息解析失败, msgId: {}", rabbitMessage.getMsgId());
            throw new IllegalArgumentException("chartId 为空");
        }

        log.info("[BIChartHandler] 收到图表分析任务, chartId: {}, 提交至线程池异步处理", chartId);

        // 提交到线程池异步执行，MQ Dispatcher 随后 ACK 本次消息
        // 任务最终状态由 DB 图表状态机保证（wait → running → succeed/failed）
        // 线程池采用 CallerRunsPolicy：当线程池满时，由 MQ 消费线程直接执行，提供自然反压
        threadPoolExecutor.execute(() -> processAnalysis(chartId));
    }

    @Override
    public Class<ChartAnalysisMessage> getDataType() {
        return ChartAnalysisMessage.class;
    }

    /**
     * 执行图表分析核心流程
     * <p>
     * 在 {@code biThreadPoolExecutor} 线程池中执行，
     * 包含状态流转、AI 调用、结果入库和通知发送。
     * </p>
     *
     * @param chartId 图表 ID
     */
    private void processAnalysis(Long chartId) {
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            log.error("[BIChartHandler] 图表不存在, chartId: {}", chartId);
            return;
        }

        // 修改图表状态为 "running"
        Chart runningChart = new Chart();
        runningChart.setId(chartId);
        runningChart.setStatus(ChartStatusEnum.RUNNING.getValue());
        boolean updated = chartService.updateById(runningChart);
        if (!updated) {
            log.error("[BIChartHandler] 更新图表状态失败, chartId: {}", chartId);
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

            // 更新图表结果（采用新对象更新防止字段冲突）
            Chart finishChart = new Chart();
            finishChart.setId(chartId);
            finishChart.setGenChart(genChart);
            finishChart.setGenResult(genResult);
            finishChart.setStatus(ChartStatusEnum.SUCCEED.getValue());
            chartService.updateById(finishChart);

            // 发送成功通知
            sendNotification(chartId, chart.getUserId(), ChartStatusEnum.SUCCEED.getValue(), chart.getName(), null);

            log.info("[BIChartHandler] 图表分析处理成功, chartId: {}", chartId);
        } catch (Exception e) {
            log.error("[BIChartHandler] 图表分析处理失败, chartId: {}", chartId, e);
            handleUpdateError(chart, e.getMessage());
        }
    }

    /**
     * 构造 AI 分析 Prompt
     *
     * @param chart 图表实体
     * @return AI 提示词
     */
    private String constructPrompt(Chart chart) {
        return "你是一个数据分析师和前端 Echarts 开发专家。请根据以下分析目标和原始数据，为我生成一个合法的 Echarts 配置。\n" +
                "分析目标：" + chart.getGoal() + "\n" +
                "图表名称：" + chart.getName() + "\n" +
                "图表类型：" + chart.getChartType() + "\n" +
                "原始数据：" + chart.getChartData() + "\n" +
                "要求：\n" +
                "1. 仅返回 Echarts Option 配置对应的 JSON。\n" +
                "2. 同时给出一段不少于 100 字的数据分析结论。\n" +
                "3. 严格遵循以下输出格式，使用五个感叹号作为分隔：\n" +
                "!!!!!\n" +
                "{Echarts Option JSON}\n" +
                "!!!!!\n" +
                "{分析结论}";
    }

    /**
     * 处理图表分析失败，更新状态并发送失败通知
     *
     * @param chart       图表实体
     * @param execMessage 错误信息
     */
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

    /**
     * 发送图表分析事件通知
     *
     * @param chartId     图表 ID
     * @param userId      用户 ID
     * @param status      任务状态
     * @param chartName   图表名称
     * @param execMessage 错误详情（成功时为 null）
     */
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
