# 构建配置
keyValues = Properties()
keyValues.load(loader.getResourceAsStream("data.properties"))
keyValues.load(loader.getResourceAsStream("model/benchmark/randomguess-test.properties"))
configurator = Configurator([keyValues])

# 此对象会返回给Java程序
_data = {}

# 构建排序任务
task = RankingTask(RandomGuessModel, configurator)
# 训练与评估模型并获取排序指标
measures = task.execute()
_data['precision'] = measures.get(PrecisionEvaluator)
_data['recall'] = measures.get(RecallEvaluator)

# 构建评分任务
task = RatingTask(RandomGuessModel, configurator)
# 训练与评估模型并获取评分指标
measures = task.execute()
_data['mae'] = measures.get(MAEEvaluator)
_data['mse'] = measures.get(MSEEvaluator)