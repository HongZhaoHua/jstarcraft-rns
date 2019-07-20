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
import org.junit.Test;

public class ConverterTestCase {

    @Test
    public void testEncode() throws Exception {
        int size = 100;
        MockComplexInput instance = MockComplexInput.instanceOf(Integer.MAX_VALUE, "birdy", "hong", size, Instant.now(), MockEnumeration.TERRAN);
        SearchCodec<MockComplexInput, MockComplexInput> codec = new SearchCodec<>(MockComplexInput.class, MockComplexInput.class);
        Document document = codec.encode(instance);

        Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new WhitespaceAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);
        indexWriter.addDocument(document);

        IndexReader indexReader = DirectoryReader.open(indexWriter);

        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        TopDocs search = indexSearcher.search(IntPoint.newExactQuery("id", Integer.MAX_VALUE), 1000);
        for (ScoreDoc scoreDoc : search.scoreDocs) {
            document = indexReader.document(scoreDoc.doc);
            document.forEach((field) -> {
                System.out.println(field.getClass());
                System.out.println(field.name());
            });
        }
    }

}
