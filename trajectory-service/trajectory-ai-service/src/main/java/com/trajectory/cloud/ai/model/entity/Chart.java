package com.trajectory.cloud.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 图表实体
 * <p>
 * 用于存储智能分析生成的图表信息，包括原始数据、生成结果、执行状态等。
 * 支持同步和异步两种生成方式，支持多种图表类型。
 * </p>
 *
 * @author StephenQiu30
 */
@TableName(value = "chart")
@Data
@Schema(description = "图表表")
public class Chart implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图表ID
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "图表ID")
    private Long id;

    /**
     * 分析目标
     */
    @Schema(description = "分析目标")
    private String goal;

    /**
     * 图表名称
     */
    @Schema(description = "图表名称")
    private String name;

    /**
     * 图表数据（CSV格式）
     */
    @Schema(description = "图表数据（CSV格式）")
    private String chartData;

    /**
     * 图表类型
     */
    @Schema(description = "图表类型")
    private String chartType;

    /**
     * 生成的图表配置（Echarts Option JSON）
     */
    @Schema(description = "生成的图表配置（Echarts Option JSON）")
    private String genChart;

    /**
     * 生成的分析结论
     */
    @Schema(description = "生成的分析结论")
    private String genResult;

    /**
     * 状态（wait-等待中，running-运行中，succeed-成功，failed-失败）
     */
    @Schema(description = "状态（wait-等待中，running-运行中，succeed-成功，failed-失败）")
    private String status;

    /**
     * 执行详情（错误信息等）
     */
    @Schema(description = "执行详情（错误信息等）")
    private String execMessage;

    /**
     * 创建用户ID
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

    /**
     * 是否删除
     */
    @TableLogic
    @Schema(description = "是否删除")
    private Integer isDelete;
}
