package com.trajectory.cloud.ai.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trajectory.cloud.ai.model.entity.Chart;
import com.trajectory.cloud.ai.service.ChartService;
import com.trajectory.cloud.api.ai.model.dto.ChartAnalysisMessage;
import com.trajectory.cloud.api.ai.model.enums.ChartStatusEnum;
import com.trajectory.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.trajectory.cloud.common.rabbitmq.utils.MqSender;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 图表重试任务
 * 定期扫描状态为 failed 的图表并重新发送到 MQ 进行分析
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class ChartRetryJob {

    @Resource
    private ChartService chartService;

    @Resource
    private MqSender mqSender;

    /**
     * 每 30 分钟扫描一次失败的图表并重试
     */
    @Scheduled(cron = "0 0/30 * * * *")
    public void retryFailedCharts() {
        log.info("[ChartRetryJob] 开始扫描失败图表进行补偿重试...");

        // 查询所有失败且未删除的图表
        LambdaQueryWrapper<Chart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Chart::getStatus, ChartStatusEnum.FAILED.getValue());
        queryWrapper.eq(Chart::getIsDelete, 0);
        // 限制一次扫描的数量，防止过多消息瞬间冲击
        queryWrapper.last("LIMIT 50");

        List<Chart> failedCharts = chartService.list(queryWrapper);
        if (failedCharts.isEmpty()) {
            log.info("[ChartRetryJob] 未发现需要重试的失败图表");
            return;
        }

        for (Chart chart : failedCharts) {
            log.info("[ChartRetryJob] 正在重试图表, chartId: {}, name: {}", chart.getId(), chart.getName());

            // 将状态重置为 wait，避免 Job 重复扫描
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus(ChartStatusEnum.WAIT.getValue());
            updateChart.setExecMessage("补偿机制触发重试");
            chartService.updateById(updateChart);

            // 发送 MQ 消息
            ChartAnalysisMessage message = ChartAnalysisMessage.builder()
                    .chartId(chart.getId())
                    .build();
            mqSender.send(MqBizTypeEnum.BI_CHART, message);
        }

        log.info("[ChartRetryJob] 补偿重试完成，处理数量: {}", failedCharts.size());
    }
}
