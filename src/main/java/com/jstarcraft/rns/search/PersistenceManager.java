package com.jstarcraft.rns.search;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorable;
import org.apache.lucene.store.Directory;

import com.jstarcraft.rns.search.exception.SearchException;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

/**
 * 持久化管理器
 * 
 * @author Birdy
 *
 */
class PersistenceManager implements LuceneManager, AutoCloseable {

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

    /** 瞬时化管理器 */
    private TransienceManager transienceManager;

    public PersistenceManager(IndexWriterConfig config, Directory directory) {
        try {
            this.config = config;
            this.directory = directory;
            this.writer = new IndexWriter(this.directory, this.config);
            this.reader = DirectoryReader.open(this.writer);
        } catch (Exception exception) {
            throw new SearchException(exception);
        }
    }

    /**
     * 设置管理器
     * 
     * @param transienceManager
     */
    void setManager(TransienceManager transienceManager) {
        this.transienceManager = transienceManager;
        this.changed.set(true);
    }

    /**
     * 合并管理器
     * 
     * @param transienceManager
     * @throws Exception
     */
    void mergeManager() {
        try {
            Term[] terms = new Term[this.transienceManager.getUpdatedIds().size() + this.transienceManager.getDeletedIds().size()];
            int index = 0;
            for (String id : this.transienceManager.getUpdatedIds().keySet()) {
                terms[index++] = new Term(ID, id);
            }
            for (String id : this.transienceManager.getDeletedIds()) {
                terms[index++] = new Term(ID, id);
            }
            this.writer.deleteDocuments(terms);
            this.writer.addIndexes(this.transienceManager.getDirectory());
        } catch (Exception exception) {
            throw new SearchException(exception);
        }
    }

    @Override
    public boolean isChanged() {
        return this.changed.get();
    }

    @Override
    public LeafCollector getCollector(LeafReaderContext context, LeafCollector collector) throws IOException {
        if (this.transienceManager == null) {
            return collector;
        }

        LeafReader reader = context.reader();
        BinaryDocValues ids = DocValues.getBinary(reader, TransienceManager.ID);
        NumericDocValues versions = DocValues.getNumeric(reader, TransienceManager.VERSION);

        Object2LongMap<String> updatedIds = this.transienceManager.getUpdatedIds();
        Set<String> deletedIds = this.transienceManager.getDeletedIds();

        return new LeafCollector() {

            @Override
            public void setScorer(Scorable scorer) throws IOException {
                collector.setScorer(scorer);
            }

            @Override
            public void collect(int index) throws IOException {
                ids.advanceExact(index);
                String id = ids.binaryValue().utf8ToString();
                if (deletedIds.contains(id)) {
                    return;
                }
                long updated = updatedIds.getLong(id);
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
            if (this.changed.compareAndSet(true, false)) {
                if (this.transienceManager != null) {
                    IndexReader reader = DirectoryReader.open(this.transienceManager.getDirectory());
                    reader = new MultiReader(reader, this.reader);
                    return reader;
                }
            }
            DirectoryReader reader = DirectoryReader.openIfChanged(this.reader);
            if (reader != null) {
                this.reader.close();
                this.reader = reader;
            }
            return this.reader;
        } catch (Exception exception) {
            throw new SearchException(exception);
        }
    }

    @Override
    public IndexWriter getWriter() {
        return this.writer;
    }

    @Override
    public void close() {
        try {
            this.reader.close();
            this.writer.close();
        } catch (Exception exception) {
            throw new SearchException(exception);
        }
    }

}
