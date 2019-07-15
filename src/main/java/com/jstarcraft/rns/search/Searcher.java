package com.jstarcraft.rns.search;

import java.nio.file.Path;

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

    /** 瞬时化管理器 */
    private TransienceManager transienceManager;

    /** 持久化管理器 */
    private PersistenceManager persistenceManager;

    /** Lucene搜索器 */
    private LuceneSearcher searcher;

    public Searcher(IndexWriterConfig config, Path path) throws Exception {
        Directory transienceDirectory = new ByteBuffersDirectory();
        this.transienceManager = new TransienceManager(config, transienceDirectory);
        Directory persistenceDirectory = FSDirectory.open(path);
        this.persistenceManager = new PersistenceManager(config, persistenceDirectory);
        this.searcher = new LuceneSearcher(this.transienceManager, this.persistenceManager);
    }

    /**
     * 创建文档
     * 
     * @param documents
     * @throws Exception
     */
    public void createDocuments(String id, Document document) throws Exception {
        this.transienceManager.createDocument(id, document);
    }

    /**
     * 变更文档
     * 
     * @param documents
     * @throws Exception
     */
    public void updateDocuments(String id, Document document) throws Exception {
        this.transienceManager.updateDocument(id, document);
    }

    /**
     * 删除文档
     * 
     * @param ids
     * @throws Exception
     */
    public void deleteDocuments(String id) throws Exception {
        this.transienceManager.deleteDocument(id);
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
        if (this.transienceManager.isChanged() || this.persistenceManager.isChanged()) {
            this.searcher = new LuceneSearcher(this.transienceManager, this.persistenceManager);
        }
        return this.searcher.search(query, size, sort);
    }

    /**
     * 统计文档
     * 
     * @param query
     * @return
     * @throws Exception
     */
    public int countDocuments(Query query) throws Exception {
        if (this.transienceManager.isChanged() || this.persistenceManager.isChanged()) {
            this.searcher = new LuceneSearcher(this.transienceManager, this.persistenceManager);
        }
        return this.searcher.count(query);
    }

}
