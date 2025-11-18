package com.hjq.demo.http.exception;

/**
 * 请求被取消异常
 */
public class CancelException extends HttpException {

    public CancelException(String message, Throwable cause) {
        super(message, cause);
    }
}
