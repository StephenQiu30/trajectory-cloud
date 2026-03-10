package com.trajectory.cloud.api.ai.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图表分析消息（MQ 载体）
 * <p>
 * 作为 BI 图表分析任务的 MQ 投递载体，承载分析所需的核心标识。
 * 遵循项目统一的 DTO 消息模式，避免直接传递基本类型导致的 JSON 序列化兼容性问题。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartAnalysisMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图表 ID
     */
    private Long chartId;
}
