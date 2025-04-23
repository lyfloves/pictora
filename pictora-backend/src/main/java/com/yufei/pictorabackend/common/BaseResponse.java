package com.yufei.pictorabackend.common;

import com.yufei.pictorabackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 全局响应封装类
 * @param <T>
 */
@Data
public class BaseResponse<T> implements Serializable {

    private static final long serialVersionUID = 5555247926818265886L;

    /**
     * 响应码
     */
    private int code;

    /**
     * 数据
     */
    private T data;

    /**
     * 信息
     */
    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
