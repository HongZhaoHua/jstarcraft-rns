##################################################################################################################################################
# This dataset contains all the extracted product feature word to user opinion word pairs (e.g. service | good, price | reasonable, etc) 
# from each of the reviews from a digital camera review website, as well as the sentiment polarity of these pairs.
#
# We appriciate if one (or more) of the following papers be cited in your publications that benifit from the dataset:
#
# [1] Yongfeng Zhang, Haochen Zhang, Min Zhang, Yiqun Liu and Shaoping Ma. Do Users Rate or Review? Boost Phrase-level Sentiment Labeling with 
#     Review-level Sentiment Classification. In Proceedings of the 37th Annual International ACM SIGIR Conference on Research and Development 
#     on Information Retrieval (SIGIR 2014), July 6 - 11, 2014, Gold Coast, Australia.
# [2] Yunzhi Tan, Yongfeng Zhang, Min Zhang, Yiqun Liu and Shaoping Ma. A Unified Framework for Emotional Elements Extraction based on Finite 
#     State Matching Machine. Natural Language Processing and Chinese Computing, Communications in Computer and Information Science (CCIS), 
#     Volume 400, 2013, pp 60-71.
# [3] Yongfeng Zhang, Guokun Lai, Min Zhang, Yi Zhang, Yiqun Liu and Shaoping Ma. Explicit Factor Models for Explainable Recommendation based 
#     on Phrase-level Sentiment Analysis. In Proceedings of the 37th Annual International ACM SIGIR Conference on Research and Development on 
#     Information Retrieval (SIGIR 2014), July 6 - 11, 2014, Gold Coast, Australia. [PDF]
#
# For any issue or problem of usage please contact us at zhangyf07@gmail.com
# For more dataset please refer to http://yongfeng.me
##################################################################################################################################################

The methodology of sentiment polarity labelling and feature-opinion pair extraction are presented in [1] and [2] respectively, and this dataset is used for explanable recommendation in [3].

Data Format:

A user review is formatted as an XML entry of the form:

<DOC>
userid itemid flavor_rating environment_rating service_rating
review_text
feature-opinion pairs matched in the review_text, each of the form [feature_word, opinion_word, sentiment_polarity, times_of_ occurrence, reversed_or_not]
</DOC>

e.g. [service, good, +1, 1, Y] means that the pair 'service | good' is matched for once in the review, and the pair itself represents a positive sentiment (+1), however, it is reversed (Y means that it is indeed reversed, and N is not reversed) by a negation word (e.g. 'not'), so the final sentiment of this pair in this review would be negative.
