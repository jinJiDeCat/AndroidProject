package com.hjq.demo.http.exception;

/**
 * Token 失效异常
 */
public class TokenException extends HttpException {

    public TokenException(String message) {
        super(message);
    }
}
