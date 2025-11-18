package com.hjq.demo.http.exception;

/**
 * 网络连接异常
 */
public class NetworkException extends HttpException {

    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
