package com.jstarcraft.rns.search.converter;

import java.time.Instant;
import java.util.TreeSet;

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

import com.jstarcraft.core.utility.StringUtility;

public class ConverterTestCase {

    @Test
    public void testEncode() throws Exception {
        Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new WhitespaceAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        SearchCodec<MockComplexObject, MockComplexObject> codec = new SearchCodec<>(MockComplexObject.class, MockComplexObject.class);
        int size = 100;
        Instant now = Instant.now();
        MockComplexObject protoss = MockComplexObject.instanceOf(Integer.MIN_VALUE, "protoss", "jstarcraft", size, now, MockEnumeration.PROTOSS);
        MockComplexObject terran = MockComplexObject.instanceOf(0, "terran", "jstarcraft", size, now, MockEnumeration.TERRAN);
        MockComplexObject zerg = MockComplexObject.instanceOf(Integer.MAX_VALUE, "zerg", "jstarcraft", size, now, MockEnumeration.ZERG);

        indexWriter.addDocument(codec.encode(protoss));
        indexWriter.addDocument(codec.encode(terran));
        indexWriter.addDocument(codec.encode(zerg));

        IndexReader indexReader = DirectoryReader.open(indexWriter);

        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        TopDocs search = indexSearcher.search(IntPoint.newExactQuery("id", 0), 1000);
        for (ScoreDoc scoreDoc : search.scoreDocs) {
            Document document = indexReader.document(scoreDoc.doc);

            System.err.println(StringUtility.reflect(codec.decode(document)));

            document.forEach((field) -> {
                System.out.println(field.getClass());
                System.out.println(field.name());
            });
        }

        TreeSet<String> tree = new TreeSet<>();
        tree.add("lastName");
        tree.add("id");
        tree.add("firstName");
        tree.add("names[0]");
        tree.add("names[1]");
        tree.add("list[0].name");
        tree.add("list[0].id");
        tree.add("list[1].name");
        tree.add("list[1].id");
        tree.add("list");
        tree.add("lisu");

        System.out.println(tree);
        char last = "lisz".charAt("lisz".length() - 1);
        last += 1;
        System.out.println("list".substring(0, "list".length() - 1) + last);
        System.out.println(tree.subSet("list", true, "list".substring(0, "list".length() - 1) + last, false));

        indexReader.close();
        indexWriter.close();
    }

}
