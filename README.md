JStarCraft RNS
==========

[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

*****

**JStarCraft RNS是一个面向推荐与搜索系统的轻量级引擎.遵循Apache 2.0协议.**

提供满足工业级别场景要求的推荐算法设计与实现.
提供满足工业级别场景要求的搜索算法设计与实现.

在此特别感谢**[LibRec团队](https://github.com/guoguibing/librec)**,也特别感谢**推荐系统QQ群**(274750470)提供的支持与帮助.

|作者|洪钊桦|
|---|---
|E-mail|110399057@qq.com, jstarcraft@gmail.com

*****

## JStarCraft RNS特性

* 1.跨平台
* [2.串行与并行计算](https://github.com/HongZhaoHua/jstarcraft-ai)
* [3.CPU与GPU硬件加速](https://github.com/HongZhaoHua/jstarcraft-ai)
* [4.模型保存与装载](https://github.com/HongZhaoHua/jstarcraft-ai)
* [5.丰富的推荐算法](https://github.com/HongZhaoHua/jstarcraft-rns/wiki/%E6%8E%A8%E8%8D%90%E7%AE%97%E6%B3%95)
    * [基准推荐算法](https://github.com/HongZhaoHua/jstarcraft-rns/wiki/%E6%8E%A8%E8%8D%90%E7%AE%97%E6%B3%95#%E5%9F%BA%E5%87%86%E7%AE%97%E6%B3%95)
    * [协同推荐算法](https://github.com/HongZhaoHua/jstarcraft-rns/wiki/%E6%8E%A8%E8%8D%90%E7%AE%97%E6%B3%95#%E5%8D%8F%E5%90%8C%E7%AE%97%E6%B3%95)
    * [内容推荐算法](https://github.com/HongZhaoHua/jstarcraft-rns/wiki/%E6%8E%A8%E8%8D%90%E7%AE%97%E6%B3%95#%E5%86%85%E5%AE%B9%E7%AE%97%E6%B3%95)
* 6.丰富的搜索算法
    分词
    索引
    查询
* [7.丰富的评估指标](#评估指标)
    * [排序指标](#排序指标)
    * [评分指标](#评分指标)
* 8.独立的环境配置与算法配置
* 9.完整的单元测试

*****

## JStarCraft RNS教程

* 1.使用准备
    * [克隆项目](#克隆项目)
    * [配置依赖](#配置依赖)
* 2.配置推荐器
    * [设置配置](#设置配置)
    * [排序推荐器](#排序推荐器)
    * [评分推荐器](#评分推荐器)
* 3.编码解码推荐器
    * [设置调制解调器](#设置调制解调器)
    * [编码推荐器](#编码推荐器)
    * [解码推荐器](#解码推荐器)

#### 克隆项目

使用Git克隆以下三个项目:
1. [JStarCraft Core](https://github.com/HongZhaoHua/jstarcraft-core)
2. [JStarCraft AI](https://github.com/HongZhaoHua/jstarcraft-ai)
3. [JStarCraft RNS](https://github.com/HongZhaoHua/jstarcraft-rns)

#### 配置依赖

*Maven依赖*

```maven
<dependency>
    <groupId>com.jstarcraft</groupId>
    <artifactId>rns</artifactId>
    <version>1.0</version>
</dependency>
```

*Gradle依赖*

```gradle
compile group: 'com.jstarcraft', name: 'rns', version: '1.0'
```

#### 设置配置

```java
String path = "recommendation/benchmark/randomguess-test.properties";
Configuration configuration = Configuration.valueOf(path);
```

#### 排序推荐器

```java
RankingTask job = new RankingTask(RandomGuessRecommender.class, configuration);
// 训练与测试推荐器
job.execute();
Recommender recommender = job.getRecommender();
```

#### 评分推荐器

```java
RatingTask job = new RatingTask(RandomGuessRecommender.class, configuration);
// 训练与测试推荐器
job.execute();
Recommender recommender = job.getRecommender();
```

#### 设置调制解调器

```java
ModemCodec codec = ModemCodec.JSON;
```

#### 编码推荐器

```java
// 将推荐器编码为字节数组
byte[] data = codec.encodeModel(recommender);
```

#### 解码推荐器

```java
// 将字节数组解码为推荐器
Recommender recommender = (Recommender) codec.decodeModel(data);
```

*****

## 上下文:社交,时间,位置与情感

*****

## 评估指标

#### 排序指标
- AUC
- Diversity
- MAP
- MRR
- NDCG
- Novelty
- Precision
- Recall

#### 评分指标
- MAE
- MPE
- MSE/RMSE

*****

## 数据集

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

*****
