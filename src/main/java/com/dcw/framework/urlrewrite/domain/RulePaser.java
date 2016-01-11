package com.dcw.framework.urlrewrite.domain;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * create by adao12.vip@gmail.com on 15/12/31
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public class RulePaser implements Converter<JSONObject, Rule> {

    private Converter<JSONArray, List<RuleItem>> mRuleItemParser;

    public RulePaser(Converter<JSONArray, List<RuleItem>> ruleItemParser) {
        this.mRuleItemParser = ruleItemParser;
    }

    @Override
    public Rule convert(JSONObject value) {
        Rule rule = new Rule();
        rule.name = value.optString("name");
        JSONArray dataArray = value.optJSONArray("data");
        if (null != dataArray) {
            rule.items = mRuleItemParser.convert(dataArray);
        }
        return rule;
    }
}
