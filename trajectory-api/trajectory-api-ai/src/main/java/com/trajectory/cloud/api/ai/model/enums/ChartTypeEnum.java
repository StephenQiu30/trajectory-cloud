package com.trajectory.cloud.api.ai.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 图表类型枚举
 *
 * @author StephenQiu30
 */
@Getter
public enum ChartTypeEnum {

    /**
     * 折线图
     */
    LINE("折线图"),

    /**
     * 柱状图
     */
    BAR("柱状图"),

    /**
     * 饼图
     */
    PIE("饼图"),

    /**
     * 散点图
     */
    SCATTER("散点图"),

    /**
     * 雷达图
     */
    RADAR("雷达图"),

    /**
     * 热力图
     */
    HEATMAP("热力图"),

    /**
     * 矩形树图
     */
    TREEMAP("矩形树图"),

    /**
     * 漏斗图
     */
    FUNNEL("漏斗图");

    /**
     * 图表类型文本
     */
    private final String text;

    ChartTypeEnum(String text) {
        this.text = text;
    }

    /**
     * 根据 text 获取枚举
     *
     * @param text 图表类型文本
     * @return 枚举
     */
    public static ChartTypeEnum getEnumByText(String text) {
        if (ObjectUtils.isEmpty(text)) {
            return null;
        }
        for (ChartTypeEnum typeEnum : ChartTypeEnum.values()) {
            if (typeEnum.text.equals(text)) {
                return typeEnum;
            }
        }
        return null;
    }
}
