package com.dcw.framework.urlrewrite.data;

import android.text.TextUtils;
import android.util.Pair;

import com.dcw.framework.urlrewrite.NGPatternMatcher;
import com.dcw.framework.urlrewrite.RewriteEngineConstant;
import com.dcw.framework.urlrewrite.RewriteEngineUtil;
import com.dcw.framework.urlrewrite.domain.Converter;
import com.dcw.framework.urlrewrite.domain.Rule;
import com.dcw.framework.urlrewrite.domain.RuleItem;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

/**
 * create by adao12.vip@gmail.com on 15/12/29
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public class UrlConverter implements Converter<String, String> {

    private Rule mRewriteRule;
    private HashSet<String> mQueryKeys;
    private NGPatternMatcher.Factory mMatcherFactory;

    public UrlConverter() {
        mRewriteRule = new RuleFactory().getRule();
        mQueryKeys = new HashSet<String>();
        mMatcherFactory = new MatcherFactory();
    }

    @Override
    public String convert(String originUrl) {
        if (TextUtils.isEmpty(originUrl)) {
            return RewriteEngineConstant.EMPTY_URL;
        }
        originUrl = originUrl.trim();//去掉开头和结尾的空格
        String targetUrl = null;
        if (mRewriteRule != null) {
            String inputUrl = originUrl;
            //遍历所有的规则
            for (RuleItem ruleItem : mRewriteRule.getItems()) {
                NGPatternMatcher rulePattern = mMatcherFactory.create(ruleItem);
                Matcher ruleMatcher = rulePattern.reset(inputUrl);
                if (ruleMatcher.find()) {//如果有规则匹配
                    //从原Url里面拿到参数,并把参数装入新的Url
                    Object[] groupArray = new Object[ruleMatcher.groupCount()];
                    for (int i = 0; i < ruleMatcher.groupCount(); i++) {
                        groupArray[i] = ruleMatcher.group(i + 1);
                    }
                    targetUrl = String.format(ruleItem.newUrl, groupArray);
                    targetUrl = mergeOriginalQuery2Output(targetUrl, originUrl);
                    //匹配成功
                    break;
                    //拿输出作为输入继续匹配下一条规则
                }
            }
        }
        if (TextUtils.isEmpty(targetUrl)) {
            targetUrl = originUrl;
        }
        return targetUrl;
    }

    private String mergeOriginalQuery2Output(String url, String originUrl) {
        String ret = url;

        if (!TextUtils.isEmpty(url)) {
            try {
                URI uri = URI.create(url);
                String schema = uri.getScheme();
                List<Pair<String, String>> queries = RewriteEngineUtil.URLParseQuery(uri);
                mQueryKeys.clear();
                for (Pair<String, String> nvp : queries) {
                    mQueryKeys.add(nvp.first);
                }

                //添加originUrl中所有参数,如果originUrl非法，则通过try-catch保证后面ret最终会生成
                try {
                    URI originUri = URI.create(originUrl);
                    List<Pair<String, String>> originQueries = RewriteEngineUtil.URLParseQuery(originUri);
                    Iterator<Pair<String, String>> itr = originQueries.iterator();
                    while (itr.hasNext()) {
                        Pair<String, String> nvp = itr.next();
                        if (mQueryKeys.contains(nvp.first)) {
                            itr.remove();
                        }
                    }
                    queries.addAll(originQueries);
                    originQueries.clear();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //默认业务线所有url中的参数都经过UTF-8编码，所以这里不需要编码
                ret = RewriteEngineUtil
                        .URLFormatQuery(queries);
                ret = RewriteEngineUtil
                        .createURI(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(),
                                ret, uri.getFragment()).toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return ret;
    }
}
