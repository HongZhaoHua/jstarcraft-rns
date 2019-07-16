package com.jstarcraft.rns.search;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * 缓存搜索器
 * 
 * @author Birdy
 *
 * @param <I>
 * @param <T>
 */
public class Searcher {

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

    public Searcher(IndexWriterConfig config, Path path) throws Exception {
        this.config = config;
        Directory transienceDirectory = new ByteBuffersDirectory();
        this.transienceManager = new TransienceManager(config, transienceDirectory);
        Directory persistenceDirectory = FSDirectory.open(path);
        this.persistenceManager = new PersistenceManager(config, persistenceDirectory);
        this.searcher = new LuceneSearcher(this.transienceManager, this.persistenceManager);
    }

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

    private void unlockRead() {
        this.semaphore.decrementAndGet();
    }

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

    private void unlockWrite() {
        this.semaphore.incrementAndGet();
    }

    /**
     * 合并管理器
     * 
     * @throws Exception
     */
    void mergeManager() throws Exception {
        TransienceManager newTransienceManager = new TransienceManager(this.config, new ByteBuffersDirectory());
        TransienceManager oldTransienceManager = this.transienceManager;

        try {
            lockWrite();
            this.transienceManager = newTransienceManager;
            this.persistenceManager.setManager(oldTransienceManager);
        } finally {
            unlockWrite();
        }

        this.persistenceManager.mergeManager();

        try {
            lockWrite();
            this.persistenceManager.setManager(null);
        } finally {
            unlockWrite();
        }

        oldTransienceManager.close();
    }

    /**
     * 创建文档
     * 
     * @param documents
     * @throws Exception
     */
    public void createDocuments(String id, Document document) throws Exception {
        try {
            lockWrite();
            this.transienceManager.createDocument(id, document);
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
    public void updateDocuments(String id, Document document) throws Exception {
        try {
            lockWrite();
            this.transienceManager.updateDocument(id, document);
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
    public void deleteDocuments(String id) throws Exception {
        try {
            lockWrite();
            this.transienceManager.deleteDocument(id);
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
    public TopDocs retrieveDocuments(Query query, Sort sort, int size) throws Exception {
        try {
            lockRead();
            synchronized (this.semaphore) {
                if (this.transienceManager.isChanged() || this.persistenceManager.isChanged()) {
                    this.searcher = new LuceneSearcher(this.transienceManager, this.persistenceManager);
                }
            }
            return this.searcher.search(query, size, sort);
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
    public int countDocuments(Query query) throws Exception {
        try {
            lockRead();
            synchronized (this.semaphore) {
                if (this.transienceManager.isChanged() || this.persistenceManager.isChanged()) {
                    this.searcher = new LuceneSearcher(this.transienceManager, this.persistenceManager);
                }
            }
            return this.searcher.count(query);
        } finally {
            unlockRead();
        }
    }

}
