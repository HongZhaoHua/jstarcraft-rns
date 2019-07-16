package com.jstarcraft.rns.search;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorable;
import org.apache.lucene.store.Directory;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

/**
 * 瞬时化管理器
 * 
 * @author Birdy
 *
 */
class TransienceManager implements LuceneManager, Closeable {

    /** Lucene配置 */
    private IndexWriterConfig config;

    /** Lucene目录 */
    private Directory directory;

    /** Lucene读出器 */
    private DirectoryReader reader;

    /** Lucene写入器 */
    private IndexWriter writer;

    /** 是否变更 */
    private AtomicBoolean changed = new AtomicBoolean(false);

    /** 创建标识 */
    private Set<String> createdIds;

    /** 更新标识 */
    private Object2LongMap<String> updatedIds;

    /** 删除标识 */
    private Set<String> deletedIds;

    public TransienceManager(IndexWriterConfig config, Directory directory) throws Exception {
        this.createdIds = new HashSet<>();
        this.updatedIds = new Object2LongOpenHashMap<>();
        this.deletedIds = new HashSet<>();

        this.config = config;
        this.directory = directory;
        this.writer = new IndexWriter(this.directory, this.config);
        this.reader = DirectoryReader.open(this.writer);
    }

    Set<String> getCreatedIds() {
        return createdIds;
    }

    Object2LongMap<String> getUpdatedIds() {
        return updatedIds;
    }

    Set<String> getDeletedIds() {
        return deletedIds;
    }

    void createDocument(String id, Document document) throws Exception {
        IndexableField field = null;
        field = new StringField(ID, id, Store.YES);
        document.add(field);
        long version = System.currentTimeMillis();
        field = new NumericDocValuesField(VERSION, version);
        document.add(field);
        if (this.deletedIds.remove(id)) {
            this.updatedIds.put(id, version);
        } else {
            this.createdIds.add(id);
        }
        this.writer.addDocument(document);
    }

    void updateDocument(String id, Document document) throws Exception {
        IndexableField field = null;
        field = new StringField(ID, id, Store.YES);
        document.add(field);
        long version = System.currentTimeMillis();
        field = new NumericDocValuesField(VERSION, version);
        document.add(field);
        if (this.createdIds.contains(id)) {
            Term term = new Term(ID, id);
            this.writer.updateDocument(term, document);
        } else {
            this.updatedIds.put(id, version);
            this.writer.addDocument(document);
        }
    }

    void deleteDocument(String id) throws Exception {
        if (this.createdIds.remove(id)) {
            Term term = new Term(ID, id);
            this.writer.deleteDocuments(term);
        } else {
            this.deletedIds.add(id);
        }
    }

    @Override
    public void close() {
        try {
            this.reader.close();
            this.writer.close();
        } catch (Exception exception) {
            // TODO 需要抛异常
        }
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
                long updated = TransienceManager.this.updatedIds.get(id);
                if (updated != 0) {
                    versions.advanceExact(index);
                    long version = versions.longValue();
                    if (updated > version) {
                        return;
                    }
                }
                collector.collect(index);
            }

        };
    }

    @Override
    public Directory getDirectory() {
        return directory;
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
