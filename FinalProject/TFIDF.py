import nltk
import string
import os

from sklearn.feature_extraction.text import TfidfVectorizer
from nltk.stem.porter import PorterStemmer
import re

# path = r"D:\\Carnegie Mellon University\\Sem 4\\Big Data Analytics\\Document Clustering\\"
path = r"D:\\Carnegie Mellon University\\Sem 4\\Big Data Analytics\\Document Clustering\\Sample\\"
token_dict = {}
stemmer = PorterStemmer()


def stem_tokens(tokens, stemmer):
    stemmed = []
    for item in tokens:
        stemmed.append(stemmer.stem(item))
    return stemmed


def tokenize(text):
    tokens = nltk.word_tokenize(text)
    stems = stem_tokens(tokens, stemmer)
    return stems


def calculate_tf_idf():
    for subdir, dirs, files in os.walk(path):
        for file in files:
            file_path = subdir + os.path.sep + file
            line = open(file_path, 'r', encoding="utf-8", errors='replace')
            text = line.read()
            text = re.sub('[^a-zA-Z]+', ' ', text)
            lowers = text.lower()

            no_punctuation = lowers.translate(string.punctuation)
            token_dict[file] = no_punctuation

    # Here goes the TFIDF Generation
    tfidf = TfidfVectorizer(tokenizer=tokenize, stop_words='english')

    # Learn vocabulary and idf, return term-document matrix.
    tfidf_matrix = tfidf.fit_transform(token_dict.values())

    feature_names = tfidf.get_feature_names()
    return [tfidf, tfidf_matrix, feature_names]


