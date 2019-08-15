# JStarCraft RNS

****

[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Total lines](https://tokei.rs/b1/github/HongZhaoHua/jstarcraft-rns?category=lines)](https://tokei.rs/b1/github/HongZhaoHua/jstarcraft-rns?category=lines)

希望路过的同学,顺手给JStarCraft框架点个Star,算是对作者的一种鼓励吧!

****

## 目录

* [介绍](#介绍)
* [特性](#特性)
* [安装](#安装)
    * [安装JStarCraft Core框架](#安装JStarCraft-Core框架)
    * [安装JStarCraft AI框架](#安装JStarCraft-AI框架)
    * [安装JStarCraft RNS引擎](#安装JStarCraft-RNS引擎)
* [使用](#使用)
    * [设置依赖](#设置依赖)
    * [构建配置器](#构建配置器)
    * [训练与评估模型](#训练与评估模型)
    * [获取模型](#获取模型)
* [架构](#架构)
* [概念](#概念)
    * [为什么需要信息检索](#为什么需要信息检索)
    * [搜索与推荐的异同](#搜索与推荐的异同)
    * [JStarCraft RNS引擎解决什么问题](#JStarCraft-RNS引擎解决什么问题)
    * [Ranking任务与Rating任务之间的区别](#Ranking任务与Rating任务之间的区别)
    * [Rating算法能不能用于Ranking问题](#Rating算法能不能用于Ranking问题)
* [示例](#示例)
    * [JStarCraft RNS引擎与Groovy脚本交互](#JStarCraft-RNS引擎与Groovy脚本交互)
    * [JStarCraft RNS引擎与JS脚本交互](#JStarCraft-RNS引擎与JS脚本交互)
    * [JStarCraft RNS引擎与Lua脚本交互](#JStarCraft-RNS引擎与Lua脚本交互)
    * [JStarCraft RNS引擎与Python脚本交互](#JStarCraft-RNS引擎与Python脚本交互)
* [对比](#对比)
* [版本](#版本)
* [参考](#参考)
    * [个性化算法](#个性化算法)
    * [数据集](#数据集)
* [协议](#协议)
* [作者](#作者)
* [致谢](#致谢)

****

## 介绍

**JStarCraft RNS是一个面向信息检索领域的轻量级引擎.遵循Apache 2.0协议.**

专注于解决信息检索领域的基本问题:推荐与搜索.

提供满足工业级别场景要求的推荐引擎设计与实现.

提供满足工业级别场景要求的搜索引擎设计与实现.

****

## 特性

* 1.跨平台
* [2.串行与并行计算](https://github.com/HongZhaoHua/jstarcraft-ai)
* [3.CPU与GPU硬件加速](https://github.com/HongZhaoHua/jstarcraft-ai)
* [4.模型保存与装载](https://github.com/HongZhaoHua/jstarcraft-ai)
* 5.丰富的推荐与搜索算法
* 7.丰富的脚本支持
    * Groovy
    * JS
    * Lua
    * MVEL
    * Python
* [8.丰富的评估指标](#评估指标)
    * [排序指标](#排序指标)
    * [评分指标](#评分指标)

****

## 安装

JStarCraft RNS要求使用者具备以下环境:
* JDK 8或者以上
* Maven 3

#### 安装JStarCraft-Core框架

```shell
git clone https://github.com/HongZhaoHua/jstarcraft-core.git

mvn install -Dmaven.test.skip=true
```

#### 安装JStarCraft-AI框架

```shell
git clone https://github.com/HongZhaoHua/jstarcraft-ai.git

mvn install -Dmaven.test.skip=true
```

####  安装JStarCraft-RNS引擎

```shell
git clone https://github.com/HongZhaoHua/jstarcraft-rns.git

mvn install -Dmaven.test.skip=true
```

****

## 使用

#### 设置依赖

* 设置Maven依赖

```maven
<dependency>
    <groupId>com.jstarcraft</groupId>
    <artifactId>rns</artifactId>
    <version>1.0</version>
</dependency>
```

* 设置Gradle依赖

```gradle
compile group: 'com.jstarcraft', name: 'rns', version: '1.0'
```

#### 构建配置器

```java
Properties keyValues = new Properties();
keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
keyValues.load(this.getClass().getResourceAsStream("/recommend/benchmark/randomguess-test.properties"));
Configurator configurator = new Configurator(keyValues);
```

#### 训练与评估模型

* 构建排序任务

```java
RankingTask task = new RankingTask(RandomGuessModel.class, configurator);
// 训练与评估排序模型
task.execute();
```

* 构建评分任务

```java
RatingTask task = new RatingTask(RandomGuessModel.class, configurator);
// 训练与评估评分模型
task.execute();
```

#### 获取模型

```java
// 获取模型
Model model = task.getModel();
```

****

## 架构

****

## 概念

#### 为什么需要信息检索

```
随着信息技术和互联网的发展,人们逐渐从信息匮乏(Information Underload)的时代走入了信息过载(Information Overload)的时代.

无论是信息消费者还是信息生产者都遇到了挑战:
* 对于信息消费者,从海量信息中寻找信息,是一件非常困难的事情;
* 对于信息生产者,从海量信息中暴露信息,也是一件非常困难的事情;

信息检索的任务就是联系用户和信息,一方面帮助用户寻找对自己有价值的信息,另一方面帮助信息暴露给对它感兴趣的用户,从而实现信息消费者和信息生产者的双赢.
```

#### 搜索与推荐的异同

```
从信息检索的角度:
* 搜索和推荐是获取信息的两种主要手段;
* 搜索和推荐是获取信息的两种不同方式;
    * 搜索(Search)是主动明确的;
    * 推荐(Recommend)是被动模糊的;

搜索和推荐是两个互补的工具.
```

#### JStarCraft-RNS引擎解决什么问题

```
JStarCraft-RNS引擎旨在解决推荐与搜索领域的两个核心任务:排序预测(Ranking)和评分预测(Rating).
```

#### Ranking任务与Rating任务之间的区别

```
根据解决基本问题的不同,将算法与评估指标划分为排序(Ranking)与评分(Rating).

两者之间的根本区别在于目标函数的不同.
通俗点的解释:
Ranking算法基于隐式反馈数据,趋向于拟合用户的排序.(关注度)
Rating算法基于显示反馈数据,趋向于拟合用户的评分.(满意度)
```

#### Rating算法能不能用于Ranking问题

```
关键在于具体场景中,关注度与满意度是否保持一致.
通俗点的解释:
人们关注的东西,并不一定是满意的东西.(例如:个人所得税)
```

****

## 示例

#### JStarCraft-RNS引擎与Groovy脚本交互

* [完整示例](https://github.com/HongZhaoHua/jstarcraft-rns/tree/master/src/test/java/com/jstarcraft/rns/script)

* 编写Groovy脚本训练与评估模型并保存到Model.groovy文件

```groovy
// 构建配置
def keyValues = new Properties();
keyValues.load(loader.getResourceAsStream("data.properties"));
keyValues.load(loader.getResourceAsStream("recommend/benchmark/randomguess-test.properties"));
def configurator = new Configurator(keyValues);

// 此对象会返回给Java程序
def _data = [:];

// 构建排序任务
task = new RankingTask(RandomGuessModel.class, configurator);
// 训练与评估模型并获取排序指标
measures = task.execute();
_data.precision = measures.get(PrecisionEvaluator.class);
_data.recall = measures.get(RecallEvaluator.class);

// 构建评分任务
task = new RatingTask(RandomGuessModel.class, configurator);
// 训练与评估模型并获取评分指标
measures = task.execute();
_data.mae = measures.get(MAEEvaluator.class);
_data.mse = measures.get(MSEEvaluator.class);

_data;
```

* 使用JStarCraft框架从Model.groovy文件加载并执行Groovy脚本

```java
// 获取Groovy脚本
File file = new File(ScriptTestCase.class.getResource("Model.groovy").toURI());
String script = FileUtils.readFileToString(file, StringUtility.CHARSET);

// 设置Groovy脚本使用到的Java类
ScriptContext context = new ScriptContext();
context.useClasses(Properties.class, Configurator.class);
context.useClasses(RankingTask.class, RatingTask.class, RandomGuessModel.class);
context.useClasses(Assert.class, PrecisionEvaluator.class, RecallEvaluator.class, MAEEvaluator.class, MSEEvaluator.class);
// 设置Groovy脚本使用到的Java变量
ScriptScope scope = new ScriptScope();
scope.createAttribute("loader", loader);

// 执行Groovy脚本
ScriptExpression expression = new GroovyExpression(context, scope, script);
Map<String, Float> data = expression.doWith(Map.class);
```

#### JStarCraft-RNS引擎与JS脚本交互

* [完整示例](https://github.com/HongZhaoHua/jstarcraft-rns/tree/master/src/test/java/com/jstarcraft/rns/script)

* 编写JS脚本训练与评估模型并保存到Model.js文件

```js
// 构建配置
var keyValues = new Properties();
keyValues.load(loader.getResourceAsStream("data.properties"));
keyValues.load(loader.getResourceAsStream("recommend/benchmark/randomguess-test.properties"));
var configurator = new Configurator([keyValues]);

// 此对象会返回给Java程序
var _data = {};

// 构建排序任务
task = new RankingTask(RandomGuessModel.class, configurator);
// 训练与评估模型并获取排序指标
measures = task.execute();
_data['precision'] = measures.get(PrecisionEvaluator.class);
_data['recall'] = measures.get(RecallEvaluator.class);

// 构建评分任务
task = new RatingTask(RandomGuessModel.class, configurator);
// 训练与评估模型并获取评分指标
measures = task.execute();
_data['mae'] = measures.get(MAEEvaluator.class);
_data['mse'] = measures.get(MSEEvaluator.class);

_data;
```

* 使用JStarCraft框架从Model.js文件加载并执行JS脚本

```java
// 获取JS脚本
File file = new File(ScriptTestCase.class.getResource("Model.js").toURI());
String script = FileUtils.readFileToString(file, StringUtility.CHARSET);

// 设置JS脚本使用到的Java类
ScriptContext context = new ScriptContext();
context.useClasses(Properties.class, Configurator.class);
context.useClasses(RankingTask.class, RatingTask.class, RandomGuessModel.class);
context.useClasses(Assert.class, PrecisionEvaluator.class, RecallEvaluator.class, MAEEvaluator.class, MSEEvaluator.class);
// 设置JS脚本使用到的Java变量
ScriptScope scope = new ScriptScope();
scope.createAttribute("loader", loader);

// 执行JS脚本
ScriptExpression expression = new JsExpression(context, scope, script);
Map<String, Float> data = expression.doWith(Map.class);
```

#### JStarCraft-RNS引擎与Lua脚本交互

* [完整示例](https://github.com/HongZhaoHua/jstarcraft-rns/tree/master/src/test/java/com/jstarcraft/rns/script)

* 编写Lua脚本训练与评估模型并保存到Model.lua文件

```lua
-- 构建配置
local keyValues = Properties.new();
keyValues:load(loader:getResourceAsStream("data.properties"));

keyValues:load(loader:getResourceAsStream("recommend/benchmark/randomguess-test.properties"));
local configurator = Configurator.new({ keyValues });

-- 此对象会返回给Java程序
local _data = {};

-- 构建排序任务
task = RankingTask.new(RandomGuessModel, configurator);
-- 训练与评估模型并获取排序指标
measures = task:execute();
_data["precision"] = measures:get(PrecisionEvaluator);
_data["recall"] = measures:get(RecallEvaluator);

-- 构建评分任务
task = RatingTask.new(RandomGuessModel, configurator);
-- 训练与评估模型并获取评分指标
measures = task:execute();
_data["mae"] = measures:get(MAEEvaluator);
_data["mse"] = measures:get(MSEEvaluator);

return _data;
```

* 使用JStarCraft框架从Model.lua文件加载并执行Lua脚本

```java
// 获取Lua脚本
File file = new File(ScriptTestCase.class.getResource("Model.lua").toURI());
String script = FileUtils.readFileToString(file, StringUtility.CHARSET);

// 设置Lua脚本使用到的Java类
ScriptContext context = new ScriptContext();
context.useClasses(Properties.class, Configurator.class);
context.useClasses(RankingTask.class, RatingTask.class, RandomGuessModel.class);
context.useClasses(Assert.class, PrecisionEvaluator.class, RecallEvaluator.class, MAEEvaluator.class, MSEEvaluator.class);
// 设置Lua脚本使用到的Java变量
ScriptScope scope = new ScriptScope();
scope.createAttribute("loader", loader);

// 执行Lua脚本
ScriptExpression expression = new LuaExpression(context, scope, script);
LuaTable data = expression.doWith(LuaTable.class);
```

#### JStarCraft-RNS引擎与Python脚本交互

* [完整示例](https://github.com/HongZhaoHua/jstarcraft-rns/tree/master/src/test/java/com/jstarcraft/rns/script)

* 编写Python脚本训练与评估模型并保存到Model.py文件

```python
# 构建配置
keyValues = Properties()
keyValues.load(loader.getResourceAsStream("data.properties"))
keyValues.load(loader.getResourceAsStream("recommend/benchmark/randomguess-test.properties"))
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
```

* 使用JStarCraft框架从Model.py文件加载并执行Python脚本

```java
// 设置Python环境变量
System.setProperty("python.console.encoding", StringUtility.CHARSET.name());

// 获取Python脚本
File file = new File(PythonTestCase.class.getResource("Model.py").toURI());
String script = FileUtils.readFileToString(file, StringUtility.CHARSET);

// 设置Python脚本使用到的Java类
ScriptContext context = new ScriptContext();
context.useClasses(Properties.class, Configurator.class);
context.useClasses(RankingTask.class, RatingTask.class, RandomGuessModel.class);
context.useClasses(Assert.class, PrecisionEvaluator.class, RecallEvaluator.class, MAEEvaluator.class, MSEEvaluator.class);
// 设置Python脚本使用到的Java变量
ScriptScope scope = new ScriptScope();
scope.createAttribute("loader", loader);
        
// 执行Python脚本
ScriptExpression expression = new PythonExpression(context, scope, script);
Map<String, Double> data = expression.doWith(Map.class);
```

****

## 对比

****

## 版本

****

## 参考

#### 个性化算法

* 基准算法

| 名称 | 问题 | 说明/论文 |
| :----: | :----: | :----: |
| RandomGuess | Ranking Rating | 随机猜测 |
| MostPopular | Ranking| 最受欢迎 |
| ConstantGuess | Rating  | 常量猜测 |
| GlobalAverage | Rating  | 全局平均 |
| ItemAverage | Rating  | 物品平均 |
| ItemCluster | Rating  | 物品聚类 |
| UserAverage | Rating  | 用户平均 |
| UserCluster | Rating  | 用户聚类 |

* 协同算法

| 名称 | 问题 | 说明/论文 |
| :----: | :----: | :----: |
| AspectModel | Ranking Rating | Latent class models for collaborative filtering |
| BHFree | Ranking Rating  | Balancing Prediction and Recommendation Accuracy: Hierarchical Latent Factors for Preference Data |
| BUCM | Ranking Rating  | Modeling Item Selection and Relevance for Accurate Recommendations |
| ItemKNN | Ranking Rating  | 基于物品的协同过滤 |
| UserKNN | Ranking Rating  | 基于用户的协同过滤 |
| AoBPR | Ranking | Improving pairwise learning for item recommendation from implicit feedback |
| BPR | Ranking | BPR: Bayesian Personalized Ranking from Implicit Feedback |
| CLiMF | Ranking | CLiMF: learning to maximize reciprocal rank with collaborative less-is-more filtering |
| EALS | Ranking | Collaborative filtering for implicit feedback dataset |
| FISM | Ranking | FISM: Factored Item Similarity Models for Top-N Recommender Systems |
| GBPR | Ranking | GBPR: Group Preference Based Bayesian Personalized Ranking for One-Class Collaborative Filtering |
| HMMForCF | Ranking | A Hidden Markov Model Purpose: A class for the model, including parameters |
| ItemBigram | Ranking | Topic Modeling: Beyond Bag-of-Words |
| LambdaFM | Ranking | LambdaFM: Learning Optimal Ranking with Factorization Machines Using Lambda Surrogates |
| LDA | Ranking | Latent Dirichlet Allocation for implicit feedback |
| ListwiseMF | Ranking | List-wise learning to rank with matrix factorization for collaborative filtering |
| PLSA | Ranking | Latent semantic models for collaborative filtering |
| RankALS | Ranking | Alternating Least Squares for Personalized Ranking |
| RankSGD | Ranking | Collaborative Filtering Ensemble for Ranking |
| SLIM | Ranking | SLIM: Sparse Linear Methods for Top-N Recommender Systems |
| WBPR | Ranking | Bayesian Personalized Ranking for Non-Uniformly Sampled Items |
| WRMF | Ranking | Collaborative filtering for implicit feedback datasets |
| Rank-GeoFM | Ranking | Rank-GeoFM: A ranking based geographical factorization method for point of interest recommendation |
| SBPR | Ranking | Leveraging Social Connections to Improve Personalized Ranking for Collaborative Filtering |
| AssociationRule | Ranking | A Recommendation Algorithm Using Multi-Level Association Rules |
| PRankD | Ranking | Personalised ranking with diversity |
| AsymmetricSVD++ | Rating | Factorization Meets the Neighborhood: a Multifaceted Collaborative Filtering Model |
| AutoRec | Rating | AutoRec: Autoencoders Meet Collaborative Filtering |
| BPMF | Rating | Bayesian Probabilistic Matrix Factorization using Markov Chain Monte Carlo |
| CCD | Rating | Large-Scale Parallel Collaborative Filtering for the Netflix Prize |
| FFM | Rating | Field Aware Factorization Machines for CTR Prediction |
| GPLSA | Rating | Collaborative Filtering via Gaussian Probabilistic Latent Semantic Analysis |
| IRRG | Rating | Exploiting Implicit Item Relationships for Recommender Systems |
| MFALS | Rating | Large-Scale Parallel Collaborative Filtering for the Netflix Prize |
| NMF | Rating | Algorithms for Non-negative Matrix Factorization |
| PMF | Rating | PMF: Probabilistic Matrix Factorization |
| RBM | Rating | Restricted Boltzman Machines for Collaborative Filtering |
| RF-Rec | Rating | RF-Rec: Fast and Accurate Computation of Recommendations based on Rating Frequencies |
| SVD++ | Rating | Factorization Meets the Neighborhood: a Multifaceted Collaborative Filtering Model |
| URP | Rating | User Rating Profile: a LDA model for rating prediction |
| RSTE | Rating | Learning to Recommend with Social Trust Ensemble |
| SocialMF | Rating | A matrix factorization technique with trust propagation for recommendation in social networks |
| SoRec | Rating | SoRec: Social recommendation using probabilistic matrix factorization |
| SoReg | Rating | Recommender systems with social regularization |
| TimeSVD++ | Rating | Collaborative Filtering with Temporal Dynamics |
| TrustMF | Rating | Social Collaborative Filtering by Trust |
| TrustSVD | Rating | TrustSVD: Collaborative Filtering with Both the Explicit and Implicit Influence of User Trust and of Item Ratings |
| PersonalityDiagnosis | Rating | A brief introduction to Personality Diagnosis |
| SlopeOne | Rating | Slope One Predictors for Online Rating-Based Collaborative Filtering |

* 内容算法

| 名称 | 问题 | 说明/论文 |
| :----: | :----: | :----: |
| EFM | Ranking Rating  | Explicit factor models for explainable recommendation based on phrase-level sentiment analysis |
| TF-IDF | Ranking | 词频-逆文档频率 |
| HFT | Rating | Hidden factors and hidden topics: understanding rating dimensions with review text |
| TopicMF | Rating | TopicMF: Simultaneously Exploiting Ratings and Reviews for Recommendation |

#### 数据集

* [Amazon Product Dataset](http://jmcauley.ucsd.edu/data/amazon/)
* [Bibsonomy Dataset](https://www.kde.cs.uni-kassel.de/wp-content/uploads/bibsonomy/)
* [BookCrossing Dataset](https://grouplens.org/datasets/book-crossing/)
* [Ciao Dataset](https://www.cse.msu.edu/~tangjili/datasetcode/truststudy.htm)
* [Douban Dataset](http://smiles.xjtu.edu.cn/Download/Download_Douban.html)
* [Eachmovie Dataset](https://grouplens.org/datasets/eachmovie/)
* [Epinions Dataset](http://www.trustlet.org/epinions.html)
* [Foursquare Dataset](https://sites.google.com/site/yangdingqi/home/foursquare-dataset)
* [HetRec2011 Dataset](https://grouplens.org/datasets/hetrec-2011/)
* [Jest Joker Dataset](https://grouplens.org/datasets/jester/)
* [Large Movie Review Dataset](http://ai.stanford.edu/~amaas/data/sentiment/)
* [Movielens Dataset](https://grouplens.org/datasets/movielens/)
* [Serendipity 2018 Dataset](https://grouplens.org/datasets/serendipity-2018/)
* [Wikilens Dataset](https://grouplens.org/datasets/wikilens/)
* [Yelp Dataset](https://www.yelp.com/dataset)
* [Yongfeng Zhang Dataset](http://yongfeng.me/dataset/)

****

## 协议

JStarCraft RNS遵循[Apache 2.0协议](https://www.apache.org/licenses/LICENSE-2.0.html),一切以其为基础的衍生作品均属于衍生作品的作者.

****

## 作者

| 作者 | 洪钊桦 |
| :----: | :----: |
| E-mail | 110399057@qq.com, jstarcraft@gmail.com |

****

## 致谢

特别感谢[LibRec团队](https://github.com/guoguibing/librec)与**推荐系统QQ群**(274750470)在推荐方面提供的支持与帮助.

特别感谢[陆徐刚](https://github.com/luxugang/Lucene-7.5.0)在搜索方面提供的支持与帮助.

****

