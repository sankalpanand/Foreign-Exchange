------------------------------------------------------------------------------------------------------------
References-
------------------------------------------------------------------------------------------------------------
http://www.biomedcentral.com/1471-2105/14/S16/S6
https://hadoop.apache.org/docs/current/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html


------------------------------------------------------------------------------------------------------------
File Info-
------------------------------------------------------------------------------------------------------------
DecisionTree.java - This is the class which is responsible for generating the decision tree
DescribeTrees.java- This is the class which parses the schema of the input parameters
GsonUtils.java- - This is the class which holds the methods to serialize/deserialize data
RandomForest.java- It is not used in this assignment, but it is capable of generating a Random Forest.
Runner.java- This is the file which contains the mapper, the reducer and the driver.
SampleOutput- This is the sample output of the map reduce program which is fed to Cassandra.


------------------------------------------------------------------------------------------------------------
Steps to compile and Run-
------------------------------------------------------------------------------------------------------------
1. Using eclipse, make runnable jar for the project

2. Use the command to execute map reduce job- 

hadoop jar /home/sankalp/randomforest/rf.jar Runner /home/sankalp/randomforest/input /home/sankalp/randomforest/output