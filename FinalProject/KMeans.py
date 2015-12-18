__author__ = 'sankalp'

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.cluster import KMeans
from sklearn.metrics import adjusted_rand_score


def k_means(tfidf_matrix, k):
    model = KMeans(n_clusters=k, init='k-means++', max_iter=100, n_init=1)
    model.fit(tfidf_matrix)
    print("Top terms per cluster:")
    order_centroids = model.cluster_centers_.argsort()[:, ::-1]
    vectorizer = TfidfVectorizer(stop_words='english')
    terms = vectorizer.get_feature_names()
    for i in range(k):
        print("Cluster %d:" % i)
        for ind in order_centroids[i, :10]:
            print('\t%s' % terms[ind])
        print
