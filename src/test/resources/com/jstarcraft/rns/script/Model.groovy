// 构建配置
def keyValues = new Properties();
keyValues.load(loader.getResourceAsStream("data.properties"));
keyValues.load(loader.getResourceAsStream("model/benchmark/randomguess-test.properties"));
def option = new Option(keyValues);

// 此对象会返回给Java程序
def _data = [:];

// 构建排序任务
task = new RankingTask(RandomGuessModel.class, option);
// 训练与评估模型并获取排序指标
measures = task.execute();
_data.precision = measures.get(PrecisionEvaluator.class);
_data.recall = measures.get(RecallEvaluator.class);

// 构建评分任务
task = new RatingTask(RandomGuessModel.class, option);
// 训练与评估模型并获取评分指标
measures = task.execute();
_data.mae = measures.get(MAEEvaluator.class);
_data.mse = measures.get(MSEEvaluator.class);

_data;