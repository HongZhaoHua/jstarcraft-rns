package com.jstarcraft.rns.search.converter;

import java.time.Instant;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.core.utility.StringUtility;

public class ConverterTestCase {

    @Test
    public void testEncode() throws Exception {
        Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new WhitespaceAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        SearchCodec<MockComplexObject, MockComplexObject> codec = new SearchCodec<>(MockComplexObject.class, MockComplexObject.class);
        int size = 5;
        Instant now = Instant.now();
        MockComplexObject protoss = MockComplexObject.instanceOf(-1, "protoss", "jstarcraft", size, now, MockEnumeration.PROTOSS);
        MockComplexObject terran = MockComplexObject.instanceOf(0, "terran", "jstarcraft", size, now, MockEnumeration.TERRAN);
        MockComplexObject zerg = MockComplexObject.instanceOf(1, "zerg", "jstarcraft", size, now, MockEnumeration.ZERG);
        MockComplexObject[] objects = new MockComplexObject[] { protoss, terran, zerg };

        indexWriter.addDocument(codec.encode(protoss));
        indexWriter.addDocument(codec.encode(terran));
        indexWriter.addDocument(codec.encode(zerg));

        IndexReader indexReader = DirectoryReader.open(indexWriter);

        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        TopDocs search = indexSearcher.search(IntPoint.newRangeQuery("id", -1, 1), 1000);
        int index = 0;
        for (ScoreDoc scoreDoc : search.scoreDocs) {
            Document document = indexReader.document(scoreDoc.doc);
            Assert.assertEquals(objects[index++], codec.decode(document));
        }

        indexReader.close();
        indexWriter.close();
    }

}
