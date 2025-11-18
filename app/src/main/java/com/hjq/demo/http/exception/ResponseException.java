package com.hjq.demo.http.exception;

import okhttp3.Response;

/**
 * 响应异常
 */
public class ResponseException extends HttpException {

    private final Response mResponse;

    public ResponseException(String message, Response response) {
        super(message);
        mResponse = response;
    }

    public Response getResponse() {
        return mResponse;
    }
}
