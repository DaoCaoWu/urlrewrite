package com.dcw.framework.urlrewrite;

import com.dcw.framework.urlrewrite.domain.RuleItem;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * create by adao12.vip@gmail.com on 15/12/14
 *
 * @author JiaYing.Cheng
 * @version 1.0
 */
public class NGPatternMatcher {

    private Matcher mMatcher;

    public NGPatternMatcher(String pattern) {
        mMatcher = Pattern.compile(pattern).matcher("");
    }

    public Matcher reset(CharSequence input) {
        return mMatcher.reset(input);
    }

    public interface Factory {

        NGPatternMatcher create(RuleItem ruleItem);
    }
}
