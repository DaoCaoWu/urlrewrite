package com.dcw.framework.urlrewrite.domain;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * create by adao12.vip@gmail.com on 15/12/12
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public class RuleItem {

    public String originUrl;

    public String newUrl;

    public HashSet<String> flag;

    public RuleItem() {
    }

    public RuleItem(JSONObject data) {
        if (data != null) {
            originUrl = data.optString("origin");
            newUrl = data.optString("new");
            flag = new HashSet<String>();
            String flags = data.optString("flag");
            if (flags != null) {
                String[] flagArray = flags.split(",");
                for (String s : flagArray) {
                    s = s != null ? s.trim() : "";
                    if (s.length() > 0) {
                        flag.add(s);
                    }
                }
            }
        }
    }

    public static ArrayList<RuleItem> parse(JSONArray dataArray) {

        ArrayList<RuleItem> ret = new ArrayList<RuleItem>();
        for (int i = 0, len = dataArray.length(); i < len; i++) {
            JSONObject obj = (JSONObject) dataArray.opt(i);
            RuleItem item = new RuleItem(obj);
            ret.add(item);
        }

        return ret;
    }

}
