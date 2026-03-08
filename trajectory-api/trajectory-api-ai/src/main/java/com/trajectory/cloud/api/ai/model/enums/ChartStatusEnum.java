package com.trajectory.cloud.api.ai.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 图表任务状态枚举
 *
 * @author StephenQiu30
 */
@Getter
public enum ChartStatusEnum {

    /**
     * 等待中
     */
    WAIT("wait", "等待中"),

    /**
     * 执行中
     */
    RUNNING("running", "执行中"),

    /**
     * 已完成
     */
    SUCCEED("succeed", "已完成"),

    /**
     * 处理失败
     */
    FAILED("failed", "处理失败");

    /**
     * 枚举值
     */
    private final String value;

    /**
     * 枚举文本
     */
    private final String text;

    ChartStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 值
     * @return 枚举
     */
    public static ChartStatusEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (ChartStatusEnum statusEnum : ChartStatusEnum.values()) {
            if (statusEnum.value.equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
