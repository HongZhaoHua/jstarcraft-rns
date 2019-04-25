JStarCraft Recommendation
==========

[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

*****

**JStarCraft Recommendation是一个面向推荐系统的轻量级引擎.遵循Apache 2.0协议.**

JStarCraft Recommendation引擎基于[JStarCraft AI框架](https://github.com/HongZhaoHua/jstarcraft-ai-1.0)重构了所有[LibRec引擎](https://github.com/guoguibing/librec)的推荐算法.

在此特别感谢**LibRec团队**,也特别感谢**推荐系统QQ群**(274750470)提供的支持与帮助.

|作者|洪钊桦|
|---|---
|E-mail|110399057@qq.com, jstarcraft@gmail.com

*****

## JStarCraft Recommendation特性

* 1.跨平台
* [2.串行与并行计算](https://github.com/HongZhaoHua/jstarcraft-ai-1.0)
* [3.CPU与GPU硬件加速](https://github.com/HongZhaoHua/jstarcraft-ai-1.0)
* [4.模型保存与装载](https://github.com/HongZhaoHua/jstarcraft-ai-1.0)
* [5.丰富的推荐算法](#算法列表)
    * [基准算法](#基准算法)
    * [协同算法](#协同算法)
    * [内容算法](#内容算法)
* [6.丰富的评估指标](#评估指标)
    * [排序指标](#排序指标)
    * [评分指标](#评分指标)
* 7.独立的环境配置与算法配置
* 8.完整的单元测试

*****

## 基本问题:排序(Ranking)与评分(Rating)

```
根据解决基本问题的不同,将推荐算法与评估指标划分为排序(Ranking)与评分(Rating).
```

```
Ranking与Rating之间的区别是什么?

两者之间的根本区别在于目标函数的不同.
通俗点的解释:
Ranking算法基于隐式反馈数据,趋向于拟合用户的排序.(关注度)
Rating算法基于显示反馈数据,趋向于拟合用户的评分.(满意度)
```

```
Rating算法的分数能不能用于Ranking指标?

关键在于具体场景中,关注度与满意度是否保持一致.
通俗点的解释:
人们关注的东西,并不一定是满意的东西.(例如:个人所得税)
```

*****

## 算法分类:基于协同与基于内容

*****

## 上下文:社交,时间,位置与情感

*****

## 算法列表

#### 基准算法

| 名称 | 问题 | 说明/论文 |
| :----: | :----: | :----: |
| RandomGuess | 排序/评分 | 随机猜测 |
| MostPopular | 排序 | 最受欢迎 |
| ConstantGuess | 评分 | 常量猜测 |
| GlobalAverage | 评分 | 全局平均 |
| ItemAverage | 评分 | 物品平均 |
| ItemCluster | 评分 | 物品聚类 |
| UserAverage | 评分 | 用户平均 |
| UserCluster | 评分 | 用户聚类 |

#### 协同算法

| 名称 | 问题 | 说明/论文 |
| :----: | :----: | :----: |
| AspectModel | 排序/评分 | Latent class models for collaborative filtering |
| BHFree | 排序/评分 | Balancing Prediction and Recommendation Accuracy: Hierarchical Latent Factors for Preference Data |
| BUCM | 排序/评分 | Modeling Item Selection and Relevance for Accurate Recommendations |
| ItemKNN | 排序/评分 | 基于物品的协同过滤 |
| UserKNN | 排序/评分 | 基于用户的协同过滤 |
| AoBPR | 排序 | Improving pairwise learning for item recommendation from implicit feedback |
| BPR | 排序 | BPR: Bayesian Personalized Ranking from Implicit Feedback |
| CLiMF | 排序 | CLiMF: learning to maximize reciprocal rank with collaborative less-is-more filtering |
| EALS | 排序 | Collaborative filtering for implicit feedback dataset |
| FISM | 排序 | FISM: Factored Item Similarity Models for Top-N Recommender Systems |
| GBPR | 排序 | GBPR: Group Preference Based Bayesian Personalized Ranking for One-Class Collaborative Filtering |
| HMMForCF | 排序 | A Hidden Markov Model Purpose: A class for the model, including parameters |
| ItemBigram | 排序 | Topic Modeling: Beyond Bag-of-Words |
| LambdaFM | 排序 | LambdaFM: Learning Optimal Ranking with Factorization Machines Using Lambda Surrogates |
| LDA | 排序 | Latent Dirichlet Allocation for implicit feedback |
| ListwiseMF | 排序 | List-wise learning to rank with matrix factorization for collaborative filtering |
| PLSA | 排序 | Latent semantic models for collaborative filtering |
| RankALS | 排序 | Alternating Least Squares for Personalized Ranking |
| RankSGD | 排序 | Collaborative Filtering Ensemble for Ranking |
| SLIM | 排序 | SLIM: Sparse Linear Methods for Top-N Recommender Systems |
| WBPR | 排序 | Bayesian Personalized Ranking for Non-Uniformly Sampled Items |
| WRMF | 排序 | Collaborative filtering for implicit feedback datasets |
| AsymmetricSVD++ | 评分 | Factorization Meets the Neighborhood: a Multifaceted Collaborative Filtering Model |
| AutoRec | 评分 | AutoRec: Autoencoders Meet Collaborative Filtering |
| BPMF | 评分 | Bayesian Probabilistic Matrix Factorization using Markov Chain Monte Carlo |
| CCD | 评分 | Large-Scale Parallel Collaborative Filtering for the Netflix Prize |
| FFM | 评分 | Field Aware Factorization Machines for CTR Prediction |
| GPLSA | 评分 | Collaborative Filtering via Gaussian Probabilistic Latent Semantic Analysis |
| IRRG | 评分 | Exploiting Implicit Item Relationships for Recommender Systems |
| MFALS | 评分 | Large-Scale Parallel Collaborative Filtering for the Netflix Prize |
| NMF | 评分 | Algorithms for Non-negative Matrix Factorization |
| PMF | 评分 | PMF: Probabilistic Matrix Factorization |
| RBM | 评分 | Restricted Boltzman Machines for Collaborative Filtering |
| RF-Rec | 评分 | RF-Rec: Fast and Accurate Computation of Recommendations based on Rating Frequencies |
| SVD++ | 评分 | Factorization Meets the Neighborhood: a Multifaceted Collaborative Filtering Model |
| URP | 评分 | User Rating Profile: a LDA model for rating prediction |

#### 内容算法

| 名称 | 问题 | 说明/论文 |
| :----: | :----: | :----: |
| EFM | 排序/评分 | Explicit factor models for explainable recommendation based on phrase-level sentiment analysis |
| TF-IDF | 排序 | 词频-逆文档频率 |
| HFT | 评分 | Hidden factors and hidden topics: understanding rating dimensions with review text |
| TopicMF | 评分 | TopicMF: Simultaneously Exploiting Ratings and Reviews for Recommendation |

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
* [Jest Joker Dataset](https://grouplens.org/datasets/jester/)
* [HetRec2011 Dataset](https://grouplens.org/datasets/hetrec-2011/)
* [Movielens Dataset](https://grouplens.org/datasets/movielens/)
* [Serendipity 2018 Dataset](https://grouplens.org/datasets/serendipity-2018/)
* [Wikilens Dataset](https://grouplens.org/datasets/wikilens/)
* [Yelp Dataset](https://www.yelp.com/dataset)
* [Yongfeng Zhang Dataset](http://yongfeng.me/dataset/)

*****
