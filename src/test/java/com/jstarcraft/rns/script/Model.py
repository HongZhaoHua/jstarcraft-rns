# 构建配置
keyValues = Properties()
keyValues.load(loader.getResourceAsStream("data.properties"))
keyValues.load(loader.getResourceAsStream("recommend/benchmark/randomguess-test.properties"))
configurator = Configurator([keyValues])

# 根据类型构建排序任务/评分任务
if (type == "ranking"):
    job = RankingTask(RandomGuessModel, configurator)
if (type == "rating"):
    job = RatingTask(RandomGuessModel, configurator)

# 训练与评估模型并获取指标
_data = job.execute()