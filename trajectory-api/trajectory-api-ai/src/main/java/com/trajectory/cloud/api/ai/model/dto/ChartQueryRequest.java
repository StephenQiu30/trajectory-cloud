package com.trajectory.cloud.api.ai.model.dto;

import com.trajectory.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图表查询请求
 *
 * @author StephenQiu30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "图表查询请求")
public class ChartQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Schema(description = "图表ID")
    private Long id;

    /**
     * 名称
     */
    @Schema(description = "图表名称")
    private String name;

    /**
     * 分析目标
     */
    @Schema(description = "分析目标")
    private String goal;

    /**
     * 图表类型
     */
    @Schema(description = "图表类型")
    private String chartType;

    /**
     * 创建用户 id
     */
    @Schema(description = "用户ID")
    private Long userId;
}
