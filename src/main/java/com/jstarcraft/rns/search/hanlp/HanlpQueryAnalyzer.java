package com.jstarcraft.rns.search.hanlp;

import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.PorterStemFilter;

import com.hankcs.hanlp.HanLP;

/**
 * HanLP查询分析器(仅用于查询)
 * 
 * @author Birdy
 *
 */
public class HanlpQueryAnalyzer extends Analyzer {

    private Set<String> filter;

    public HanlpQueryAnalyzer(Set<String> filter) {
        this.filter = filter;
    }

    public HanlpQueryAnalyzer() {
        super();
    }

    /**
     * 重载Analyzer接口,构造分词组件
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer from = new HanlpTokenizer(HanLP.newSegment().enableOffset(true), filter);
        TokenStream to = new LowerCaseFilter(from);
        to = new PorterStemFilter(to);
        return new TokenStreamComponents(from, to);
    }

}
