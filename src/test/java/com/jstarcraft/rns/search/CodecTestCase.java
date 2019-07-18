package com.jstarcraft.rns.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.Test;

public class CodecTestCase {

    @Test
    public void testEncode() throws Exception {
        MockSimpleInput input = MockSimpleInput.instanceOf(0, "Mock");
        SearchCodec<MockSimpleInput, Object> codec = new SearchCodec<>(MockSimpleInput.class, Object.class);
        Document document = codec.encode(input);
        System.out.println(document.get("id"));
        System.out.println(document.get("name"));

        FieldType type = new FieldType();
        type.setOmitNorms(true);
        type.setIndexOptions(IndexOptions.DOCS);
        type.setDocValuesType(DocValuesType.BINARY);
        type.setStored(true);
        type.setTokenized(true);
        type.freeze();

//        Document document = new Document();
//        IndexableField field = null;
//        field = new Field("_id", "id", type);
//        document.add(field);

        Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new WhitespaceAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);
        indexWriter.addDocument(document);
    }

}
