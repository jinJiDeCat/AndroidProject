package com.hjq.demo.http.exception;

/**
 * 服务器异常
 */
public class ServerException extends HttpException {

    public ServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
