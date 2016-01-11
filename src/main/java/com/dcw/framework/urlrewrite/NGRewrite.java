package com.dcw.framework.urlrewrite;

import android.text.TextUtils;

import com.dcw.framework.urlrewrite.data.UrlConverter;
import com.dcw.framework.urlrewrite.domain.Rewrite;

import java.util.HashSet;

/**
 * create by adao12.vip@gmail.com on 15/12/29
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public class NGRewrite implements Rewrite {

    private UrlConverter mUrlConverter;

    private HashSet<String> queryKeys = new HashSet<String>();

    public NGRewrite() {
        mUrlConverter = new UrlConverter();
    }

    public String rewrite(String originUrl) {
        String ret = null;
        if (TextUtils.isEmpty(originUrl)) {
            return RewriteEngineConstant.EMPTY_URL;
        }
        originUrl = originUrl.trim();//去掉开头和结尾的空格
        ret = mUrlConverter.convert(originUrl);
//        ret = "http://baidu.com";
        return ret;
    }
}
