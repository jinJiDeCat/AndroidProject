package com.hjq.demo.http.request;

import com.hjq.demo.http.core.RequestApi;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : 获取用户信息
 */
public final class UserInfoApi implements RequestApi {

    @Override
    public String getApi() {
        return "user/info";
    }
}