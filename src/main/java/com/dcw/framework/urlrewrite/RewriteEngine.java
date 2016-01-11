package com.dcw.framework.urlrewrite;

import android.app.Application;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Pair;

import com.dcw.framework.urlrewrite.data.RuleFactory;
import com.dcw.framework.urlrewrite.domain.Rule;
import com.dcw.framework.urlrewrite.domain.RuleItem;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * create by adao12.vip@gmail.com on 15/12/12
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public class RewriteEngine {


    //$1
    private static final String PATTERN_STR
            = "(\\$\\d+|\\$scheme|\\$host|\\$port|\\$path|\\$query|\\$fragment|\\$shopid)";
    //$$1
    private static final String varEncodePatternStr
            = "(\\$\\$\\d+|\\$\\$scheme|\\$\\$host|\\$\\$port|\\$\\$path|\\$\\$query|\\$\\$fragment|\\$\\$shopid)";
    //$#1
    private static final String varDecodePatternStr
            = "(\\$#\\d+|\\$#scheme|\\$#host|\\$#port|\\$#path|\\$#query|\\$#fragment|\\$#shopid)";
    //$$$1
    private static final String varDecodeEncodePatternStr
            = "(\\$\\$\\$\\d+|\\$\\$\\$scheme|\\$\\$\\$host|\\$\\$\\$port|\\$\\$\\$path|\\$\\$\\$query|\\$\\$\\$fragment|\\$\\$\\$shopid)";

    private static final int VARIABLE_GROUP_INDEX = 1;

    private static final int NORMAL_PATTERN_INDEX = 0;

    private static final int ENCODE_PATTERN_INDEX = 1;

    private static final int DECODE_PATTERN_INDEX = 2;

    private static final int DECODE_ENCODE_PATTERN_INDEX = 3;


    private Application mApplication;

    private String rewriteModuleName;

    private Rule.Factory mRuleFactory;

    private Rule mRewriteRule;

    private HashMap<String, NGPatternMatcher> mPatternCache = new HashMap<String, NGPatternMatcher>();

    private StringBuffer buffer = new StringBuffer();

    private HashSet<String> queryKeys = new HashSet<String>();

    private String lastInputUrl;

    private String lastOutputUrl;

    private RewriteEngine(@NonNull Rule.Factory ruleFactory) {
        mPatternCache.put(PATTERN_STR, new NGPatternMatcher(PATTERN_STR));
        mPatternCache.put(varDecodePatternStr, new NGPatternMatcher(varDecodePatternStr));
        mPatternCache.put(varEncodePatternStr, new NGPatternMatcher(varEncodePatternStr));
        mPatternCache.put(varDecodeEncodePatternStr, new NGPatternMatcher(varDecodeEncodePatternStr));
        mRewriteRule = ruleFactory.getRule();
    }

    private static final class TMSingletonHolder {

        public final static RewriteEngine INSTANCE = new RewriteEngine(new RuleFactory());
    }

    public static RewriteEngine getInstance() {
        return TMSingletonHolder.INSTANCE;
    }

    public void setRuleFactory(@NonNull Rule.Factory ruleFactory) {
        mRuleFactory = ruleFactory;
        mRewriteRule = mRuleFactory.getRule();
    }

    /**
     * 初始化，启动配置中心监听
     * @param application 全局application对象
     */
    public void init(Application application) {
        mApplication = application;
//        LocalBroadcastManager.getInstance(application).registerReceiver(new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                String action = intent.getAction();
//                if (TMConfigCenterManager.CONFIG_CENTER_UPDATE_ACTION.equals(action)) {
//                    mRewriteRule = getRewriteRule();
//                }
//            }
//        }, new IntentFilter(TMConfigCenterManager.CONFIG_CENTER_UPDATE_ACTION));
    }

    /**
     * 设置rewrite规则的配置模块，会重新获取一下配置信息
     * @param rewriteModuleName rewrite模块名
     */
    public void setRewriteRuleModuleName(String rewriteModuleName) {
        this.rewriteModuleName = rewriteModuleName;
        mRewriteRule = getRewriteRule();
    }

    private Rule getRewriteRule() {
//        ArrayList<String> configdata = rewriteModuleName != null && rewriteModuleName.length() > 0
//                ? TMConfigCenterManager.getInstance().getAllConfigDataByName(rewriteModuleName)
//                : new ArrayList<String>();
//        Rule ret = Rule.parseNewConfig(rewriteModuleName, configdata);
        return null;
    }

    /**
     * 将url转换成另一条url，如果没有匹配的规则，原样输出
     * @param originUrl 待rewrite的链接
     * @return 经过rewrite引擎转换之后的链接
     */
    public String rewrite(String originUrl) {
        String ret = null;
        if (TextUtils.isEmpty(originUrl)) {
            return RewriteEngineConstant.EMPTY_URL;
        }
        originUrl = originUrl.trim();//去掉开头和结尾的空格
//        Log.i(RewriteEngineConstant.LOG_TAG, "input url:" + originUrl);

        //进行规则匹配
        Rule rewriteRule = mRewriteRule;
        if (null != rewriteRule) {
            String inputUrl = originUrl;

            for (int i = 0, len = rewriteRule.items.size(); i < len; i++) {
                try {
                    RuleItem ruleItem = rewriteRule.items.get(i);
                    NGPatternMatcher rulePattern = mPatternCache.get(ruleItem.originUrl);
                    if (rulePattern == null) {
                        rulePattern = new NGPatternMatcher(ruleItem.originUrl);
                        mPatternCache.put(ruleItem.originUrl, rulePattern);
                    }
                    Matcher ruleMatcher = rulePattern.reset(inputUrl);
                    if (ruleMatcher.find()) {
                        String outputUrl = null;
                        if (ruleItem.flag.contains("s")) {
                            //加上个性店铺域名处理
                            long shopId = retrieveShopIdByDomain(inputUrl);
                            if (shopId > 0) {
                                outputUrl = processConvertRule(ruleMatcher, inputUrl,
                                        ruleItem.newUrl, shopId);
                            } else {
                                //个性店铺域名匹配错误或网络错误，则直接放过去，当h5处理
                                continue;
                            }
                        } else {
                            //正常处理
                            outputUrl = processConvertRule(ruleMatcher, inputUrl, ruleItem.newUrl,
                                    -1);
                        }
//                        Log.i(RewriteEngineConstant.LOG_TAG, "find output: " + outputUrl);

                        //剥离tmallclient, link:ulr, internal:url等旧版协议外壳之后的url才是真实的 h5 url
                        //后面各种针对源url的处理需要针对这个才有效果
                        if (ruleItem.flag.isEmpty()
                                && (outputUrl.startsWith("http")
                                || outputUrl.startsWith("//") //集团https改造去schema
                                || outputUrl.startsWith("tmall://"))) {//TODO 这个tmall协议判断也应该去掉
//                            Log.i(RewriteEngineConstant.LOG_TAG, "reset originUrl to: " + outputUrl);
                            originUrl = outputUrl;
                        }
                        if (ruleItem.flag.contains("l")) {
//                            Log.i(RewriteEngineConstant.LOG_TAG, "get flag l:" + outputUrl);
                            //匹配成功
                            ret = outputUrl;
                            break;
                        } else {
                            if (TextUtils.isEmpty(outputUrl)) {
//                                Log.e(RewriteEngineConstant.LOG_TAG,
//                                        "outputUrl is empty while rule index =" + i);
                                break;
                            } else {
//                                Log.i(RewriteEngineConstant.LOG_TAG, "next input: " + outputUrl);
                                //拿输出作为输入继续匹配下一条规则
                                inputUrl = outputUrl;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //所有规则都没匹配到, 原样输出
            if (ret == null) {
//                Log.i(RewriteEngineConstant.LOG_TAG, "not matched:" + originUrl);
                ret = originUrl;
            }
        } else {
            //找不到配置项，原样输出
            if (ret == null) {
//                Log.e(RewriteEngineConstant.LOG_TAG,
//                        "rewriteRule=" + rewriteRule + "|originUrl=" + originUrl);
                ret = originUrl;
            }
        }
        lastInputUrl = originUrl;
        lastOutputUrl = ret;
//        Log.i(RewriteEngineConstant.LOG_TAG, "at end input.url=" + originUrl);
//        Log.i(RewriteEngineConstant.LOG_TAG, "at end final.url=" + ret);
        return ret;
    }

    /**
     * @return 返回上次rewrite过程中，最后一次匹配成功的输入，如果没有匹配，则返回原始输入
     */
    public String getLastInputUrl() {
        return lastInputUrl;
    }

    /**
     * @return 返回上次rewrite过程中，最后一次匹配成功的输出，如果没有匹配，则返回原始输入
     */
    public String getLastOutputUrl() {
        return lastOutputUrl;
    }

    private String convertGbk2UtfUrl(String gbkUrl) {
        HashMap<String, String> parameters = new HashMap<String, String>();
        try {
            URI uri = URI.create(gbkUrl);
            String query = uri.getRawQuery();
            if (!TextUtils.isEmpty(query)) {
                String pairs[] = query.split("&");
                if (pairs != null) {
//                    Log.i(RewriteEngineConstant.LOG_TAG, "gbk params:" + pairs);
                    for (String pair : pairs) {
                        if (!TextUtils.isEmpty(pair)) {
                            String[] map = pair.split("=");
                            if (map != null && map.length > 1) {
                                String name = map[0];
                                if (TextUtils.isEmpty(name)) {
                                    continue;
                                }

                                //参数默认UTF-8编码 ，但q, loc等字段需要特殊处理 ，它们既可能是UTF-8, 也可能是GBK
                                String value = null;
                                String charset = EncodingDetector.decGBKorUTF8(map[1]);
                                if ("UTF-8".equalsIgnoreCase(charset)) {
                                    //兼容Uri.decode会将"+"解码成""rewriteUriDecode
                                    value = RewriteEngineUtil.rewriteUriDecode(map[1]);
                                } else {
                                    value = URLDecoder.decode(map[1], charset);
                                }
                                parameters.put(name, value);
                            }
                        }
                    }
                    query = RewriteEngineUtil.processProtocolParameters(parameters);
                    String convertedUrl = RewriteEngineUtil
                            .createURI(uri.getScheme(), uri.getHost(),
                                    uri.getPort(), uri.getPath(),
                                    query, uri.getFragment()).toString();
                    return convertedUrl;
                } else {
//                    Log.e(RewriteEngineConstant.LOG_TAG,
//                            "rewrite as search without param:" + gbkUrl);
                }
            } else {
//                Log.e(RewriteEngineConstant.LOG_TAG, "rewrite as search without query" + gbkUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gbkUrl;
    }

    private String mergeOriginalQuery2Output(String url, String originUrl) {
        String ret = url;

        if (!TextUtils.isEmpty(url)) {
            try {
                URI uri = URI.create(url);
                String schema = uri.getScheme();
                List<Pair<String, String>> querys = RewriteEngineUtil.URLParseQuery(uri);
                queryKeys.clear();
                for (Pair<String, String> nvp : querys) {
                    queryKeys.add(nvp.first);
                }

                //添加originUrl中所有参数,如果originUrl非法，则通过try-catch保证后面ret最终会生成
                try {
                    URI originUri = URI.create(originUrl);
                    List<Pair<String, String>> originQuerys = RewriteEngineUtil.URLParseQuery(originUri);
                    Iterator<Pair<String, String>> itr = originQuerys.iterator();
                    while (itr.hasNext()) {
                        Pair<String, String> nvp = itr.next();
                        if (queryKeys.contains(nvp.first)) {
                            itr.remove();
                        }
                    }
                    querys.addAll(originQuerys);
                    originQuerys.clear();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //默认业务线所有url中的参数都经过UTF-8编码，所以这里不需要编码
                ret = RewriteEngineUtil
                        .URLFormatQuery(querys);
                ret = RewriteEngineUtil
                        .createURI(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(),
                                ret, uri.getFragment()).toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return ret;
    }

    private String processConvertRule(Matcher macher, String oldUrl, String newUrl, long shopId) {
        String ret = null;

        //处理$$$解编码变量
        ret = convertVariable(macher, oldUrl, newUrl, DECODE_ENCODE_PATTERN_INDEX, shopId);
        //处理$$编码变量
        ret = convertVariable(macher, oldUrl, ret, ENCODE_PATTERN_INDEX, shopId);
        //处理$普通变量
        ret = convertVariable(macher, oldUrl, ret, NORMAL_PATTERN_INDEX, shopId);
        //处理$#解码变量
        ret = convertVariable(macher, oldUrl, ret, DECODE_PATTERN_INDEX, shopId);

        return ret;
    }

    /**
     * 根据newUrl里的变量标识符，将oldUrl里的变量先处理一下，替换newUrl里的占位，返回处理后的newUrl
     * @param matcher
     * @param oldUrl
     * @param newUrl
     * @param varFlag
     * @param shopId
     * @return
     */
    private String convertVariable(Matcher matcher, String oldUrl, String newUrl, int varFlag,
                                   long shopId) {

        Uri oldUri = null;
        try {
            oldUri = Uri.parse(oldUrl);
        } catch (Exception e) {
            oldUri = null;
            e.printStackTrace();
        }

        //选择匹配模式
        NGPatternMatcher varPattern = null;
        if (NORMAL_PATTERN_INDEX == varFlag) {
            varPattern = mPatternCache.get(PATTERN_STR);
        } else if (ENCODE_PATTERN_INDEX == varFlag) {
            varPattern = mPatternCache.get(varEncodePatternStr);
        } else if (DECODE_PATTERN_INDEX == varFlag) {
            varPattern = mPatternCache.get(varDecodePatternStr);
        } else if (DECODE_ENCODE_PATTERN_INDEX == varFlag) {
            varPattern = mPatternCache.get(varDecodeEncodePatternStr);
        }
        buffer.delete(0, buffer.length());

        if (null != varPattern) {
            Matcher varMatcher = varPattern.reset(newUrl);
            if (varMatcher.find()) {
                do {
                    String varStr = varMatcher.group(VARIABLE_GROUP_INDEX); //取出第一个分组
                    String var = "";
                    if (!TextUtils.isEmpty(varStr)) {
                        if (NORMAL_PATTERN_INDEX == varFlag) {
                            var = varStr.substring(1); //取原变量，例如$1中的数字1
                        } else if (ENCODE_PATTERN_INDEX == varFlag || DECODE_PATTERN_INDEX == varFlag) {
                            var = varStr.substring(2); //取编码变量，例如$#1中的数字1
                        } else if (DECODE_ENCODE_PATTERN_INDEX == varFlag) {
                            var = varStr.substring(3); //取编码变量，例如$$$1中的数字1
                        }

                        try {
                            int index = Integer.parseInt(var);
                            int groupCnt = matcher.groupCount();
                            if ((index >= 0) && (index <= groupCnt)) {
                                String replacement = null;
                                if (0 == index) {
                                    //group 0只会返回输入中匹配规则的部分,不会返回完整的输入
                                    replacement = oldUrl;
                                } else {
                                    replacement = matcher.group(index);
                                }
                                if (!TextUtils.isEmpty(replacement)) {
                                    replacement = processStrCode(replacement, varFlag);
                                    varMatcher.appendReplacement(buffer, replacement);
                                } else {
                                    varMatcher.appendReplacement(buffer, "");
                                }
                            } else {
                                varMatcher.appendReplacement(buffer, "");
//                                Log.e(RewriteEngineConstant.LOG_TAG,
//                                        "group index out of bound:" + varStr + "|" + groupCnt);
                            }
                        } catch (NumberFormatException e) {
                            if (varStr.endsWith("shopid")) {
                                //添加店铺id
                                String replacement = String.valueOf(shopId);
                                replacement = processStrCode(replacement, varFlag);
                                varMatcher.appendReplacement(buffer, replacement);
                            } else {
                                if (null != oldUri) {
                                    if (varStr.endsWith("scheme")) {
                                        String replacement = oldUri.getScheme();
                                        if (null != replacement) {
                                            //集团https改造导致url没有schema
                                            replacement = processStrCode(replacement, varFlag);
                                            varMatcher.appendReplacement(buffer, replacement);
                                        }
                                    } else if (varStr.endsWith("host")) {
                                        String replacement = oldUri.getHost();
                                        if (null != replacement) {
                                            replacement = processStrCode(replacement, varFlag);
                                            varMatcher.appendReplacement(buffer, replacement);
                                        }
                                    } else if (varStr.endsWith("port")) {
                                        String replacement = String.valueOf(oldUri.getPort());
                                        replacement = processStrCode(replacement, varFlag);
                                        varMatcher.appendReplacement(buffer, replacement);
                                    } else if (varStr.endsWith("path")) {
                                        String replacement = oldUri.getPath();
                                        if (null != replacement) {
                                            replacement = processStrCode(replacement, varFlag);
                                            varMatcher.appendReplacement(buffer, replacement);
                                        }

                                    } else if (varStr.endsWith("query")) {
                                        String replacement = oldUri.getQuery();
                                        if (null != replacement) {
                                            replacement = processStrCode(replacement, varFlag);
                                            varMatcher.appendReplacement(buffer, replacement);
                                        }
                                    } else if (varStr.endsWith("fragment")) {
                                        String replacement = oldUri.getFragment();
                                        if (null != replacement) {
                                            replacement = processStrCode(replacement, varFlag);
                                            varMatcher.appendReplacement(buffer, replacement);
                                        }
                                    } else {
//                                        Log.e(RewriteEngineConstant.LOG_TAG,
//                                                "shema name err:" + varStr);
                                    }
                                }
                            }
                        }
                    } else {
//                        Log.e(RewriteEngineConstant.LOG_TAG,
//                                varFlag + " matched group 1 is empty for:" + newUrl);
                    }
                } while (varMatcher.find());
                varMatcher.appendTail(buffer);
            } else {
                buffer.append(newUrl);
            }
        } else {
//            Log.e(RewriteEngineConstant.LOG_TAG, "varFlag err:" + varFlag);
        }

        return buffer.toString();
    }

    private String processStrCode(String str, int flag) {
        String ret = str;
        try {
            if(ENCODE_PATTERN_INDEX == flag) {
                ret = URLEncoder.encode(str, "UTF-8");
            } else if(DECODE_PATTERN_INDEX == flag) {
                String charset = EncodingDetector.decGBKorUTF8(str);
                ret = URLDecoder.decode(str, charset);
            } else if (DECODE_ENCODE_PATTERN_INDEX == flag) {
                String charset = EncodingDetector.decGBKorUTF8(str);
                ret = URLEncoder.encode(URLDecoder.decode(ret, charset), "UTF-8");
            }
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }

        return ret;
    }

    private static HashSet<Long> chaoshiShopIdSets = new HashSet<Long>();

    static {
        chaoshiShopIdSets.add(67597230L);
        chaoshiShopIdSets.add(101975462L);
        chaoshiShopIdSets.add(108330122L);
        chaoshiShopIdSets.add(107693821L);
    }

    private static final String TMALL_HOST_WAP_POSTFIX = ".m.tmall.com";

    private static final String TMALL_HOST_POSTFIX = ".tmall.com";

    private static final String TMALL_SHOP_BLACK_LIST_PATTERN
            = "^(www|login|register|vip|m|mobile|vip|guize|service|chaoshi)+\\.(taobao|tmall)\\.com$";

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private long retrieveShopIdByDomain(String url) {

        try {
            String host = RewriteEngineUtil.getHost(url);
            final String compactHost = host.replace(TMALL_HOST_WAP_POSTFIX,
                    TMALL_HOST_POSTFIX);
            Matcher shopBlackListMatcher = Pattern.compile(TMALL_SHOP_BLACK_LIST_PATTERN)
                    .matcher(host);
            if (!shopBlackListMatcher.find()) {
                FutureTask<Long> shopIdTask = new FutureTask<Long>(new Callable<Long>() {

                    @Override
                    public Long call() throws Exception {
//                        TMShopGetShopInfoByDomainRequest request = new TMShopGetShopInfoByDomainRequest();
//                        request.setDomainName(compactHost);
//                        TMShopGetShopInfoByDomainResponse response = TMNetBus
//                                .sendRequest(request,
//                                        TMShopGetShopInfoByDomainResponse.class);
//                        if (response != null && TMNetUtil
//                                .isMTopRequestSuccuss(response.getRet()) && response.getData() != null) {
//                            long id = response.getData().shopId;
////                            Log.i(RewriteEngineConstant.LOG_TAG, "retrieve shopId " + id);
//                            return id;
//                        }
                        return 0L;
                    }

                });
                executor.execute(shopIdTask);
                long ret = shopIdTask.get(1000, TimeUnit.MILLISECONDS);
                if (chaoshiShopIdSets.contains(ret)) {
                    ret = 0L;
                }

                return ret;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0L;
    }
}
