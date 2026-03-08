package com.trajectory.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 图表视图
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "图表视图")
public class ChartVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Schema(description = "图表ID")
    private Long id;

    /**
     * 目标
     */
    @Schema(description = "分析目标")
    private String goal;

    /**
     * 名称
     */
    @Schema(description = "图表名称")
    private String name;

    /**
     * 图表数据
     */
    @Schema(description = "图表数据")
    private String chartData;

    /**
     * 图表类型
     */
    @Schema(description = "图表类型")
    private String chartType;

    /**
     * 生成的图表
     */
    @Schema(description = "生成的图表配置")
    private String genChart;

    /**
     * 生成的结论
     */
    @Schema(description = "生成的结论")
    private String genResult;

    /**
     * 状态 (wait, running, succeed, failed)
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 执行详情
     */
    @Schema(description = "执行详情")
    private String execMessage;

    /**
     * 创建用户 id
     */
    @Schema(description = "创建用户ID")
    private Long userId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date updateTime;
}
