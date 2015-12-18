__author__ = 'sankalp'
import TextCleanup
import TFIDF

def store_tfidf_to_db(feature_names, tfidf_matrix):
    from MySQLConnecter import MySQL
    db_obj = MySQL()
    for col in tfidf_matrix.nonzero()[1]:
        word = feature_names[col]
        score = tfidf_matrix[0, col]
        db_obj = MySQL.insert(word, score)
        pass

tf_idf_params = TFIDF.calculate_tf_idf()
tfidf = tf_idf_params[0]
tfidf_matrix = tf_idf_params[1]
feature_names = tf_idf_params[2]


# To store the TFIDF result in to the MySQL database
store_tfidf_to_db(feature_names, tfidf_matrix)

# TF-IDF is a sparse matrix. To convert it into a dense matrix, uncomment this line.
dense = tfidf_matrix.todense()


print('Enter a search term: ')
# str = 'Dispersion and migration of uranium (U) and other toxic metals and radionuclides from'
str = input()
response = tfidf.transform([str])


# To print out the values and their response
for col in response.nonzero()[1]:
    print(feature_names[col], ' - ', response[0, col])
    pass

# To print k-means clustering result
from KMeans import k_means
k_means(tfidf_matrix, 3)


# Create JSON
json_string = "{\"name\" : \"Root\", \"Children\" : ["
for col in response.nonzero()[1]:
    word = feature_names[col]
    score = response[0, col]
    # print(word, ' - ', score)
    json_string += "{\"name\": \"%s\", \"children\": null, \"size\": %s }," %(word, score)

# Trim last comma
json_string = json_string[:-1]
json_string += "],\"size\": 41103329}"


print('Done!')

