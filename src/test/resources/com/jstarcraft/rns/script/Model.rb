# 构建配置
keyValues = Properties.new()
keyValues.load($loader.getResourceAsStream("data.properties"))
keyValues.load($loader.getResourceAsStream("model/benchmark/randomguess-test.properties"))
configurator = Configurator.new(keyValues)

# 此对象会返回给Java程序
_data = Hash.new()

# 构建排序任务
task = RankingTask.new(RandomGuessModel.java_class, configurator)
# 训练与评估模型并获取排序指标
measures = task.execute()
_data['precision'] = measures.get(PrecisionEvaluator.java_class)
_data['recall'] = measures.get(RecallEvaluator.java_class)

# 构建评分任务
task = RatingTask.new(RandomGuessModel.java_class, configurator)
# 训练与评估模型并获取评分指标
measures = task.execute()
_data['mae'] = measures.get(MAEEvaluator.java_class)
_data['mse'] = measures.get(MSEEvaluator.java_class)

_data;