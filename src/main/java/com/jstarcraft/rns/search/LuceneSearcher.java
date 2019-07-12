package com.jstarcraft.rns.search;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.CollectionTerminatedException;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Weight;

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

}
