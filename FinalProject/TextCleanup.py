__author__ = 'sankalp'

import nltk
import string
import re
import os
import fnmatch


def get_document_names():
    root_directory = r"D:\\Carnegie Mellon University\\Sem 4\\Big Data Analytics\\Document Clustering"
    file = open('Document_Names.csv', 'w')
    i = 0

    for dirName, subdirList, fileList in os.walk(root_directory):
        for file_name in fnmatch.filter(fileList, "*.txt"):
            line = str(i) + ',' + dirName + '/' + file_name + '\n'
            file.writelines(line)
            i += 1

    file.close()

def get_tokens():
    path = r"D:\\Carnegie Mellon University\\Sem 4\\Big Data Analytics\\Document Clustering"
    with open(path, 'r', encoding="utf-8", errors='replace') as line:
        text = line.read()
        text = re.sub('[^a-zA-Z]+', ' ', text)

        lowers = text.lower()

        # To print, encode the line as below. Otherwise, you will get an exception.
        # print(lowers.encode('ascii', 'ignore'))

        #remove the punctuation using the character deletion step of translate
        no_punctuation = lowers.translate(string.punctuation)
        tokens = nltk.word_tokenize(no_punctuation)
        return tokens


def stem_tokens(tokens, stemmer):
    stemmed = []
    for item in tokens:
        stemmed.append(stemmer.stem(item))
    return stemmed



