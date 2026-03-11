package com.trajectory.cloud.api.ai.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新图表请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "更新图表请求")
public class ChartUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图表ID
     */
    @Schema(description = "图表ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    /**
     * 图表名称
     */
    @Schema(description = "图表名称", example = "网站用户增长分析")
    private String name;

    /**
     * 分析目标
     */
    @Schema(description = "分析目标", example = "分析网站用户的增长趋势")
    private String goal;

    /**
     * 图表类型
     */
    @Schema(description = "图表类型", example = "折线图")
    private String chartType;
}