package com.dcw.framework.urlrewrite;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * create by adao12.vip@gmail.com on 15/12/14
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public class RewriteEngineUtil {

    /**
     * 以map的形式返回所有参数，如果有重复参数，以第一次出现的为准，其中参数值未解码
     * @param intent 打开页面的intent
     * @return 参数键值对
     */
    public static final HashMap<String, String> getAllRawQueryParameters(Intent intent) {
        HashMap<String, String> queries = new HashMap<String, String>();
        if(intent != null) {
            Uri uri = intent.getData();
            if(null != uri){
                final String query = uri.getEncodedQuery();
                if (query != null) {
                    final int length = query.length();
                    int start = 0;
                    do {
                        int nextAmpersand = query.indexOf('&', start);
                        int end = nextAmpersand != -1 ? nextAmpersand : length;

                        int separator = query.indexOf('=', start);
                        if (separator > end || separator == -1) {
                            separator = end;
                        }

                        String encodedKey = query.substring(start, separator);
                        String encodedValue = query.substring(separator + 1, end);

                        String key = Uri.decode(encodedKey);
                        if (!queries.containsKey(key)) {
                            queries.put(key, encodedValue);
                        }

                        // Move start to end of name.
                        if (nextAmpersand != -1) {
                            start = nextAmpersand + 1;
                        } else {
                            break;
                        }
                    } while (true);
                }
            }
        }
        return queries;
    }

    /**
     * 以map的形式返回所有参数，如果有重复参数，以第一次出现的为准
     * @param intent 打开页面的intent
     * @return 参数键值对
     */
    public static final HashMap<String, String> getAllQueryParameters(Intent intent) {
        HashMap<String, String> querys = getAllRawQueryParameters(intent);
        Iterator<Map.Entry<String, String>> keys = querys.entrySet().iterator();
        while (keys.hasNext()) {
            Map.Entry<String, String> entry = keys.next();
            String key = entry.getKey();
            String value = querys.get(key);
            querys.put(key, rewriteUriDecode(value));
        }
        return querys;
    }

    /**
     * @param uri 原始uri对象
     * @param queryParameterName  query中参数名
     * @return url中queryParameterName对应的原始值(没有解码), 如获取q的值时, q=android返回android, q=返回"", 其他返回null
     */
    public static final String getRawQueryParameter(Uri uri, String queryParameterName) {
        String ret = null;

        if ((uri != null) && (!TextUtils.isEmpty(queryParameterName))) {
            final String query = uri.getEncodedQuery();
            if (query == null) {
                return null;
            }

            final String encodedKey = Uri.encode(queryParameterName, null);
            final int length = query.length();
            int start = 0;
            do {
                int nextAmpersand = query.indexOf('&', start);
                int end = nextAmpersand != -1 ? nextAmpersand : length;

                int separator = query.indexOf('=', start);
                if (separator > end || separator == -1) {
                    separator = end;
                }

                if (separator - start == encodedKey.length()
                        && query.regionMatches(start, encodedKey, 0, encodedKey.length())) {
                    if (separator == end) {
                        ret = "";
                    } else {
                        ret = query.substring(separator + 1, end);
                    }
                    break;
                }

                // Move start to end of name.
                if (nextAmpersand != -1) {
                    start = nextAmpersand + 1;
                } else {
                    break;
                }
            } while (true);
        }

        return ret;
    }

    /**
     * @param uri 原始uri对象
     * @param queryParameterName  query中参数名
     * @return url中queryParameterName对应的值(经过一次UTF-8解码)
     */
    public static final String getQueryParameter(Uri uri, String queryParameterName) {
        String ret = getRawQueryParameter(uri, queryParameterName);
        if (!TextUtils.isEmpty(ret)) {
            //兼容Uri.decode会将"+"解码成""
            ret = rewriteUriDecode(ret);
        }

        return ret;
    }

    /**
     * @param uri 原始uri对象
     * @param queryParameterName  query中参数名
     * @param defaultValue  query中参数名的默认值
     * @return url中queryParameterName对应的值(经过一次UTF-8解码)，如果不存在则返回defaultValue
     */
    public static final String getQueryParameter(Uri uri, String queryParameterName,
                                                 String defaultValue) {
        String ret = getRawQueryParameter(uri, queryParameterName);
        if (!TextUtils.isEmpty(ret)) {
            //兼容Uri.decode会将"+"解码成""
            ret = rewriteUriDecode(ret);
        } else {
            ret = defaultValue;
        }
        return ret;
    }

    /**
     * 通过URI里的各个部分，构造一个URI对象
     * @param scheme scheme
     * @param host host名
     * @param port 端口号
     * @param path 路径
     * @param query 参数
     * @param fragment 片段
     * @return 新构造的URI对象
     * @throws URISyntaxException 如果格式有问题，会抛错误
     */
    public static URI createURI(
            final String scheme,
            final String host,
            int port,
            final String path,
            final String query,
            final String fragment) throws URISyntaxException {

        StringBuilder buffer = new StringBuilder();
        if (host != null) {
            if (scheme != null) {
                buffer.append(scheme);
                buffer.append("://");
            } else {
                buffer.append("//");
            }
            buffer.append(host);
            if (port > 0) {
                buffer.append(':');
                buffer.append(port);
            }
        }
        if (path == null || !path.startsWith("/")) {
            buffer.append('/');
        }
        if (path != null) {
            buffer.append(path);
        }
        if (query != null) {
            buffer.append('?');
            buffer.append(query);
        }
        if (fragment != null) {
            buffer.append('#');
            buffer.append(fragment);
        }
        return new URI(buffer.toString());
    }

    /**
     * 为url添加query参数，键为key， 其值为value
     * @param url 原始url
     * @param key 键
     * @param value 值
     * @return 添加了新参数的url
     */
    public static final String addQueryParameterToUrl(String url, String key, String value) {
        String ret = url;

        if ((!TextUtils.isEmpty(url)) && (!TextUtils.isEmpty(key)) && (!TextUtils.isEmpty(value))) {
            try {
                URI uri = new URI(url);
                List<Pair<String, String>> querys = parse(uri, "UTF-8");
                if (null == querys || querys.isEmpty()) {
                    querys = new ArrayList<Pair<String, String>>();
                }
                if (querys != null) {
                    Pair<String, String> findItem = new Pair<String, String>(key, value);
                    if (findItem != null) {
                        querys.add(findItem);
                        ret = format(querys, "UTF-8");
                        if (TextUtils.isEmpty(ret)) {
                            ret = createURI(uri.getScheme(), uri.getHost(), uri.getPort(),
                                    uri.getPath(), null, uri.getFragment()).toString();
                        } else {
                            ret = createURI(uri.getScheme(), uri.getHost(), uri.getPort(),
                                    uri.getPath(), ret, uri.getFragment()).toString();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return ret;
    }

    /**
     * 删除url中以key为键的参数
     * @param url 原始url
     * @param key 键值
     * @return 删除参数之后的url
     */
    public static String deleteQueryParameterFromUrl(String url, String key) {
        String ret = url;

        if ((!TextUtils.isEmpty(url)) && (!TextUtils.isEmpty(key))) {
            try {
                URI uri = new URI(url);
                List<Pair<String, String>> queries = parse(uri, "UTF-8");
                if ((null != queries) && (queries.size() > 0)) {
                    Pair<String, String> findItem = null;

                    for (Pair<String, String> query : queries) {
                        if (query.first.equals(key)) {
                            findItem = query;
                            break;
                        }
                    }

                    if (findItem != null) {
                        queries.remove(findItem);
                        ret = format(queries, "UTF-8");
                        if (TextUtils.isEmpty(ret)) {
                            ret = createURI(uri.getScheme(), uri.getHost(), uri.getPort(),
                                    uri.getPath(), null, uri.getFragment()).toString();
                        } else {
                            ret = createURI(uri.getScheme(), uri.getHost(), uri.getPort(),
                                    uri.getPath(), ret, uri.getFragment()).toString();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return ret;
    }

    private static final String PARAMETER_SEPARATOR = "&";

    private static final String NAME_VALUE_SEPARATOR = "=";

    /**
     * 将URI中的参数部分解析成键值对列表
     * 会去掉没有值的参数如,shopid=&nick=nike最终会变成nick=nike
     * @param uri 待解析URI对象
     * @return 键值对列表
     */
    public static List<Pair<String, String>> URLParseQuery(final URI uri) {
        List<Pair<String, String>> result = new ArrayList<Pair<String, String>>();

        final String query = uri.getRawQuery();
        if (!TextUtils.isEmpty(query)) {
            Scanner scanner = new Scanner(query);
            scanner.useDelimiter(PARAMETER_SEPARATOR);
            while (scanner.hasNext()) {
                final String[] nameValue = scanner.next().split(NAME_VALUE_SEPARATOR);
                if (nameValue.length != 2) {
                    //只取有数值的
                    continue;
                }

                final String name = nameValue[0];
                final String value = nameValue[1];
                result.add(new Pair<String, String>(name, value));
            }
            scanner.close();
        }

        return result;
    }

    /**
     * 将键值对列表转换成URI中的参数字符串
     * @param parameters 键值对列表
     * @return 键值对字符串
     */
    public static String URLFormatQuery(List<Pair<String, String>> parameters) {
        if ((null != parameters) && (parameters.size() > 0)) {
            final StringBuilder result = new StringBuilder();
            for (final Pair<String, String> parameter : parameters) {
                final String encodedName = parameter.first;
                final String value = parameter.second;
                final String encodedValue = (value != null ? value : "");
                if (result.length() > 0) {
                    result.append(PARAMETER_SEPARATOR);
                }
                result.append(encodedName);
                result.append(NAME_VALUE_SEPARATOR);
                result.append(encodedValue);
            }
            return result.toString();
        }

        return null;
    }

    /**
     * 将键值对列表转换成URI中的参数字符串，其中参数的value是经过URLEncode的，如果是utf-8编码，还会兼容将" "编码成+的问题
     * @param parameters 参数列表
     * @param encoding 编码
     * @return 键值对字符串
     */
    public static String URLFormatEncodedQuery(List<Pair<String, String>> parameters,
                                               String encoding) {
        if ((null != parameters) && (parameters.size() > 0)) {
            final StringBuilder result = new StringBuilder();
            for (final Pair<String, String> parameter : parameters) {
                try {
                    final String encodedName = URLEncoder.encode(parameter.first, encoding);
                    String value = parameter.second;
                    String encodedValue = null;
                    if (null != value) {
                        encodedValue = URLEncoder.encode(value, encoding);
                        //兼容URLEncoder.encode把“ ”编码成"+"
                        if ("UTF-8".equalsIgnoreCase(encoding)) {
                            encodedValue = encodedValue.replace("+", "%20");
                        }
                    } else {
                        encodedValue = "";
                    }
                    if (result.length() > 0) {
                        result.append(PARAMETER_SEPARATOR);
                    }
                    result.append(encodedName);
                    result.append(NAME_VALUE_SEPARATOR);
                    result.append(encodedValue);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            return result.toString();
        }

        return null;
    }

    /**
     * 将键值对map转换成URI中的参数字符串，其中参数的value是经过URLEncode的，如果是utf-8编码，还会兼容将" "编码成+的问题
     * @param parameters 参数列表
     * @return 键值对参数字符串
     */
    public static String processProtocolParameters(HashMap<String, String> parameters) {
        String query = null;
        if ((null != parameters) && (parameters.size() > 0)) {
            List<Pair<String, String>> aquerys = new ArrayList<Pair<String, String>>();
            Iterator<String> keys = parameters.keySet().iterator();

            while (keys.hasNext()) {
                String key = keys.next();
                String value = parameters.get(key);
                Pair<String, String> queryItem = new Pair<String, String>(key, value);
                aquerys.add(queryItem);
            }

            query = URLFormatEncodedQuery(aquerys, "UTF-8");
            aquerys.clear();
        }

        return query;
    }

    //+ urlencode %2B
    //+ urldecode " "
    //" " urlencode %20

    /**
     * 正常情况下，"+"的URLEncode编码是%2B，" "的URLEncode编码是%20<br />
     * 在JAVA中，URLEncode方法会将" "编码成"+"<br />
     * 在我们的框架里，要求兼容URLEncoder“ ”编码成"+"，因此会将encode结果里的"+"替换成%20<br />
     * @param s 原始字符串
     * @return 编码后的字符串
     */
    public static String rewriteUriEncode(String s) {
        if (TextUtils.isEmpty(s)) {
            return s;
        }

        //兼容URLEncoder.encode把“ ”编码成"+"
        String encodedValue = Uri.encode(s);
        encodedValue = encodedValue.replace("+", "%20");

        return encodedValue;
    }

    /**
     * 正常情况下，"+"的URLEncode编码是%2B，" "的URLEncode编码是%20<br />
     * 在JAVA中，URLEncode方法会将" "编码成"+"<br />
     * 在我们的框架里，要求兼容URLDecoder"+"解码成" "，因此在decode之前将+替换成%2B
     * @param s 原始字符串，经过编码的
     * @return 解码后的字符串
     */
    public static String rewriteUriDecode(String s) {
        if (TextUtils.isEmpty(s)) {
            return s;
        }

        //兼容Uri.decode会将"+"解码成" "
        String decodeValue = s.replace("+", "%2B");
        decodeValue = Uri.decode(decodeValue);

        return decodeValue;
    }

    /**
     * 获取url中的host，简单靠谱的方法
     * 比如：<br />
     * 输入http://www.stackoverflow.com，输出www.stackoverflow.com
     * 输入//www.stackoverflow.com，输出www.stackoverflow.com
     * @param url 原始url
     * @return host
     */
    public static String getHost(String url){
        if(url == null || url.length() == 0)
            return "";

        int doubleslash = url.indexOf("//");
        if(doubleslash == -1)
            doubleslash = 0;
        else
            doubleslash += 2;

        int end = url.indexOf('/', doubleslash);
        end = end >= 0 ? end : url.length();

        int port = url.indexOf(':', doubleslash);
        end = (port > 0 && port < end) ? port : end;

        return url.substring(doubleslash, end);
    }


    /**
     * 获取url链接中的域名，比如输入http://www.stackoverflow.com，那么输出stackoverflow.com。
     * 基于<a href="http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/2.3.3_r1/android/webkit/CookieManager.java#CookieManager.getBaseDomain%28java.lang.String%29">CookieManager源码</a>
     * @param url 原始url
     * @return 域名
     */
    public static String getBaseDomain(String url) {
        String host = getHost(url);

        int startIndex = 0;
        int nextIndex = host.indexOf('.');
        int lastIndex = host.lastIndexOf('.');
        while (nextIndex < lastIndex) {
            startIndex = nextIndex + 1;
            nextIndex = host.indexOf('.', startIndex);
        }
        if (startIndex > 0) {
            return host.substring(startIndex);
        } else {
            return host;
        }
    }

    /**
     * Returns a String that is suitable for use as an <code>application/x-www-form-urlencoded</code>
     * list of parameters in an HTTP PUT or HTTP POST.
     *
     * @param parameters  The parameters to include.
     * @param encoding The encoding to use.
     */
    public static String format (
            final List <Pair<String, String>> parameters,
            final String encoding) {
        final StringBuilder result = new StringBuilder();
        for (final Pair<String, String> parameter : parameters) {
            final String encodedName = encode(parameter.first, encoding);
            final String value = parameter.second;
            final String encodedValue = value != null ? encode(value, encoding) : "";
            if (result.length() > 0)
                result.append(PARAMETER_SEPARATOR);
            result.append(encodedName);
            result.append(NAME_VALUE_SEPARATOR);
            result.append(encodedValue);
        }
        return result.toString();
    }

    /**
     * Returns a list of {@link Pair<String, String> NameValuePairs} as built from the
     * URI's query portion. For example, a URI of
     * http://example.org/path/to/file?a=1&b=2&c=3 would return a list of three
     * NameValuePairs, one for a=1, one for b=2, and one for c=3.
     * <p>
     * This is typically useful while parsing an HTTP PUT.
     *
     * @param uri
     *            uri to parse
     * @param encoding
     *            encoding to use while parsing the query
     */
    public static List <Pair<String, String>> parse (final URI uri, final String encoding) {
        List <Pair<String, String>> result = new ArrayList<Pair<String, String>>();
        final String query = uri.getRawQuery();
        if (query != null && query.length() > 0) {
            parse(result, new Scanner(query), encoding);
        }
        return result;
    }

    /**
     * Adds all parameters within the Scanner to the list of
     * <code>parameters</code>, as encoded by <code>encoding</code>. For
     * example, a scanner containing the string <code>a=1&b=2&c=3</code> would
     * add the {@link Pair<String, String> NameValuePairs} a=1, b=2, and c=3 to the
     * list of parameters.
     *
     * @param parameters
     *            List to add parameters to.
     * @param scanner
     *            Input that contains the parameters to parse.
     * @param encoding
     *            Encoding to use when decoding the parameters.
     */
    public static void parse (
            final List<Pair<String, String>> parameters,
            final Scanner scanner,
            final String encoding) {
        scanner.useDelimiter(PARAMETER_SEPARATOR);
        while (scanner.hasNext()) {
            final String[] nameValue = scanner.next().split(NAME_VALUE_SEPARATOR);
            if (nameValue.length == 0 || nameValue.length > 2)
                throw new IllegalArgumentException("bad parameter");

            final String name = decode(nameValue[0], encoding);
            String value = null;
            if (nameValue.length == 2)
                value = decode(nameValue[1], encoding);
            parameters.add(new Pair<String, String>(name, value));
        }
    }

    private static String decode (final String content, final String encoding) {
        try {
            return URLDecoder.decode(content,
                    encoding != null ? encoding : "UTF-8");
        } catch (UnsupportedEncodingException problem) {
            throw new IllegalArgumentException(problem);
        }
    }

    private static String encode (final String content, final String encoding) {
        try {
            return URLEncoder.encode(content,
                    encoding != null ? encoding : "UTF-8");
        } catch (UnsupportedEncodingException problem) {
            throw new IllegalArgumentException(problem);
        }
    }
}
