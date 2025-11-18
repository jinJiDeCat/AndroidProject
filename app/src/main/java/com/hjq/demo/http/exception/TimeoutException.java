package com.hjq.demo.http.exception;

/**
 * 请求超时异常
 */
public class TimeoutException extends HttpException {

    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
