package com.hjq.demo.http.exception;

/**
 * 数据解析异常
 */
public class DataException extends HttpException {

    public DataException(String message, Throwable cause) {
        super(message, cause);
    }
}
