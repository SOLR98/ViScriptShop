package com.viscriptshop.compat;

import com.viscriptshop.ViscriptShop;
import me.towdium.jecharacters.utils.Match;

public final class JechHelper {
    public static boolean containsIgnoreCase(CharSequence text, CharSequence query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        if (text == null || text.isEmpty()) {
            return false;
        }
        if (!ViscriptShop.isJECharactersLoaded()) {
            return false;
        }

        try {
            return Match.contains(text, query, true);
        } catch (Throwable e) {
            ViscriptShop.LOGGER.warn("JECH 拼音搜索调用失败，已回退到普通文本匹配", e);
            return false;
        }
    }
}
