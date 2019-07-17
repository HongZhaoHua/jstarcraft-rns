package com.jstarcraft.rns.search;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.document.BinaryDocValuesField;
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
import org.apache.lucene.util.BytesRef;

import com.jstarcraft.core.common.lockable.HashLockable;
import com.jstarcraft.rns.search.exception.SearchException;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

/**
 * 瞬时化管理器
 * 
 * @author Birdy
 *
 */
class TransienceManager implements LuceneManager, AutoCloseable {

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

    private static final int size = 1000;

    private HashLockable[] lockables;

    public TransienceManager(IndexWriterConfig config, Directory directory) {
        try {
            this.createdIds = new HashSet<>();
            this.updatedIds = new Object2LongOpenHashMap<>();
            this.deletedIds = new HashSet<>();

            this.config = config;
            this.directory = directory;
            this.writer = new IndexWriter(this.directory, this.config);
            this.reader = DirectoryReader.open(this.writer);

            this.lockables = new HashLockable[size];
            for (int index = 0; index < size; index++) {
                this.lockables[index] = new HashLockable();
            }
        } catch (Exception exception) {
            throw new SearchException(exception);
        }
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

    void createDocument(String id, Document document) {
        try {
            IndexableField field = null;
            field = new StringField(ID, id, Store.YES);
            document.add(field);
            field = new BinaryDocValuesField(ID, new BytesRef(id));
            document.add(field);
            long version = System.currentTimeMillis();
            field = new NumericDocValuesField(VERSION, version);
            document.add(field);
            HashLockable lockable = lockables[Math.abs(id.hashCode() % size)];
            lockable.open();
            if (this.deletedIds.remove(id)) {
                this.updatedIds.put(id, version);
            } else {
                this.createdIds.add(id);
            }
            this.writer.addDocument(document);
            lockable.close();
            changed.set(true);
        } catch (Exception exception) {
            throw new SearchException(exception);
        }
    }

    void updateDocument(String id, Document document) {
        try {
            IndexableField field = null;
            field = new StringField(ID, id, Store.YES);
            document.add(field);
            field = new BinaryDocValuesField(ID, new BytesRef(id));
            document.add(field);
            long version = System.currentTimeMillis();
            field = new NumericDocValuesField(VERSION, version);
            document.add(field);
            HashLockable lockable = lockables[Math.abs(id.hashCode() % size)];
            lockable.open();
            if (!this.createdIds.contains(id)) {
                this.updatedIds.put(id, version);
            }
            Term term = new Term(ID, id);
            this.writer.updateDocument(term, document);
            lockable.close();
            changed.set(true);
        } catch (Exception exception) {
            throw new SearchException(exception);
        }
    }

    void deleteDocument(String id) {
        try {
            HashLockable lockable = lockables[Math.abs(id.hashCode() % size)];
            lockable.open();
            if (this.createdIds.remove(id)) {
                Term term = new Term(ID, id);
                this.writer.deleteDocuments(term);
            } else {
                this.deletedIds.add(id);
            }
            lockable.close();
            changed.set(true);
        } catch (Exception exception) {
            throw new SearchException(exception);
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
                long updated = TransienceManager.this.updatedIds.getLong(id);
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
    public IndexReader getReader() {
        try {
            if (changed.compareAndSet(true, false)) {
                this.writer.flush();
                DirectoryReader reader = DirectoryReader.openIfChanged(this.reader);
                if (reader != null) {
                    this.reader.close();
                    this.reader = reader;
                }
            }
            return this.reader;
        } catch (Exception exception) {
            throw new SearchException(exception);
        }
    }

    @Override
    public IndexWriter getWriter() {
        return writer;
    }

    @Override
    public void close() {
        try {
            // 只关闭writer,不关闭reader.
            this.writer.close();
        } catch (Exception exception) {
            throw new SearchException(exception);
        }
    }

}
