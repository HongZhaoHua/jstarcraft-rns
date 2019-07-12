package com.jstarcraft.rns.search.hanlp;

import com.hankcs.hanlp.dictionary.py.Pinyin;

import java.util.List;

/**
 * 拼音字符串
 */
public interface HanlpPinyinConverter {

    CharSequence convert(String text, List<Pinyin> pinyin);

    /**
     * 全拼,HanLP.convertToPinyinString
     */
    class ToPinyinString implements HanlpPinyinConverter {
        private StringBuilder buffer = new StringBuilder(32);

        @Override
        public CharSequence convert(String text, List<Pinyin> pinyin) {
            buffer.setLength(0);
            for (Pinyin p : pinyin) {
                if (p != Pinyin.none5) {
                    buffer.append(p.getPinyinWithoutTone());
                }
            }
            return buffer;
        }
    }

    /**
     * 首字母,HanLP.convertToPinyinFirstCharString
     */
    class ToPinyinFirstCharString implements HanlpPinyinConverter {
        private StringBuilder buffer = new StringBuilder(32);

        @Override
        public CharSequence convert(String text, List<Pinyin> pinyin) {
            buffer.setLength(0);
            for (Pinyin p : pinyin) {
                if (p != Pinyin.none5) {
                    buffer.append(p.getFirstChar());
                }
            }
            return buffer;
        }
    }

}
