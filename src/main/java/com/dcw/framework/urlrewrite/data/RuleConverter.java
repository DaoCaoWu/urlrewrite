package com.dcw.framework.urlrewrite.data;

import com.dcw.framework.urlrewrite.domain.Converter;
import com.dcw.framework.urlrewrite.domain.Rule;
import com.dcw.framework.urlrewrite.domain.RuleItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * create by adao12.vip@gmail.com on 15/12/29
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public class RuleConverter implements Converter<JSONArray, Rule> {

    @Override
    public Rule convert(JSONArray data) {
        Rule rule = new Rule();
        ArrayList<RuleItem> ret = new ArrayList<RuleItem>();
        for (int i = 0, len = data.length(); i < len; i++) {
            JSONObject obj = (JSONObject) data.opt(i);
            RuleItem item = new RuleItem(obj);
            ret.add(item);
        }
        rule.items = ret;
        return rule;
    }
}
