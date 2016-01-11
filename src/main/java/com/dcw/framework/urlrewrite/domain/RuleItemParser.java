package com.dcw.framework.urlrewrite.domain;

import org.json.JSONObject;

import java.util.HashSet;

/**
 * create by adao12.vip@gmail.com on 15/12/31
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public class RuleItemParser implements Converter<JSONObject, RuleItem> {

    @Override
    public RuleItem convert(JSONObject value) {
        RuleItem ruleItem = new RuleItem();
        ruleItem.originUrl = value.optString("origin");
        ruleItem.newUrl = value.optString("new");
        ruleItem.flag = new HashSet<String>();
        String flags = value.optString("flag");
        if (flags != null) {
            String[] flagArray = flags.split(",");
            for (String s : flagArray) {
                s = s != null ? s.trim() : "";
                if (s.length() > 0) {
                    ruleItem.flag.add(s);
                }
            }
        }
        return ruleItem;
    }
}
