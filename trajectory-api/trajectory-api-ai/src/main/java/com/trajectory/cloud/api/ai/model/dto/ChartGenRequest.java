package com.trajectory.cloud.api.ai.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 智能分析请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "智能分析请求")
public class ChartGenRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 名称
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
