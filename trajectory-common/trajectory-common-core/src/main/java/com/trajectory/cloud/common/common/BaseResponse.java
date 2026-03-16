package com.trajectory.cloud.common.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用返回类
 *
 * @param <T>
 * @author StephenQiu30
 */
@Data
@Schema(description = "通用返回类")
public class BaseResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 3801016192261040965L;

    /**
     * 状态码
     */
    @Schema(description = "状态码")
    private int code;

    /**
     * 数据
     */
    @Schema(description = "数据")
    private T data;

    /**
     * 消息
     */
    @Schema(description = "消息")
    private String message;

    /**
     * 无参构造，用于 Jackson/Feign 反序列化
     */
    public BaseResponse() {
    }

    /**
     * 响应构造函数 (全参)
     *
     * @param code    状态码
     * @param data    响应数据
     * @param message 响应消息
     */
    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    /**
     * 响应构造函数 (不带消息)
     *
     * @param code 状态码
     * @param data 响应数据
     */
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    /**
     * 响应构造函数 (使用 ErrorCode)
     *
     * @param errorCode 错误码枚举
     */
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
