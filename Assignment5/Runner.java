package Assignment5;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;


public class Runner 
{
	public static void main(String[] args) throws Exception 
	{
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "random forest");
		job.setJarByClass(Runner.class);
		// job.setJar("rf.jar");
		job.setMapperClass(Map.class);
		job.setCombinerClass(Reduce.class);
		job.setReducerClass(Reduce.class);
		
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		FileInputFormat.addInputPath(job, new Path(args[1]));
		FileOutputFormat.setOutputPath(job, new Path(args[2]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
	
	public static class Map extends Mapper<Object, Text, Text, Text>
	{
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException 
		{
			DecisionTree tree = generateDecisionTree();
			String serializedTree = GsonUtils.serialize(tree);
			// System.out.println(serializedTree);
			
			Text treeText = new Text();
			treeText.set(serializedTree);
			
			Text treeId = new Text();
			treeId.set(tree.hashCode() + "");
			
			// System.out.println(tree.hashCode());
			
			context.write(treeId, treeText);
			
		}
	}

	public static class Reduce extends Reducer<Text,Text,Text,Text> 
	{

		public void reduce(Text key, Iterable<Text> values, Context context ) throws IOException, InterruptedException 
		{
			StringBuilder forest = new StringBuilder();
			
			for (Text val : values) 
			{
				forest.append(val.toString() + "\n");
				System.out.println("Key: " + key + "Value: " + val.toString());
			}
			
			WriteCassandra(forest.toString());
			context.write(key, new Text(forest.toString()));
		}
	}

	public static DecisionTree generateDecisionTree()
	{


		// This is the pattern of my raw data files.
		// The pattern given below means the first column is Nominal, then next three are categorical, then next 2 are nominal
		// String inputDataLayout= "N,3,C,2,N,C,4,N,C,8,N,2,C,19,N,L,I";

		// For text file
		// String inputDataLayout = "I,C,9,N,L,I";

		// For cassandra table
		String inputDataLayout= "C,I,3,N,L,I,6,N";


		// Step 1- Start preparing data from the files to Decision Trees. Instantiate a class that has methods for this purpose. 
		DescribeTrees DT = new DescribeTrees();

		// Step 2- Generate training and testing data with the help of above class
		ArrayList<ArrayList<String>> trainDataInstances = DT.prepareInputDataFromCassandra("forex_train", inputDataLayout);
		// ArrayList<ArrayList<String>> trainDataInstances = DT.prepareInputDataFromRawFile(traindata, inputDataLayout);

		

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


		// Step 7- Start tree generation
		DecisionTree tree = new DecisionTree(finalDataLayout, trainDataInstances, numTotalFeatures, Ms);
		
		return tree;
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

	public static void WriteCassandra(String serializedTree)
	{
		Cluster cluster;
		Session session;

		cluster = Cluster.builder().addContactPoint("127.0.0.1").build();

		session = cluster.connect("mykeyspace");

		// System.out.println("Time: " + training.get(0) + "\tPredicted: " + pred + "\tActual: " + actual);
		// Insert one record into the users table
		String query = "INSERT INTO Tree (tree) VALUES ('"+serializedTree+"');";
		session.execute(query);

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
