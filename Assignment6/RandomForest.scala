import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import datastax.spark.connector._
import org.apache.spark.mllib.regression.LabeledPoint

object RandomForest 
{
	def main(args: Array[String]) 
	{
		println("Start...\n")
		
		// Step 1- Build a SparkConf object that contains information about your application. 
		val sparkConf = new SparkConf()
		sparkConf.setAppName("Random Forest on Spark")
		sparkConf.setMaster("local[*]") // Run Spark locally with as many worker threads as logical cores on your machine.
    sparkConf.set("spark.cassandra.connection.host", "127.0.0.1")
		
		// Step 2- Create a SparkContext object, which tells Spark how to access a cluster
		val sc = new SparkContext(sparkConf)
		
		// Step 3 - Load training data in RDD (Resilient Distributed Dataset)
		val trainRDD = sc.cassandraTable("forex", "forex_train")
    val trainingData : RDD[LabeledPoint] = trainRDD.map 
    { 
		  row  => 
		    { 
            (LabeledPoint(row.getInt("bidDir").toDouble, 
            Vectors.dense(9, Array(0, 1, 2, 3, 4, 5, 6, 7, 8), Array
                (
                  row.getInt("minBid"), 
                  row.getInt("maxBid"), 
                  row.getInt("avgBid"), 
                  
                  row.getInt("minAsk"), 
                  row.getInt("maxAsk"), 
                  row.getInt("avgAsk"),
                  
                  row.getInt("minDelta"), 
                  row.getInt("maxDelta"), 
                  row.getInt("avgDelta"),
                  ))))
        }
		}
    
		
		// Step 4 - Load testing data in RDD (Resilient Distributed Dataset)
    val testRDD = sc.cassandraTable("forex", "forex_test")
    val testingData : RDD[LabeledPoint] = testRDD.map 
    {
      row  => 
		    { 
            (LabeledPoint(row.getInt("bidDir").toDouble, 
            Vectors.dense(9, Array(0, 1, 2, 3, 4, 5, 6, 7, 8), Array(
                  row.getInt("minBid"), 
                  row.getInt("maxBid"), 
                  row.getInt("avgBid"), 
                  
                  row.getInt("minAsk"), 
                  row.getInt("maxAsk"), 
                  row.getInt("avgAsk"),
                  
                  row.getInt("minDelta"), 
                  row.getInt("maxDelta"), 
                  row.getInt("avgDelta"),
                  ))))
        }
    }
		
		
    
    // Step 5 - Train the RandomForest model.
    val numClasses = 2
    val categoricalFeaturesInfo = Map[Int, Int]()
    val numTrees = 3 // Use more in practice.
    val featureSubsetStrategy = "auto" // Let the algorithm choose.
    val impurity = "gini"
    val maxDepth = 4
    val maxBins = 32
    
    val model = RandomForest.trainClassifier(trainingData, 
                                            numClasses, 
                                            categoricalFeaturesInfo, 
                                            numTrees, 
                                            featureSubsetStrategy, 
                                            impurity, 
                                            maxDepth, 
                                            maxBins)
    
    // Step 6 - Evaluate model on test instances and compute test error
    val labelAndPreds = testingData.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    
    val testErr = labelAndPreds.filter(r => r._1 != r._2).count.toDouble / testingData.count()
    println("Test Error = " + testErr)
    println("Learned classification forest model:\n" + model.toDebugString)
    
    

		println("End...")
	}
}