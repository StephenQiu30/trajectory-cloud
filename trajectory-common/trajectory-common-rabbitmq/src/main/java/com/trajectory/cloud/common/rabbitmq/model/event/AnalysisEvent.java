package com.trajectory.cloud.common.rabbitmq.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图表分析事件（MQ）
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图表 ID
     */
    private Long chartId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 任务状态 (succeed, failed)
     */
    private String status;

    /**
     * 图表名称
     */
    private String chartName;

    /**
     * 错误详情 (如果失败)
     */
    private String execMessage;
}
