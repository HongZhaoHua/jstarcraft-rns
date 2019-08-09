import com.jstarcraft.core.utility.Configurator
import com.jstarcraft.rns.task.RankingTask
import com.jstarcraft.rns.task.RatingTask
import com.jstarcraft.rns.recommend.benchmark.RandomGuessRecommender

Configurator = com.jstarcraft.core.utility.Configurator
RankingTask = com.jstarcraft.rns.task.RankingTask
RatingTask = com.jstarcraft.rns.task.RatingTask
RandomGuessRecommender = com.jstarcraft.rns.recommend.benchmark.RandomGuessRecommender

configuration = Configurator(keyValues)
if (type == 'ranking'):
    recommender = RandomGuessRecommender()
    job = RankingTask(recommender, configuration)
    _data = job.execute()
if (type == 'rating'):
    recommender = RandomGuessRecommender()
    job = RatingTask(recommender, configuration)
    _data = job.execute()