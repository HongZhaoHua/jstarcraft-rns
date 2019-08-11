# JStarCraft RNS

****

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
* [示例](#示例)
    * [JStarCraft RNS引擎与Groovy脚本交互](#JStarCraft-RNS引擎与Groovy脚本交互)
    * [JStarCraft RNS引擎与JS脚本交互](#JStarCraft-RNS引擎与JS脚本交互)
    * [JStarCraft RNS引擎与Lua脚本交互](#JStarCraft-RNS引擎与Lua脚本交互)
    * [JStarCraft RNS引擎与Python脚本交互](#JStarCraft-RNS引擎与Python脚本交互)
* [对比](#对比)
* [版本](#版本)
* [参考](#参考)
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
* [5.丰富的推荐算法](https://github.com/HongZhaoHua/jstarcraft-rns/wiki/%E6%8E%A8%E8%8D%90%E7%AE%97%E6%B3%95)
* 6.丰富的搜索算法
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

###### 设置Maven依赖

```maven
<dependency>
    <groupId>com.jstarcraft</groupId>
    <artifactId>rns</artifactId>
    <version>1.0</version>
</dependency>
```

###### 设置Gradle依赖

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

###### 构建排序任务

```java
RankingTask task = new RankingTask(RandomGuessModel.class, configurator);
// 训练与评估排序模型
task.execute();
```

###### 构建评分任务

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

****

## 示例

#### JStarCraft-RNS引擎与Groovy脚本交互

```java

```

```groovy

```

#### JStarCraft-RNS引擎与JS脚本交互

```java

```

```js

```

#### JStarCraft-RNS引擎与Lua脚本交互

```java

```

```lua

```

#### JStarCraft-RNS引擎与Python脚本交互

* 编写Python脚本训练与评估模型

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

* 使用JStarCraft引擎执行Python脚本

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

[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

****

## 作者

|作者|洪钊桦|
|---|---
|E-mail|110399057@qq.com, jstarcraft@gmail.com

****

## 致谢

特别感谢[LibRec团队](https://github.com/guoguibing/librec)与**推荐系统QQ群**(274750470)在推荐方面提供的支持与帮助.

特别感谢[陆徐刚](https://github.com/luxugang/Lucene-7.5.0)在搜索方面提供的支持与帮助.

****

