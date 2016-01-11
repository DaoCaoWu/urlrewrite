package com.dcw.framework.urlrewrite.data;

import com.dcw.framework.urlrewrite.domain.Rule;
import com.dcw.framework.urlrewrite.domain.RuleItem;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * create by adao12.vip@gmail.com on 15/12/29
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public class RuleFactory implements Rule.Factory {

    String mConfigStr = "{  \n" +
            "   \"from\":\"^http:\\/\\/(?:[\\\\w-]+\\\\.)?9game\\\\.cn\\/personal\\/(\\\\d+)\\\\/(\\\\d+).html.*$\",\n" +
            "   \"to\":\"http:\\/\\/www.9game.cn\\/personal\\/%2$s\\/forum\\/%1$s.html\",\n" +
            "   \"flag\":0\n" +
            "}";

    public RuleFactory() {

    }

    @Override
    public Rule getRule() {
        Rule rule = new Rule();
        try {

            JSONObject jsonObject = new JSONObject(mConfigStr);
            RuleItemConverter ruleItemConverter = new RuleItemConverter();
            rule.items = new ArrayList<RuleItem>();
            rule.items.add(ruleItemConverter.convert(jsonObject));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rule;
    }
}
