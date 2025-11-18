package com.hjq.demo.http.server;

import com.hjq.demo.http.core.ServerHost;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : 正式环境
 */
public class ReleaseServer implements ServerHost {

    @Override
    public String getHost() {
        return "https://www.baidu.com/";
    }

    @Override
    public String getPath() {
        return "api/";
    }
}