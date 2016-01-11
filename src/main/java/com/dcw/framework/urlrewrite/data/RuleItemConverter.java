package com.dcw.framework.urlrewrite.data;

import com.dcw.framework.urlrewrite.domain.Converter;
import com.dcw.framework.urlrewrite.domain.RuleItem;

import org.json.JSONObject;

import java.util.HashSet;

/**
 * create by adao12.vip@gmail.com on 15/12/29
 *
 * 把Json数据转换成RuleItem
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public class RuleItemConverter implements Converter<JSONObject, RuleItem> {

    @Override
    public RuleItem convert(JSONObject data) {
        RuleItem item = new RuleItem();
        if (data != null) {
            item.originUrl = data.optString("from");
            item.newUrl = data.optString("to");
            item.flag = new HashSet<String>();
            String flags = data.optString("flag");
            if (flags != null) {
                String[] flagArray = flags.split(",");
                for (String s : flagArray) {
                    s = s != null ? s.trim() : "";
                    if (s.length() > 0) {
                        item.flag.add(s);
                    }
                }
            }
        }
        return item;
    }
}
