package com.jstarcraft.rns.search;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.store.Directory;

/**
 * Lucene管理器
 * 
 * <pre>
 * 用于管理{@link LeafCollector},{@link IndexReader},{@link IndexWriter}的变更与获取
 * </pre>
 * 
 * @author Birdy
 *
 */
public interface LuceneManager {

    static final String ID = "_id";

    static final String VERSION = "_version";

    default String getId(Document document) {
        return document.get(ID);
    }

    /**
     * 是否变更
     * 
     * @return
     */
    boolean isChanged();

    /**
     * 获取采集器
     * 
     * @param context
     * @param collector
     * @return
     * @throws IOException
     */
    LeafCollector getCollector(LeafReaderContext context, LeafCollector collector) throws IOException;

    /**
     * 获取目录
     * 
     * @return
     */
    Directory getDirectory();

    /**
     * 获取读取器
     * 
     * @return
     */
    IndexReader getReader();

    /**
     * 获取写入器
     * 
     * @return
     */
    IndexWriter getWriter();

}
