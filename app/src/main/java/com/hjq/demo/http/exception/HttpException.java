package com.hjq.demo.http.exception;

/**
 * author : Android 轮子哥
 * github : https://github.com/getActivity/AndroidProject
 * desc   : HTTP 基础异常
 */
public class HttpException extends Exception {

    public HttpException(String message) {
        super(message);
    }

    public HttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
