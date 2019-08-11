# 构建配置
keyValues = Properties()
keyValues.load(loader.getResourceAsStream("data.properties"))
keyValues.load(loader.getResourceAsStream("recommend/benchmark/randomguess-test.properties"))
configurator = Configurator([keyValues])

# 构建模型
model = RandomGuessModel()

# 根据类型构建排序任务/评分任务
if (type == "ranking"):
    job = RankingTask(model, configurator)
if (type == "rating"):
    job = RatingTask(model, configurator)

# 获取评估指标
_data = job.execute()