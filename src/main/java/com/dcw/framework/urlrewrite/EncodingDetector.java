package com.dcw.framework.urlrewrite;

import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * create by adao12.vip@gmail.com on 15/12/14
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public class EncodingDetector {

    private static final Pattern UTF_8_PATTERN = Pattern.compile("^([\\x00-\\x7f]|[\\xe0-\\xef]|[\\x80-\\xbf])+$");
    private static HashSet<String> sBadCaseSets = new HashSet<String>();

    static {
        sBadCaseSets.add("鏈條");
        sBadCaseSets.add("瑷媄");
        sBadCaseSets.add("妤媞");
        sBadCaseSets.add("浜叉鼎");
        sBadCaseSets.add("蟹");
    }
    
    /**
     * 使用nio的库进行编码检测，输入编码后的中文字符串，输出UTF-8或者GBK
     *
     * @param inputStr 待检测的文本
     * @return 编码字符串
     */
    public static String decGBKorUTF8(String inputStr) {

        try {

            if (TextUtils.isEmpty(inputStr)) {
                return "UTF-8";
            }

            // 因为GBK码表少 所以优先GBK
            String pureValue = inputStr;//new String(bytes, "ISO-8859-1");
            String gbkValue = URLDecoder.decode(inputStr, "GBK");//new String(bytes, "GBK");
            String utfValue = URLDecoder.decode(inputStr,"UTF-8");//new String(bytes, "UTF-8");

            boolean canGbkEncode = java.nio.charset.Charset.forName("GBK").newEncoder().canEncode(gbkValue);
            boolean canUtfEncode = java.nio.charset.Charset.forName("GBK").newEncoder().canEncode(utfValue);

            if (canGbkEncode && canUtfEncode) {
                // 原来是gbk的字符串被错误认为是UTF8
                Matcher matcher = UTF_8_PATTERN.matcher(pureValue);
                if (matcher.matches()) {
                    // badcase额外的列表 上面的编码表还是有覆盖不全的情况
                    if (sBadCaseSets.contains(gbkValue)) {
                        return "GBK";
                    }
                    return "UTF-8";
                } else {
                    return "GBK";
                }
            } else if (canGbkEncode) {
                return "GBK";
            } else if (canUtfEncode) {
                return "UTF-8";
            }
        } catch (UnsupportedEncodingException e) {
            // IGNORE 不可能出问题的地方
            e.printStackTrace();
        }

        return "UTF-8";
    }
}
