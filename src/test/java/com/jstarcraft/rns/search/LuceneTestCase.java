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
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.QueryBuilder;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.rns.search.hanlp.HanlpIndexAnalyzer;
import com.jstarcraft.rns.search.hanlp.HanlpQueryAnalyzer;

public class LuceneTestCase {

    @Test
    public void test() throws Exception {
        Directory directory = new ByteBuffersDirectory();

        {
            Analyzer analyzer = new HanlpIndexAnalyzer();
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
                        // 电影URL
                        datas.get(4);
                        indexWriter.addDocument(document);
                    }
                }
            }
            indexWriter.close();
        }

        {
            Analyzer analyzer = new HanlpQueryAnalyzer();
            IndexReader indexReader = DirectoryReader.open(directory);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            QueryBuilder builder = new QueryBuilder(analyzer);
            Query query = builder.createBooleanQuery("title", "stories");
            Sort sort = new Sort(new SortField("date", SortField.Type.STRING, true));
            TopDocs topDocs = indexSearcher.search(query, 10, sort);
            Assert.assertEquals(8, topDocs.totalHits.value);
            Document document = indexSearcher.doc(topDocs.scoreDocs[0].doc);
            Assert.assertEquals("FairyTale: A True Story (1997)", document.get("title"));
            indexReader.close();
        }
    }

}
