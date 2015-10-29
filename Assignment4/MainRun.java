
import java.util.ArrayList;
import java.util.HashMap;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class MainRun 
{
	public static void main(String[] args)
	{
		MainRun obj = new MainRun();
		// obj.ReadCassandra();
		obj.RandomForest();
		
	}
	
	public void RandomForest()
	{
		System.out.println("Random-Forest generation starts...");

//		String traindata = "/home/sankalp/workspace/BigDataAnalytics/src/KDDTrainSmall.txt";
//		String testdata = "/home/sankalp/workspace/BigDataAnalytics/src/KDDTestSmall.txt";
		
		String traindata = "/home/sankalp/Downloads/OneDrive-2015-10-28/input.csv";
		String testdata = "/home/sankalp/Downloads/OneDrive-2015-10-28/output.csv";
		
		int numTrees=10;
		int numThreads=1;
		boolean shouldTestForAccuracy = true;
		boolean shouldTestWithThreads = true;

		// This is the pattern of my raw data files.
		// The pattern given below means the first column is Nominal, then next three are categorical, then next 2 are nominal
		// String inputDataLayout= "N,3,C,2,N,C,4,N,C,8,N,2,C,19,N,L,I";
		String inputDataLayout= "I,C,9,N,L,I";

		
		// Step 1- Start preparing data from the files to Decision Trees. Instantiate a class that has methods for this purpose. 
		DescribeTrees DT = new DescribeTrees(traindata, inputDataLayout);
		
		// Step 2- Generate training and testing data with the help of above class
		ArrayList<ArrayList<String>> trainDataInstances = DT.prepareInputData("forex", inputDataLayout);
		ArrayList<ArrayList<String>> testDataList = DT.prepareInputData("forex", inputDataLayout);


		// Step 3- Create frequency table of how many times each label appeared
		HashMap<String, Integer> labelFreq = new HashMap<String, Integer>();
		for(ArrayList<String> instance : trainDataInstances)
		{
			String label = instance.get(instance.size()-1);
			if(labelFreq.containsKey(label))
				labelFreq.put(label, labelFreq.get(label)+1);
			else
				labelFreq.put(label, 1);				
		}
		int totalUniqueLabels = labelFreq.size();

		// Step 4- Get total number of features in the final dataset
		ArrayList<Character> finalDataLayout = DT.CreateFinalLayout(inputDataLayout);
		int numTotalFeatures = finalDataLayout.size()-1;

		// Step 5- Set the number of attributes to be chosen ( sqrt(M) is set to default ). 
		// If sqrt(N) < 1 then set some default value
		int numChosenAttris = (int) Math.sqrt(numTotalFeatures);		
		int Ms;
		if(numChosenAttris<1)
			Ms = (int)Math.round(Math.log(numTotalFeatures)/Math.log(2)+1);
		else
			Ms=numChosenAttris;
		
		
		// Step 6- Generate random forest
		RandomForest RFC = new RandomForest(finalDataLayout, numTrees,numThreads, numTotalFeatures, Ms, totalUniqueLabels, trainDataInstances, testDataList);
		
		// Step 7- Test for accuracy
		RFC.Start(shouldTestForAccuracy, shouldTestWithThreads);
		
		// Step 8 - Get result
		HashMap<ArrayList<String>, String> result = RFC.getFinalPrediction();
		
		WriteCassandra(result);
		// DisplayResults(result);
		
		System.out.println("Done");
	}
	
	public void ReadCassandra()
	{
		Cluster cluster;
		Session session;
		
		// Connect to the cluster and keyspace "demo"
		cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
		session = cluster.connect("mykeyspace");
		
		// Use select to get the user we just entered
		ResultSet results = session.execute("SELECT * FROM forex");
		for (Row row : results) 
		{
			System.out.println(row.getString("currency"));
			
		}
		
		// Clean up the connection by closing it
		cluster.close();

	}
	
	public void WriteCassandra(HashMap<ArrayList<String>, String> result)
	{
		Cluster cluster;
		Session session;
		
		// Connect to the cluster and keyspace "demo"
		cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
		session = cluster.connect("mykeyspace");
		
		
		
		for(ArrayList<String> training : result.keySet())
		{
			String pred = training.get(training.size()-1);
			String actual = result.get(training);
			String timestamp = training.get(0);
			
			// System.out.println("Time: " + training.get(0) + "\tPredicted: " + pred + "\tActual: " + actual);
			// Insert one record into the users table
			String query = "INSERT INTO predictions (timestamp, predicted, actual) VALUES ('"+timestamp+"', '"+pred+"', '"+actual+"');";
			session.execute(query);
		}
		
		
		// Clean up the connection by closing it
		cluster.close();
	}
	
	public void DisplayResults(HashMap<ArrayList<String>, String> result)
	{
		
		for(ArrayList<String> training : result.keySet())
		{
			String pred = training.get(training.size()-1);
			String actual = result.get(training);
			String timestamp = training.get(0);
			
			System.out.println("Time: " + timestamp + "\tPredicted: " + pred + "\tActual: " + actual);
		}
		
	}
}
