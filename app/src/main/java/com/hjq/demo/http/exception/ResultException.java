package com.hjq.demo.http.exception;

/**
 * 业务结果异常
 */
public class ResultException extends HttpException {

    private final Object mData;

    public ResultException(String message, Object data) {
        super(message);
        mData = data;
    }

    public Object getData() {
        return mData;
    }
}
