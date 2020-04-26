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
    * [JStarCraft RNS引擎与Ruby脚本交互](#JStarCraft-RNS引擎与Ruby脚本交互)
* [对比](#对比)
* [版本](#版本)
* [参考](#参考)
    * [个性化模型](#个性化模型)
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
* 6.丰富的脚本支持
    * Groovy
    * JS
    * Lua
    * MVEL
    * Python
    * Ruby
* [7.丰富的评估指标](#评估指标)
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
context.useClasses(Properties.class, Assert.class);
context.useClass("Configurator", MapConfigurator.class);
context.useClasses("com.jstarcraft.ai.evaluate");
context.useClasses("com.jstarcraft.rns.task");
context.useClasses("com.jstarcraft.rns.model.benchmark");
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
context.useClasses(Properties.class, Assert.class);
context.useClass("Configurator", MapConfigurator.class);
context.useClasses("com.jstarcraft.ai.evaluate");
context.useClasses("com.jstarcraft.rns.task");
context.useClasses("com.jstarcraft.rns.model.benchmark");
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
context.useClasses(Properties.class, Assert.class);
context.useClass("Configurator", MapConfigurator.class);
context.useClasses("com.jstarcraft.ai.evaluate");
context.useClasses("com.jstarcraft.rns.task");
context.useClasses("com.jstarcraft.rns.model.benchmark");
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
context.useClasses(Properties.class, Assert.class);
context.useClass("Configurator", MapConfigurator.class);
context.useClasses("com.jstarcraft.ai.evaluate");
context.useClasses("com.jstarcraft.rns.task");
context.useClasses("com.jstarcraft.rns.model.benchmark");
// 设置Python脚本使用到的Java变量
ScriptScope scope = new ScriptScope();
scope.createAttribute("loader", loader);

// 执行Python脚本
ScriptExpression expression = new PythonExpression(context, scope, script);
Map<String, Double> data = expression.doWith(Map.class);
```

#### JStarCraft-Ruby

* [完整示例](https://github.com/HongZhaoHua/jstarcraft-rns/tree/master/src/test/java/com/jstarcraft/rns/script)

* 编写Ruby脚本训练与评估模型并保存到Model.rb文件

```ruby
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
```

* 使用JStarCraft框架从Model.rb文件加载并执行Ruby脚本

```java
// 获取Ruby脚本
File file = new File(ScriptTestCase.class.getResource("Model.rb").toURI());
String script = FileUtils.readFileToString(file, StringUtility.CHARSET);

// 设置Ruby脚本使用到的Java类
ScriptContext context = new ScriptContext();
context.useClasses(Properties.class, Assert.class);
context.useClass("Configurator", MapConfigurator.class);
context.useClasses("com.jstarcraft.ai.evaluate");
context.useClasses("com.jstarcraft.rns.task");
context.useClasses("com.jstarcraft.rns.model.benchmark");
// 设置Ruby脚本使用到的Java变量
ScriptScope scope = new ScriptScope();
scope.createAttribute("loader", loader);

// 执行Ruby脚本
ScriptExpression expression = new RubyExpression(context, scope, script);
Map<String, Double> data = expression.doWith(Map.class);
Assert.assertEquals(0.005825241096317768D, data.get("precision"), 0D);
Assert.assertEquals(0.011579763144254684D, data.get("recall"), 0D);
Assert.assertEquals(1.270874261856079D, data.get("mae"), 0D);
Assert.assertEquals(2.425075054168701D, data.get("mse"), 0D);
```

****

## 对比

#### 个性化模型

* 基准模型

| 名称 | 数据集 | 训练 | 预测 | AUC | MAP | MRR | NDCG | Novelty | Precision | Recall |
| :----: | :----: | :----: | :----: | :----: | :----: | :----: | :----: | :----: | :----: | :----: |
| MostPopular | filmtrust | 43 | 273 | 0.92080 | 0.41246 | 0.57196 | 0.51583 | 11.79295 | 0.33230 | 0.62385 |
| RandomGuess | filmtrust | 38 | 391 | 0.51922 | 0.00627 | 0.02170 | 0.01121 | 91.94900 | 0.00550 | 0.01262 |

| 名称 | 数据集 | 训练 | 预测 | MAE | MPE | MSE |
| :----: | :----: | :----: | :----: | :----: | :----: | :----: |
| ConstantGuess | filmtrust | 137 | 45 | 1.05608 | 1.00000 | 1.42309 |
| GlobalAverage | filmtrust | 60 | 13 | 0.71977 | 0.77908 | 0.85199 |
| ItemAverage | filmtrust | 59 | 12 | 0.72968 | 0.97242 | 0.86413 |
| ItemCluster | filmtrust | 471 | 41 | 0.71976 | 0.77908 | 0.85198 |
| RandomGuess | filmtrust | 38 | 8 | 1.28622 | 0.99597 | 2.47927 |
| UserAverage | filmtrust | 35 | 9 | 0.64618 | 0.97242 | 0.70172 |
| UserCluster | filmtrust | 326 | 45 | 0.71977 | 0.77908 | 0.85199 |

* 协同模型

| 名称 | 数据集 | 训练 | 预测 | AUC | MAP | MRR | NDCG | Novelty | Precision | Recall |
| :----: | :----: | :----: | :----: | :----: | :----: | :----: | :----: | :----: | :----: | :----: |
| AoBPR | filmtrust | 12448 | 253 | 0.89324 | 0.38967 | 0.53990 | 0.48338 | 21.13004 | 0.32295 | 0.56864 |
| AspectRanking | filmtrust | 177 | 58 | 0.85130 | 0.15498 | 0.42480 | 0.26012 | 37.36273 | 0.13302 | 0.31292 |
| BHFreeRanking | filmtrust | 5720 | 4257 | 0.92080 | 0.41316 | 0.57231 | 0.51662 | 11.79567 | 0.33276 | 0.62500 |
| BPR | filmtrust | 4228 | 137 | 0.89390 | 0.39886 | 0.54790 | 0.49180 | 21.46738 | 0.32268 | 0.57623 |
| BUCMRanking | filmtrust | 2111 | 1343 | 0.90782 | 0.39794 | 0.55776 | 0.49651 | 13.08073 | 0.32407 | 0.59141 |
| CDAE | filmtrust | 89280 | 376 | 0.91880 | 0.40759 | 0.56855 | 0.51089 | 11.82466 | 0.33051 | 0.61967 |
| CLiMF | filmtrust | 48429 | 140 | 0.88293 | 0.37395 | 0.52407 | 0.46572 | 19.38964 | 0.32049 | 0.54605 |
| DeepFM | filmtrust | 69264 | 99 | 0.91679 | 0.40580 | 0.56995 | 0.50985 | 11.90242 | 0.32719 | 0.61426 |
| EALS | filmtrust | 850 | 185 | 0.86132 | 0.31263 | 0.45680 | 0.39475 | 20.08964 | 0.27381 | 0.46271 |
| FISMAUC | filmtrust | 2338 | 663 | 0.91216 | 0.40032 | 0.55730 | 0.50114 | 12.07469 | 0.32845 | 0.60294 |
| FISMRMSE | filmtrust | 4030 | 729 | 0.91482 | 0.40795 | 0.56470 | 0.50920 | 11.91234 | 0.33044 | 0.61107 |
| GBPR | filmtrust | 14827 | 150 | 0.92113 | 0.41003 | 0.57144 | 0.51464 | 11.87609 | 0.33090 | 0.62512 |
| HMM | game | 38697 | 11223 | 0.80559 | 0.18156 | 0.37516 | 0.25803 | 16.01041 | 0.14572 | 0.22810 |
| ItemBigram | filmtrust | 12492 | 61 | 0.88807 | 0.33520 | 0.46870 | 0.42854 | 17.11172 | 0.29191 | 0.53308 |
| ItemKNNRanking | filmtrust | 2683 | 250 | 0.87438 | 0.33375 | 0.46951 | 0.41767 | 20.23449 | 0.28581 | 0.49248 |
| LDA | filmtrust | 696 | 161 | 0.91980 | 0.41758 | 0.58130 | 0.52003 | 12.31348 | 0.33336 | 0.62274 |
| LambdaFMStatic | game | 25052 | 27078 | 0.87064 | 0.27294 | 0.43640 | 0.34794 | 16.47330 | 0.13941 | 0.35696 |
| LambdaFMWeight | game | 25232 | 28156 | 0.87339 | 0.27333 | 0.43720 | 0.34728 | 14.71413 | 0.13742 | 0.35252 |
| LambdaFMDynamic | game | 74218 | 27921 | 0.87380 | 0.27288 | 0.43648 | 0.34706 | 13.50578 | 0.13822 | 0.35132 |
| ListwiseMF | filmtrust | 714 | 161 | 0.90820 | 0.40511 | 0.56619 | 0.50521 | 15.53665 | 0.32944 | 0.60092 |
| PLSA | filmtrust | 1027 | 116 | 0.89950 | 0.41217 | 0.57187 | 0.50597 | 16.01080 | 0.32401 | 0.58557 |
| RankALS | filmtrust | 3285 | 182 | 0.85901 | 0.29255 | 0.51014 | 0.38871 | 25.27197 | 0.22931 | 0.42509 |
| RankCD | product | 1442 | 8905 | 0.56271 | 0.01253 | 0.04618 | 0.02682 | 55.42019 | 0.01548 | 0.03520 |
| RankSGD | filmtrust | 309 | 113 | 0.80388 | 0.23587 | 0.42290 | 0.32081 | 42.83305 | 0.19363 | 0.35374 |
| RankVFCD | product | 54273 | 6524 | 0.58022 | 0.01784 | 0.06181 | 0.03664 | 62.95810 | 0.01980 | 0.04852 |
| SLIM | filmtrust | 62434 | 91 | 0.91849 | 0.44851 | 0.61083 | 0.54557 | 16.67990 | 0.34019 | 0.63021 |
| UserKNNRanking | filmtrust | 1154 | 229 | 0.90752 | 0.41616 | 0.57525 | 0.51393 | 12.90921 | 0.32891 | 0.60152 |
| VBPR | product | 184473 | 15304 | 0.54336 | 0.00920 | 0.03522 | 0.01883 | 45.05101 | 0.01037 | 0.02266 |
| WBPR | filmtrust | 20705 | 183 | 0.78072 | 0.24647 | 0.33373 | 0.30442 | 17.18609 | 0.25000 | 0.35516 |
| WRMF | filmtrust | 482 | 158 | 0.90616 | 0.43278 | 0.58284 | 0.52480 | 15.17956 | 0.32918 | 0.60780 |
| RankGeoFM | FourSquare | 368436 | 1093 | 0.72708 | 0.05485 | 0.24012 | 0.11057 | 37.50040 | 0.07866 | 0.08640 |
| SBPR | filmtrust | 41481 | 247 | 0.91010 | 0.41189 | 0.56480 | 0.50726 | 15.67905 | 0.32440 | 0.59699 |

| 名称 | 数据集 | 训练 | 预测 | MAE | MPE | MSE |
| :----: | :----: | :----: | :----: | :----: | :----: | :----: |
| AspectRating | filmtrust | 220 | 5 | 0.65754 | 0.97918 | 0.71809 |
| ASVDPlusPlus | filmtrust | 5631 | 8 | 0.71975 | 0.77921 | 0.85196 |
| BiasedMF | filmtrust | 92 | 6 | 0.63157 | 0.98387 | 0.66220 |
| BHFreeRating | filmtrust | 6667 | 76 | 0.71974 | 0.77908 | 0.85198 |
| BPMF | filmtrust | 25942 | 52 | 0.66504 | 0.98465 | 0.70210 |
| BUCMRating | filmtrust | 1843 | 30 | 0.64834 | 0.99102 | 0.67992 |
| CCD | product | 15715 | 9 | 0.96670 | 0.93947 | 1.62145 |
| FFM | filmtrust | 5422 | 6 | 0.63446 | 0.98413 | 0.66682 |
| FMALS | filmtrust | 1854 | 5 | 0.64788 | 0.96032 | 0.73636 |
| FMSGD | filmtrust | 3496 | 10 | 0.63452 | 0.98426 | 0.66710 |
| GPLSA | filmtrust | 2567 | 7 | 0.67311 | 0.98972 | 0.79883 |
| IRRG | filmtrust | 40284 | 6 | 0.64766 | 0.98777 | 0.73700 |
| ItemKNNRating | filmtrust | 2052 | 27 | 0.62341 | 0.95394 | 0.67312 |
| LDCC | filmtrust | 8650 | 84 | 0.66383 | 0.99284 | 0.70666 |
| LLORMA | filmtrust | 16618 | 82 | 0.64930 | 0.96591 | 0.76067 |
| MFALS | filmtrust | 2944 | 5 | 0.82939 | 0.94549 | 1.30547 |
| NMF | filmtrust | 1198 | 8 | 0.67661 | 0.96604 | 0.83493 |
| PMF | filmtrust | 215 | 7 | 0.72959 | 0.98165 | 0.99948 |
| RBM | filmtrust | 19551 | 270 | 0.74484 | 0.98504 | 0.88968 |
| RFRec | filmtrust | 16330 | 54 | 0.64008 | 0.97112 | 0.69390 |
| SVDPlusPlus | filmtrust | 452 | 26 | 0.65248 | 0.99141 | 0.68289 |
| URP | filmtrust | 1514 | 25 | 0.64207 | 0.99128 | 0.67122 |
| UserKNNRating | filmtrust | 1121 | 135 | 0.63933 | 0.94640 | 0.69280 |
| RSTE | filmtrust | 4052 | 10 | 0.64303 | 0.99206 | 0.67777 |
| SocialMF | filmtrust | 918 | 13 | 0.64668 | 0.98881 | 0.68228 |
| SoRec | filmtrust | 1048 | 10 | 0.64305 | 0.99232 | 0.67776 |
| SoReg | filmtrust | 635 | 8 | 0.65943 | 0.96734 | 0.72760 |
| TimeSVD | filmtrust | 11545 | 36 | 0.68954 | 0.93326 | 0.87783 |
| TrustMF | filmtrust | 2038 | 7 | 0.63787 | 0.98985 | 0.69017 |
| TrustSVD | filmtrust | 12465 | 22 | 0.61984 | 0.98933 | 0.63875 |
| AssociationRule | filmtrust | 2628 | 195 | 0.90853 | 0.41801 | 0.57777 | 0.51621 | 12.65794 | 0.33263 | 0.60700 |
| PersonalityDiagnosis | filmtrust | 45 | 642 | 0.72964 | 0.76620 | 1.03071 |
| PRankD | filmtrust | 3321 | 170 | 0.74472 | 0.22894 | 0.32406 | 0.28390 | 45.81069 | 0.19436 | 0.32904 |
| SlopeOne | filmtrust | 135 | 28 | 0.63788 | 0.96175 | 0.71057 |

* 内容模型

| 名称 | 数据集 | 训练 | 预测 | AUC | MAP | MRR | NDCG | Novelty | Precision | Recall |
| :----: | :----: | :----: | :----: | :----: | :----: | :----: | :----: | :----: | :----: | :----: |
| EFMRanking | dc_dense | 2066 | 2276 | 0.61271 | 0.01611 | 0.04631 | 0.04045 | 53.26140 | 0.02387 | 0.07357 |
| TFIDF | musical_instruments | 942 | 1085 | 0.52756 | 0.01067 | 0.01917 | 0.01773 | 72.71228 | 0.00588 | 0.03103 |

| 名称 | 数据集 | 训练 | 预测 | MAE | MPE | MSE |
| :----: | :----: | :----: | :----: | :----: | :----: | :----: |
| EFMRating | dc_dense | 659 | 8 | 0.61546 | 0.85364 | 0.78279 |
| HFT | musical_instruments | 162753 | 13 | 0.64272 | 0.94886 | 0.81393 |
| TopicMFAT | musical_instruments | 6907 | 7 | 0.61896 | 0.98734 | 0.72545 |
| TopicMFMT | musical_instruments | 6323 | 7 | 0.61896 | 0.98734 | 0.72545 |

****

## 版本

****

## 参考

#### 个性化模型

* 基准模型

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

* 协同模型

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

* 内容模型

| 名称 | 问题 | 说明/论文 |
| :----: | :----: | :----: |
| EFM | Ranking Rating  | Explicit factor models for explainable recommendation based on phrase-level sentiment analysis |
| TF-IDF | Ranking | 词频-逆文档频率 |
| HFT | Rating | Hidden factors and hidden topics: understanding rating dimensions with review text |
| TopicMF | Rating | TopicMF: Simultaneously Exploiting Ratings and Reviews for Recommendation |

#### 数据集

* [Amazon Dataset](http://jmcauley.ucsd.edu/data/amazon/)
* [Bibsonomy Dataset](https://www.kde.cs.uni-kassel.de/wp-content/uploads/bibsonomy/)
* [BookCrossing Dataset](https://grouplens.org/datasets/book-crossing/)
* [Ciao Dataset](https://www.cse.msu.edu/~tangjili/datasetcode/truststudy.htm)
* [Douban Dataset](http://smiles.xjtu.edu.cn/Download/Download_Douban.html)
* [Eachmovie Dataset](https://grouplens.org/datasets/eachmovie/)
* [Epinions Dataset](http://www.trustlet.org/epinions.html)
* [Foursquare Dataset](https://sites.google.com/site/yangdingqi/home/foursquare-dataset)
* [Goodbooks Dataset](http://fastml.com/goodbooks-10k-a-new-dataset-for-book-recommendations/)
* [Gowalla Dataset](http://snap.stanford.edu/data/loc-gowalla.html)
* [HetRec2011 Dataset](https://grouplens.org/datasets/hetrec-2011/)
* [Jest Joker Dataset](https://grouplens.org/datasets/jester/)
* [Large Movie Review Dataset](http://ai.stanford.edu/~amaas/data/sentiment/)
* [MovieLens Dataset](https://grouplens.org/datasets/movielens/)
* [Newsgroups Dataset](http://qwone.com/~jason/20Newsgroups/)
* [Stanford Large Network Dataset](http://snap.stanford.edu/data/)
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

