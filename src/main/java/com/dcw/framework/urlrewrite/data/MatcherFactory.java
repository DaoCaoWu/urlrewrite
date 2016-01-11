package com.dcw.framework.urlrewrite.data;

import android.support.annotation.NonNull;

import com.dcw.framework.urlrewrite.NGPatternMatcher;
import com.dcw.framework.urlrewrite.domain.RuleItem;

import java.util.HashMap;
import java.util.Map;

/**
 * create by adao12.vip@gmail.com on 15/12/29
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public class MatcherFactory implements NGPatternMatcher.Factory {

    private Map<String, NGPatternMatcher> mPatternCache;

    public MatcherFactory() {
        mPatternCache = new HashMap<String, NGPatternMatcher>();
    }

    public MatcherFactory(@NonNull Map<String, NGPatternMatcher> patternCache) {
        mPatternCache = patternCache;
    }

    @Override
    public NGPatternMatcher create(RuleItem ruleItem) {
        NGPatternMatcher rulePattern = mPatternCache.get(ruleItem.originUrl);
        if (rulePattern == null) {
            rulePattern = new NGPatternMatcher(ruleItem.originUrl);
            mPatternCache.put(ruleItem.originUrl, rulePattern);
        }
        return rulePattern;
    }


}
