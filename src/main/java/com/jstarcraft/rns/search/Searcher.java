package com.jstarcraft.rns.search;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.exception.SearchException;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;

/**
 * 搜索器
 * 
 * @author Birdy
 *
 */
public class Searcher implements AutoCloseable {

    /** 配置 */
    private IndexWriterConfig config;

    /** 瞬时化管理器 */
    private volatile TransienceManager transienceManager;

    /** 持久化管理器 */
    private volatile PersistenceManager persistenceManager;

    /** Lucene搜索器 */
    private volatile LuceneSearcher searcher;

    /** 信号量 */
    private AtomicInteger semaphore;

    public Searcher(IndexWriterConfig config, Path path) {
        try {
            this.config = config;
            Directory transienceDirectory = new ByteBuffersDirectory();
            this.transienceManager = new TransienceManager((IndexWriterConfig) BeanUtils.cloneBean(config), transienceDirectory);
            Directory persistenceDirectory = FSDirectory.open(path);
            this.persistenceManager = new PersistenceManager((IndexWriterConfig) BeanUtils.cloneBean(config), persistenceDirectory);
            this.searcher = new LuceneSearcher(this.transienceManager, this.persistenceManager);
            this.semaphore = new AtomicInteger();
        } catch (Exception exception) {
            throw new SearchException(exception);
        }
    }

    /**
     * 加锁读操作
     */
    private void lockRead() {
        while (true) {
            int semaphore = this.semaphore.get();
            if (semaphore >= 0) {
                if (this.semaphore.compareAndSet(semaphore, semaphore + 1)) {
                    break;
                }
            }
        }
    }

    /**
     * 解锁读操作
     */
    private void unlockRead() {
        this.semaphore.decrementAndGet();
    }

    /**
     * 加锁写操作
     */
    private void lockWrite() {
        while (true) {
            int semaphore = this.semaphore.get();
            if (semaphore <= 0) {
                if (this.semaphore.compareAndSet(semaphore, semaphore - 1)) {
                    break;
                }
            }
        }
    }

    /**
     * 解锁写操作
     */
    private void unlockWrite() {
        this.semaphore.incrementAndGet();
    }

    /**
     * 合并管理器
     * 
     * @throws Exception
     */
    void mergeManager() throws Exception {
        TransienceManager newTransienceManager = new TransienceManager((IndexWriterConfig) BeanUtils.cloneBean(config), new ByteBuffersDirectory());
        TransienceManager oldTransienceManager = this.transienceManager;

        try {
            lockWrite();
            this.transienceManager = newTransienceManager;
            this.persistenceManager.setManager(oldTransienceManager);
        } finally {
            unlockWrite();
        }

        // 此处需要考虑防止有线程在使用时关闭.
        try {
            lockRead();
            oldTransienceManager.close();
        } finally {
            unlockRead();
        }

        this.persistenceManager.mergeManager();

        try {
            lockWrite();
            this.persistenceManager.setManager(null);
        } finally {
            unlockWrite();
        }
    }

    /**
     * 创建文档
     * 
     * @param documents
     * @throws Exception
     */
    public void createDocument(String id, Document document) {
        try {
            lockWrite();
            this.transienceManager.createDocument(id, document);
        } catch (Exception exception) {
            throw new SearchException(exception);
        } finally {
            unlockWrite();
        }
    }

    /**
     * 变更文档
     * 
     * @param documents
     * @throws Exception
     */
    public void updateDocument(String id, Document document) {
        try {
            lockWrite();
            this.transienceManager.updateDocument(id, document);
        } catch (Exception exception) {
            throw new SearchException(exception);
        } finally {
            unlockWrite();
        }
    }

    /**
     * 删除文档
     * 
     * @param ids
     * @throws Exception
     */
    public void deleteDocument(String id) {
        try {
            lockWrite();
            this.transienceManager.deleteDocument(id);
        } catch (Exception exception) {
            throw new SearchException(exception);
        } finally {
            unlockWrite();
        }
    }

    /**
     * 检索文档
     * 
     * @param query
     * @param sort
     * @param size
     * @return
     * @throws Exception
     */
    public KeyValue<List<Document>, FloatList> retrieveDocuments(Query query, Sort sort, int size) {
        try {
            lockRead();
            synchronized (this.semaphore) {
                if (this.transienceManager.isChanged() || this.persistenceManager.isChanged()) {
                    this.searcher = new LuceneSearcher(this.transienceManager, this.persistenceManager);
                }
            }
            ScoreDoc[] search = null;
            if (sort == null) {
                search = this.searcher.search(query, size).scoreDocs;
            } else {
                search = this.searcher.search(query, size, sort).scoreDocs;
            }
            ArrayList<Document> documents = new ArrayList<>(search.length);
            FloatList scores = new FloatArrayList(search.length);
            for (ScoreDoc score : search) {
                Document document = this.searcher.doc(score.doc);
                documents.add(document);
                scores.add(score.score);
            }
            return new KeyValue<>(documents, scores);
        } catch (Exception exception) {
            throw new SearchException(exception);
        } finally {
            unlockRead();
        }
    }

    /**
     * 统计文档
     * 
     * @param query
     * @return
     * @throws Exception
     */
    public int countDocuments(Query query) {
        try {
            lockRead();
            synchronized (this.semaphore) {
                if (this.transienceManager.isChanged() || this.persistenceManager.isChanged()) {
                    this.searcher = new LuceneSearcher(this.transienceManager, this.persistenceManager);
                }
            }
            return this.searcher.count(query);
        } catch (Exception exception) {
            throw new SearchException(exception);
        } finally {
            unlockRead();
        }
    }

    @Override
    public void close() {
        try {
            mergeManager();
            this.transienceManager.close();
            this.persistenceManager.close();
        } catch (Exception exception) {
            throw new SearchException(exception);
        }
    }

}
