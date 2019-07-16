package com.jstarcraft.rns.search;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.junit.Assert;
import org.junit.Test;

public class SearcherTestCase {

    @Test
    public void testMergeManager() throws Exception {
        IndexWriterConfig config = new IndexWriterConfig();

        Path path = Paths.get("./lucene");
        File file = path.toFile();
        FileUtils.deleteDirectory(file);
        Searcher searcher = new Searcher(config, path);

        for (int index = 0; index < 1000; index++) {
            String data = String.valueOf(index);
            Document document = new Document();
            Field field = new StringField("title", data, Store.NO);
            document.add(field);
            searcher.createDocument(data, document);
        }
        Assert.assertEquals(1000, searcher.countDocuments(new MatchAllDocsQuery()));
        Assert.assertEquals(1, searcher.countDocuments(new TermQuery(new Term("title", "0"))));
        Assert.assertEquals(0, searcher.countDocuments(new TermQuery(new Term("title", "1000"))));
        searcher.mergeManager();
        Assert.assertEquals(1000, searcher.countDocuments(new MatchAllDocsQuery()));
        Assert.assertEquals(1, searcher.countDocuments(new TermQuery(new Term("title", "0"))));
        Assert.assertEquals(0, searcher.countDocuments(new TermQuery(new Term("title", "1000"))));

        for (int index = 0; index < 1000; index++) {
            String data = String.valueOf(index % 2);
            Document document = new Document();
            Field field = new StringField("title", data, Store.NO);
            document.add(field);
            searcher.updateDocument(String.valueOf(index), document);
        }
        Assert.assertEquals(1000, searcher.countDocuments(new MatchAllDocsQuery()));
        Assert.assertEquals(500, searcher.countDocuments(new TermQuery(new Term("title", "0"))));
        Assert.assertEquals(500, searcher.countDocuments(new TermQuery(new Term("title", "1"))));
        searcher.mergeManager();
        Assert.assertEquals(1000, searcher.countDocuments(new MatchAllDocsQuery()));
        Assert.assertEquals(500, searcher.countDocuments(new TermQuery(new Term("title", "0"))));
        Assert.assertEquals(500, searcher.countDocuments(new TermQuery(new Term("title", "1"))));

        for (int index = 0; index < 500; index++) {
            searcher.deleteDocument(String.valueOf(index));
        }

        Assert.assertEquals(500, searcher.countDocuments(new MatchAllDocsQuery()));
        Assert.assertEquals(250, searcher.countDocuments(new TermQuery(new Term("title", "0"))));
        Assert.assertEquals(250, searcher.countDocuments(new TermQuery(new Term("title", "1"))));
        searcher.mergeManager();
        Assert.assertEquals(500, searcher.countDocuments(new MatchAllDocsQuery()));
        Assert.assertEquals(250, searcher.countDocuments(new TermQuery(new Term("title", "0"))));
        Assert.assertEquals(250, searcher.countDocuments(new TermQuery(new Term("title", "1"))));
        
        searcher.close();
        FileUtils.deleteDirectory(file);
    }

}
