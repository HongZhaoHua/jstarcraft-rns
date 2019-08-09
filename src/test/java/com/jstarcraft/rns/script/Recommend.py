# 构建配置器
keyValues = Properties()
keyValues.load(loader.getResourceAsStream("data.properties"))
keyValues.load(loader.getResourceAsStream("recommend/benchmark/randomguess-test.properties"))
configurator = Configurator([keyValues])

# 构建推荐器
recommender = RandomGuessRecommender()

# 根据类型构建排序任务/评分任务
if (type == "ranking"):
    job = RankingTask(recommender, configurator)
if (type == "rating"):
    job = RatingTask(recommender, configurator)

# 获取评估指标
_data = job.execute()