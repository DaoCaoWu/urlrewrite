package com.dcw.framework.urlrewrite.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * create by adao12.vip@gmail.com on 15/12/12
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public class Rule {

    public String name;

    public List<RuleItem> items;

    public Rule() {
        items = new ArrayList<RuleItem>();
    }

    public List<RuleItem> getItems() {
        return items;
    }

    public int size() {
        return items == null ? 0 : items.size();
    }

    public Rule(JSONObject data) {
        name = data.optString("name");
        JSONArray dataArray = data.optJSONArray("data");
        if (null != dataArray) {
            items = RuleItem.parse(dataArray);
        }
    }

    public static Rule parseNewConfig(String moduleName, ArrayList<String> configData) {
        Rule ret = new Rule();
        if ((null != configData) && (configData.size() > 0)) {
            ret.name = moduleName;
            for (int i = 0, len = configData.size(); i < len; i++) {
                String itemS = configData.get(i);
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(itemS);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (null != jsonObject) {
                    RuleItem item = new RuleItem(jsonObject);
                    ret.items.add(item);
                }
            }
        }
        return ret;
    }

    public interface Factory {

        Rule getRule();
    }
}
