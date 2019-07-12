package com.jstarcraft.rns.search;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorable;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

/**
 * 瞬时化搜索器
 * 
 * @author Birdy
 *
 */
public class TransienceManager implements LuceneManager {

    private AtomicBoolean changed = new AtomicBoolean(false);

    /** 创建标识 */
    // TODO 准备重构为Object2LongMap,支持查询
    private Set<String> createdIds;

    /** 更新标识 */
    // TODO 准备重构为Object2LongMap,支持查询
    private Object2LongMap<String> updatedIds;

    /** 删除标识 */
    // TODO 准备重构为Object2LongMap,支持查询
    private Set<String> deletedIds;

    private IndexWriterConfig config;

    private Directory directory;

    private DirectoryReader reader;

    private IndexWriter writer;

    public TransienceManager(IndexWriterConfig config) throws Exception {
        this.createdIds = new HashSet<>();
        this.updatedIds = new Object2LongOpenHashMap<>();
        this.deletedIds = new HashSet<>();

        this.config = config;
        this.directory = new ByteBuffersDirectory();
        this.writer = new IndexWriter(this.directory, this.config);
        this.reader = DirectoryReader.open(this.writer);
    }

    Set<String> getCreatedIds() {
        // TODO Auto-generated method stub
        return null;
    }

    Object2LongMap<String> getUpdatedIds() {
        // TODO Auto-generated method stub
        return null;
    }

    Set<String> getDeletedIds() {
        // TODO Auto-generated method stub
        return null;
    }

    void createDocuments(Document... documents) throws Exception {
        for (Document document : documents) {
            String id = getId(document);
            if (this.deletedIds.remove(id)) {
                long version = getVersion(document);
                this.updatedIds.put(id, version);
            } else {
                this.createdIds.add(id);
            }
            this.writer.addDocument(document);
        }
    }

    void updateDocuments(Document... documents) throws Exception {
        for (Document document : documents) {
            String id = getId(document);
            long version = getVersion(document);
            this.updatedIds.put(id, version);
            this.writer.addDocument(document);
        }
    }

    void deleteDocuments(String... ids) throws Exception {
        for (String id : ids) {
            if (this.createdIds.remove(id)) {
                Term term = new Term(ID, id);
                this.writer.deleteDocuments(term);
            } else {
                this.deletedIds.add(id);
            }
        }
    }

    @Override
    public void open() {
        // TODO Auto-generated method stub

    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isChanged() {
        return changed.get();
    }

    @Override
    public LeafCollector getCollector(LeafReaderContext context, LeafCollector collector) throws IOException {
        LeafReader reader = context.reader();
        BinaryDocValues ids = DocValues.getBinary(reader, TransienceManager.ID);
        NumericDocValues versions = DocValues.getNumeric(reader, TransienceManager.VERSION);

        return new LeafCollector() {

            @Override
            public void setScorer(Scorable scorer) throws IOException {
                collector.setScorer(scorer);
            }

            @Override
            public void collect(int index) throws IOException {
                ids.advanceExact(index);
                String id = ids.binaryValue().utf8ToString();
                if (TransienceManager.this.deletedIds.contains(id)) {
                    return;
                }
                long newVersion = TransienceManager.this.updatedIds.get(id);
                if (newVersion != 0) {
                    versions.advanceExact(index);
                    long oldVersion = versions.longValue();
                    if (newVersion != oldVersion) {
                        return;
                    }
                }
                collector.collect(index);
            }

        };
    }

    @Override
    public Directory getDirectory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IndexReader getReader() throws Exception {
        if (changed.compareAndSet(true, false)) {
            this.writer.flush();
            DirectoryReader reader = DirectoryReader.openIfChanged(this.reader);
            if (reader != null) {
                this.reader.close();
                this.reader = reader;
            }
        }
        return this.reader;
    }

    @Override
    public IndexWriter getWriter() {
        return writer;
    }

}
