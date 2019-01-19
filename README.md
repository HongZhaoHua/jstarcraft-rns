JStarCraft Recommendation
==========

[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

*****

#### JStarCraft Recommendation是一个推荐算法的演示项目,目标是为推荐系统领域的学术界与工业界提供推荐算法参考实现.遵循Apache 2.0协议.

在此特别感谢[LibRec团队](https://github.com/guoguibing/librec),也特别感谢推荐系统QQ群(274750470)提供的支持与帮助.

|作者|洪钊桦|
|---|---
|E-mail|110399057@qq.com, jstarcraft@gmail.com

*****

## JStarCraft Recommendation主要特性如下

* 1.支持并行计算
* 2.支持硬件加速
* 3.支持模型保存与装载
* 4.多种推荐算法
* 5.多种评估指标
* 6.独立的环境配置与算法配置

*****

## 基本问题:排序(Ranking)与评分(Rating)

```
推荐算法根据基本问题的不同(即目标函数的区别)划分为Ranking预测与Rating预测.
```

#### 通俗点的解释
- Ranking预测基于隐式反馈与关注度
- Rating预测基于显示反馈与满意度

#### Ranking指标
- AUC
- Diversity
- MAP
- MRR
- NDCG
- Novelty
- Precision
- Recall

#### Rating指标
- MAE
- MPE
- MSE/RMSE

```
注意:因为关注度与满意度并不总是一致的,所以Rating预测的分数并不总能用于Ranking指标.
```

*****

## 算法分类:基于协同与基于内容

*****

## 上下文:社交,时间,位置与情感

*****

## 数据集

* [Amazon Product Dataset](http://jmcauley.ucsd.edu/data/amazon/)
* [Bibsonomy Dataset](https://www.kde.cs.uni-kassel.de/wp-content/uploads/bibsonomy/)
* [BookCrossing Dataset](https://grouplens.org/datasets/book-crossing/)
* [Ciao Dataset](https://www.cse.msu.edu/~tangjili/datasetcode/truststudy.htm)
* [Douban Dataset](http://socialcomputing.asu.edu/datasets/Douban)
* [Eachmovie Dataset](https://grouplens.org/datasets/eachmovie/)
* [Epinions Dataset](http://www.trustlet.org/epinions.html)
* [Flixster Dataset](http://socialcomputing.asu.edu/datasets/Flixster)
* [Foursquare Dataset](https://sites.google.com/site/yangdingqi/home/foursquare-dataset)
* [Jest Joker Dataset](https://grouplens.org/datasets/jester/)
* [HetRec2011 Dataset](https://grouplens.org/datasets/hetrec-2011/)
* [Movielens Dataset](https://grouplens.org/datasets/movielens/)
* [Serendipity 2018 Dataset](https://grouplens.org/datasets/serendipity-2018/)
* [Wikilens Dataset](https://grouplens.org/datasets/wikilens/)
* [Yelp Dataset](https://www.yelp.com/dataset)
* [Yongfeng Zhang Dataset](http://yongfeng.me/dataset/)

*****