package com.jstarcraft.rns.search;

import java.nio.file.Path;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;

/**
 * 缓存搜索器
 * 
 * @author Birdy
 *
 * @param <I>
 * @param <T>
 */
public class Searcher {

    private TransienceManager transienceManager;

    private PersistenceManager persistenceManager;

    private LuceneSearcher searcher;

    public Searcher(IndexWriterConfig config, Path path) throws Exception {
        this.transienceManager = new TransienceManager(config);
        this.persistenceManager = new PersistenceManager(config, path);
        this.searcher = new LuceneSearcher(this.transienceManager, this.persistenceManager);
    }

    /**
     * 创建文档
     * 
     * @param documents
     * @throws Exception
     */
    public void createDocuments(Document... documents) throws Exception {
        this.transienceManager.createDocuments(documents);
    }

    /**
     * 变更文档
     * 
     * @param documents
     * @throws Exception
     */
    public void updateDocuments(Document... documents) throws Exception {
        this.transienceManager.updateDocuments(documents);
    }

    /**
     * 删除文档
     * 
     * @param ids
     * @throws Exception
     */
    public void deleteDocuments(String... ids) throws Exception {
        this.transienceManager.deleteDocuments(ids);
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
