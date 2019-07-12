package com.jstarcraft.rns.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FeatureField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanFirstQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.core.utility.StringUtility;

public class SearcherTestCase {

    private IndexSearcher searcher;

    {
        try {
            Directory directory = new ByteBuffersDirectory();
            Analyzer analyzer = new WhitespaceAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter indexWriter = new IndexWriter(directory, config);
            File file = new File(this.getClass().getResource("movie.csv").toURI());
            InputStream stream = new FileInputStream(file);
            String format = "dd-MMM-yyyy";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format, Locale.US);
            try (InputStreamReader reader = new InputStreamReader(stream); BufferedReader buffer = new BufferedReader(reader)) {
                try (CSVParser parser = new CSVParser(buffer, CSVFormat.newFormat('|'))) {
                    Iterator<CSVRecord> iterator = parser.iterator();
                    while (iterator.hasNext()) {
                        CSVRecord datas = iterator.next();
                        Document document = new Document();
                        // 电影标识
                        Field idField = new IntPoint("id", Integer.parseInt(datas.get(0)));
                        document.add(idField);
                        // 电影标题
                        Field titleField = new TextField("title", datas.get(1), Store.YES);
                        document.add(titleField);
                        // 电影日期
                        if (StringUtility.isEmpty(datas.get(2))) {
                            continue;
                        }
                        LocalDate date = LocalDate.parse(datas.get(2), formatter);
                        Field dateField = new SortedDocValuesField("date", new BytesRef(date.toString()));
                        document.add(dateField);
                        // 电影特征
                        Field feature = new FeatureField("feature", "score", Float.parseFloat(datas.get(0)));
                        document.add(feature);
                        indexWriter.addDocument(document);
                    }
                }
            }
            DirectoryReader directoryReader = DirectoryReader.open(indexWriter);
            searcher = new IndexSearcher(directoryReader);
        } catch (Exception exception) {

        }
    }

    // 测试查询

    @Test
    public void testMatchAllDocsQuery() throws Exception {
        // 全部匹配查询
        Query query = new MatchAllDocsQuery();
        TopDocs search = searcher.search(query, 1000000);
        Assert.assertEquals(1681, search.totalHits.value);
    }

    @Test
    public void testMatchNoDocsQuery() throws Exception {
        // 全不匹配查询
        Query query = new MatchNoDocsQuery();
        TopDocs search = searcher.search(query, 1000);
        Assert.assertEquals(0, search.totalHits.value);
    }

    // 词项查询

    @Test
    public void testTermQuery() throws Exception {
        // 词项查询
        Query query = new TermQuery(new Term("title", "Toy"));
        TopDocs search = searcher.search(query, 1000);
        Assert.assertEquals(1, search.totalHits.value);
    }

    @Test
    public void testTermRangeQuery() throws Exception {
        // 范围查询
        Query query = new TermRangeQuery("title", new BytesRef("Toa"), new BytesRef("Toz"), true, true);
        TopDocs search = searcher.search(query, 1000);
        Assert.assertEquals(22, search.totalHits.value);
    }

    @Test
    public void testPrefixQuery() throws Exception {
        // 前缀查询
        PrefixQuery query = new PrefixQuery(new Term("title", "Touc"));
        TopDocs search = searcher.search(query, 1000);
        Assert.assertEquals(2, search.totalHits.value);
    }

    @Test
    public void testWildcardQuery() throws Exception {
        // 通配符查询
        {
            // *代表0个或者多个字母
            Query query = new WildcardQuery(new Term("title", "*ouc*"));
            TopDocs search = searcher.search(query, 1000);
            Assert.assertEquals(2, search.totalHits.value);
        }
        {
            // ?代表0个或者1个字母
            Query query = new WildcardQuery(new Term("title", "?ouc?"));
            TopDocs search = searcher.search(query, 1000);
            Assert.assertEquals(2, search.totalHits.value);
        }
    }

    @Test
    public void testRegexpQuery() throws Exception {
        // 正则查询
        RegexpQuery query = new RegexpQuery(new Term("title", "To[a-z]"));
        TopDocs search = searcher.search(query, 1000);
        Assert.assertEquals(7, search.totalHits.value);
    }

    @Test
    public void testFuzzyQuery() throws Exception {
        // 模糊查询
        Query query = new FuzzyQuery(new Term("title", "Stori"));
        TopDocs search = searcher.search(query, 1000);
        Assert.assertEquals(32, search.totalHits.value);
    }

    // 短语查询

    @Test
    public void testPhraseQuery() throws Exception {
        // 短语查询
        // 设置短语之间的跨度为2,也就是说Story和The之间的短语小于等于均可检索
        PhraseQuery build = new PhraseQuery.Builder().setSlop(2).add(new Term("title", "Story")).add(new Term("title", "The")).build();
        TopDocs search = searcher.search(build, 1000);
        Assert.assertEquals(2, search.totalHits.value);
    }

    @Test
    public void testMultiPhraseQuery() throws Exception {
        // 多短语查询
        Term[] terms = new Term[] { new Term("title", "NeverEnding"), new Term("title", "Xinghua,") };
        Term term = new Term("title", "The");
        // add之间认为是OR操作,即"NeverEnding", "Xinghua,"和"The"之间的slop不大于3
        MultiPhraseQuery multiPhraseQuery = new MultiPhraseQuery.Builder().add(terms).add(term).setSlop(3).build();
        TopDocs search = searcher.search(multiPhraseQuery, 1000);
        Assert.assertEquals(2, search.totalHits.value);
    }

    @Test
    public void testSpanTermQuery() throws Exception {
        // 相当于TermQuery,区别是使用SpanTermQuery可以得到词项的跨度信息
        Query query = new SpanTermQuery(new Term("title", "Toy"));
        TopDocs search = searcher.search(query, 1000);
        Assert.assertEquals(1, search.totalHits.value);
    }

    @Test
    public void testSpanFirstQuery() throws Exception {
        // 临近查询(匹配域中[0,n]范围内的词项)
        SpanQuery spanQuery = new SpanTermQuery(new Term("title", "Story"));
        SpanFirstQuery firstQuery = new SpanFirstQuery(spanQuery, 5);
        TopDocs search = searcher.search(firstQuery, 1000);
        Assert.assertEquals(5, search.totalHits.value);
    }

    @Test
    public void testSpanNearQuery() throws Exception {
        // 跨度查询
        SpanQuery[] spanQueries = new SpanQuery[] { new SpanTermQuery(new Term("title", "The")), new SpanTermQuery(new Term("title", "Story")) };
        {
            // 不考虑顺序的情况
            SpanNearQuery nearQuery = new SpanNearQuery(spanQueries, 5, false);
            TopDocs search = searcher.search(nearQuery, 1000);
            Assert.assertEquals(3, search.totalHits.value);
        }
        {
            // 考虑顺序的情况
            SpanNearQuery nearQuery = new SpanNearQuery(spanQueries, 5, true);
            TopDocs search = searcher.search(nearQuery, 1000);
            Assert.assertEquals(1, search.totalHits.value);
        }
        {
            // 考虑间隔的情况
            {
                SpanNearQuery.Builder builder = SpanNearQuery.newOrderedNearQuery("title");
                builder.addClause(spanQueries[0]).addGap(2).setSlop(5).addClause(spanQueries[1]);
                SpanNearQuery nearQuery = builder.build();
                TopDocs search = searcher.search(nearQuery, 1000);
                Assert.assertEquals(1, search.totalHits.value);
            }
            {
                SpanNearQuery.Builder builder = SpanNearQuery.newOrderedNearQuery("title");
                builder.addClause(spanQueries[0]).addGap(3).setSlop(5).addClause(spanQueries[1]);
                SpanNearQuery nearQuery = builder.build();
                TopDocs search = searcher.search(nearQuery, 1000);
                Assert.assertEquals(0, search.totalHits.value);
            }
        }
    }

    @Test
    public void testSpanNotQuery() throws Exception {
        SpanQuery[] spanQueries = new SpanQuery[] { new SpanTermQuery(new Term("title", "The")), new SpanTermQuery(new Term("title", "Story")) };
        {
            SpanQuery spanQuery = new SpanTermQuery(new Term("title", "Angels"));
            SpanNearQuery nearQuery = new SpanNearQuery(spanQueries, 5, true);
            SpanNotQuery notQuery = new SpanNotQuery(nearQuery, spanQuery);
            TopDocs search = searcher.search(notQuery, 1000);
            Assert.assertEquals(1, search.totalHits.value);
        }
        {
            SpanQuery spanQuery = new SpanTermQuery(new Term("title", "Day"));
            SpanNearQuery nearQuery = new SpanNearQuery(spanQueries, 5, true);
            SpanNotQuery notQuery = new SpanNotQuery(nearQuery, spanQuery);
            TopDocs search = searcher.search(notQuery, 1000);
            Assert.assertEquals(0, search.totalHits.value);
        }
    }

    @Test
    public void testSpanOrQuery() throws Exception {
        SpanQuery[] spanQueries = new SpanQuery[] { new SpanTermQuery(new Term("title", "The")), new SpanTermQuery(new Term("title", "Story")) };
        SpanNotQuery leftQuery = null;
        {
            SpanQuery spanQuery = new SpanTermQuery(new Term("title", "Angels"));
            SpanNearQuery nearQuery = new SpanNearQuery(spanQueries, 5, true);
            leftQuery = new SpanNotQuery(nearQuery, spanQuery);
        }
        SpanNotQuery rightQuery = null;
        {
            SpanQuery spanQuery = new SpanTermQuery(new Term("title", "Day"));
            SpanNearQuery nearQuery = new SpanNearQuery(spanQueries, 5, true);
            rightQuery = new SpanNotQuery(nearQuery, spanQuery);
        }
        SpanOrQuery orQuery = new SpanOrQuery(new SpanQuery[] { leftQuery, rightQuery });
        TopDocs search = searcher.search(orQuery, 1000);
        Assert.assertEquals(1, search.totalHits.value);
    }

    // 数值查询

    @Test
    public void testPointExactQuery() throws Exception {
        // 精确查询
        Query exactQuery = IntPoint.newExactQuery("id", 1);
        TopDocs search = searcher.search(exactQuery, 1000);
        Assert.assertEquals(1, search.totalHits.value);
    }

    @Test
    public void testPointRangeQuery() throws Exception {
        // 范围查询
        Query rangeQuery = IntPoint.newRangeQuery("id", 501, 1000);
        TopDocs search = searcher.search(rangeQuery, 1000);
        Assert.assertEquals(500, search.totalHits.value);
    }

    @Test
    public void testPointSetQuery() throws Exception {
        // 集合查询
        Query setQuery = IntPoint.newSetQuery("id", 1, 10, 100, 1000);
        TopDocs search = searcher.search(setQuery, 1000);
        Assert.assertEquals(4, search.totalHits.value);
    }

    // 组合查询

    @Test
    public void testBooleanQuery() throws Exception {
        // 与或查询
        Query leftQuery = new TermQuery(new Term("title", "Story"));
        Query rightQuery = new TermQuery(new Term("title", "Toy"));
        {
            // 与查询
            BooleanQuery booleanQuery = new BooleanQuery.Builder().add(leftQuery, Occur.MUST).add(rightQuery, Occur.MUST).build();
            TopDocs search = searcher.search(booleanQuery, 1000);
            Assert.assertEquals(1, search.totalHits.value);
        }
        {
            // 或查询
            BooleanQuery booleanQuery = new BooleanQuery.Builder().add(leftQuery, Occur.SHOULD).add(rightQuery, Occur.SHOULD).build();
            TopDocs search = searcher.search(booleanQuery, 1000);
            Assert.assertEquals(6, search.totalHits.value);
        }
    }

    @Test
    public void testBoostQuery() throws Exception {
        Query leftQuery = new TermQuery(new Term("title", "Ace"));
        Query rightQuery = new TermQuery(new Term("title", "Toy"));
        // 两次查询排序相反
        Query booleanQuery = new BooleanQuery.Builder().add(leftQuery, Occur.SHOULD).add(rightQuery, Occur.SHOULD).build();
        {
            TopDocs search = searcher.search(booleanQuery, 1000);
            Assert.assertEquals(3, search.totalHits.value);
            Assert.assertEquals(0, search.scoreDocs[0].doc);
        }
        Query featureQuery = FeatureField.newSaturationQuery("feature", "score");
        Query boostedQuery = new BoostQuery(featureQuery, 10F);
        booleanQuery = new BooleanQuery.Builder().add(booleanQuery, Occur.MUST).add(boostedQuery, Occur.MUST).build();
        {
            TopDocs search = searcher.search(booleanQuery, 1000);
            Assert.assertEquals(3, search.totalHits.value);
            Assert.assertEquals(0, search.scoreDocs[2].doc);
        }
    }

    // 功能查询

    // 查询解析器

    @Test
    public void testQueryParser() throws Exception {
    }

}
