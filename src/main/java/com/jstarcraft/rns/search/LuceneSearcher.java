package com.jstarcraft.rns.search;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.CollectionTerminatedException;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.Weight;

/**
 * Lucene搜索器
 * 
 * <pre>
 * 支持按照createdIds,updatedIds与deletedIds过滤文档
 * </pre>
 * 
 * @author Birdy
 *
 */
public class LuceneSearcher extends IndexSearcher {

    private LuceneManager[] luceneManagers;

    public LuceneSearcher(TransienceManager transienceManager, PersistenceManager persistenceManager) throws Exception {
        super(new MultiReader(transienceManager.getReader(), persistenceManager.getReader()));
        this.luceneManagers = new LuceneManager[] { persistenceManager, transienceManager };
    }

    @Override
    protected void search(List<LeafReaderContext> leaves, Weight weight, Collector collector) throws IOException {
        for (LeafReaderContext context : leaves) {
            LeafCollector instance;
            try {
                // 此处刻意通过LuceneManager重载LeafCollector.
                instance = collector.getLeafCollector(context);
                for (LuceneManager luceneManager : luceneManagers) {
                    instance = luceneManager.getCollector(context, instance);
                }
            } catch (CollectionTerminatedException exception) {
                continue;
            }
            BulkScorer scorer = weight.bulkScorer(context);
            if (scorer != null) {
                try {
                    scorer.score(instance, context.reader().getLiveDocs());
                } catch (CollectionTerminatedException exception) {
                }
            }
        }
    }

    @Override
    public int count(Query query) throws IOException {
        query = rewrite(query);
        while (true) {
            if (query instanceof ConstantScoreQuery) {
                query = ((ConstantScoreQuery) query).getQuery();
            } else {
                break;
            }
        }

        final CollectorManager<TotalHitCountCollector, Integer> collectorManager = new CollectorManager<TotalHitCountCollector, Integer>() {

            @Override
            public TotalHitCountCollector newCollector() throws IOException {
                return new TotalHitCountCollector();
            }

            @Override
            public Integer reduce(Collection<TotalHitCountCollector> collectors) throws IOException {
                int count = 0;
                for (TotalHitCountCollector collector : collectors) {
                    count += collector.getTotalHits();
                }
                return count;
            }

        };
        return search(query, collectorManager);
    }

}
