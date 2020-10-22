// 构建配置
var keyValues = Properties();
var loader = bindings["loader"] as ClassLoader;
keyValues.load(loader.getResourceAsStream("data.properties"));
keyValues.load(loader.getResourceAsStream("model/benchmark/randomguess-test.properties"));
var option = Option(keyValues);

// 此对象会返回给Java程序
var _data = mutableMapOf<String, Float>();

// 构建排序任务
var rankingTask = RankingTask(RandomGuessModel::class.java, option);
// 训练与评估模型并获取排序指标
val rankingMeasures = rankingTask.execute();
_data["precision"] = rankingMeasures.getFloat(PrecisionEvaluator::class.java);
_data["recall"] = rankingMeasures.getFloat(RecallEvaluator::class.java);

// 构建评分任务
var ratingTask = RatingTask(RandomGuessModel::class.java, option);
// 训练与评估模型并获取评分指标
var ratingMeasures = ratingTask.execute();
_data["mae"] = ratingMeasures.getFloat(MAEEvaluator::class.java);
_data["mse"] = ratingMeasures.getFloat(MSEEvaluator::class.java);

_data;