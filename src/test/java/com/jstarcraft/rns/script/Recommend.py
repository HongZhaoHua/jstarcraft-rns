configuration = Configurator(keyValues)
if (type == 'ranking'):
    recommender = RandomGuessRecommender()
    job = RankingTask(recommender, configuration)
    _data = job.execute()
if (type == 'rating'):
    recommender = RandomGuessRecommender()
    job = RatingTask(recommender, configuration)
    _data = job.execute()