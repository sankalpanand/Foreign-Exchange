package Assignment5;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RandomForest {
  
	private static int NUM_THREADS;//=Runtime.getRuntime().availableProcessors();
	//private static final int NUM_THREADS=1;
	
	/** the number of categorical responses of the data (the classes, the "Y" values) */
	public static int C;
	
	/** the number of attributes in the data - set this before beginning the forest creation */
	public static int M;
	
	/** Of the M total attributes, the random forest computation requires a subset of them
	 * to be used and picked via random selection. "Ms" is the number of attributes in this
	 * subset. The formula used to generate Ms was recommended on Breiman's website.
	 */
	public static int Ms;//recommended by Breiman: =(int)Math.round(Math.log(M)/Math.log(2)+1);
	
	/** the collection of the forest's decision trees */
	private ArrayList<DecisionTree> treeCollection;
	
	/** the starting time when timing random forest creation */
	private long time_o;
	
	/** the number of trees in this random tree */
	private int numTrees;
	
	/** For progress bar display for the creation of this random forest, this is the amount to update by when one tree is completed */
	private double update;
	
	/** For progress bar display for the creation of this random forest, this records the total progress */
	private double progress;
	
	/** this is an array whose indices represent the forest-wide importance for that given attribute */
	private int[] importances;
	
	/** This maps from a data record to an array that records the classifications by the trees where it was a "left out" record (the indices are the class and the values are the counts) */
	private HashMap<int[],int[]> estimateOOB;
	
	/** the total forest-wide error */
	private double error;
	
	/** the thread pool that controls the generation of the decision trees */
	private ExecutorService treePool;
	
	/** the original training data matrix that will be used to generate the random forest classifier */
	private ArrayList<ArrayList<String>> trainDataInstances;
	
	/** the data on which produced random forest will be tested*/
	private ArrayList<ArrayList<String>> testDataInstances;
	
	/** This holds all of the predictions of trees in a Forest */
	private ArrayList<ArrayList<String>> AllTreesPredictions;
	
	/** This holds the user specified layout for training data */
	public ArrayList<Character> DataAttributes;
	
	/** This holds all of the predictions of trees in a Forest Vs Actual Value */
	public HashMap<ArrayList<String>, String> FinalPredictions;
	
	/** This holds the number of correct predictions*/
	public int correctPredictions;
	

	// Initializes a Breiman random forest creation
	public RandomForest(ArrayList<Character> finalDataLayout, int numTrees, int numThreads, int numTotalFeatures, int numSelectedFeatures, int totalUniqueLabels, ArrayList<ArrayList<String>> trainDataInstances, ArrayList<ArrayList<String>> testDataList) 
	{
		// Start the clock to monitor forest generation time
		this.startTimer();
		
		// Set the local class level variables
		this.DataAttributes = finalDataLayout;
		this.numTrees = numTrees;
		this.NUM_THREADS = numThreads;
		this.M = numTotalFeatures;
		this.Ms = numSelectedFeatures;
		this.C = totalUniqueLabels;
		this.trainDataInstances = trainDataInstances;
		this.testDataInstances = testDataList;
		
		// Instantiate a collection of trees
		treeCollection = new ArrayList<DecisionTree>(numTrees);
		
		update= 100/((double)numTrees);
		progress=0;
		correctPredictions =0;
		System.out.println("Number of trees to be created in this random forest: "+ numTrees);
		System.out.println("Total training data size is: "+ trainDataInstances.size());
		System.out.println("Total number of features: "+ M);
		System.out.println("Total number of features to be selected: " + Ms);
		
		estimateOOB = new HashMap<int[],int[]>(trainDataInstances.size());
		AllTreesPredictions = new ArrayList<ArrayList<String>>();
		FinalPredictions = new HashMap<ArrayList<String>, String>();
		System.out.println("Forest initialized...");
	}
	
	
	/** This method creates a tree and add it to the collection */
	/*public DecisionTree generateTree() 
	{
		CreateTree obj = new CreateTree(trainDataInstances, this);
		DecisionTree tree = obj.run();
		return tree;
	}*/

	
	/** This method runs the test data against the training data */
	public void testTree(boolean forAccuracy,boolean withThreads) 
	{
		// Step 2- Testing for accuracy starts
		if(forAccuracy)
		{
			if(withThreads)
			{
				System.out.println("Testing Forest for Accuracy with threads...");
				testForAccuracyWithThread(treeCollection, trainDataInstances, testDataInstances);
			}
			else
			{
				System.out.println("Testing Forest for Accuracy without threads");
				testForAccuracyWithoutThread(treeCollection, trainDataInstances, testDataInstances);
			}
			
		}
		else
		{
			if(withThreads)
			{
				System.out.println("Testing Forest for Labels with threads");
				TestForestForLabelWT(treeCollection, trainDataInstances, testDataInstances);
			}
			else
			{
				System.out.println("Testing Forest for Labels without threads");
				TestForestForLabel(treeCollection, trainDataInstances, testDataInstances);
			}
		}
	}
	
	/** Test for accuracy with threads */
	private void testForAccuracyWithThread(
											ArrayList<DecisionTree> treeCollection, 
											ArrayList<ArrayList<String>> trainDataInstances, 
											ArrayList<ArrayList<String>> testDataInstances
											) 
	{
		long startTime = System.currentTimeMillis();
		ExecutorService TestthreadPool = Executors.newFixedThreadPool(NUM_THREADS);
		
		for(ArrayList<String> instance: testDataInstances)
		{
			TestthreadPool.execute(new TestTree(instance, treeCollection, trainDataInstances));
		}
		
		try
		{
			TestthreadPool.shutdown();
			TestthreadPool.awaitTermination(10, TimeUnit.SECONDS);
		}
		catch(InterruptedException ignored)
		{
			System.out.print("Interuption in testing");
		}
		
		System.out.println("Testing Complete..." );
		System.out.println("Forest Accuracy is: "+ ((correctPredictions*100)/testDataInstances.size())+"%");
		System.out.println("Time taken for tests: "+ TimeElapsed(startTime));
	}
	
	/** Test for accuracy without threads */
	public void testForAccuracyWithoutThread(ArrayList<DecisionTree> forest, ArrayList<ArrayList<String>> trainDataInstances, ArrayList<ArrayList<String>> testDataInstances)
	{
		long startTime = System.currentTimeMillis();
		int correctness=0;
		ArrayList<String> actualValues = new ArrayList<String>();
		
		for(ArrayList<String> instance : testDataInstances)
		{
			actualValues.add(instance.get(instance.size()-1));
		}
		
		System.out.println("Testing forest now ");
		
		for(DecisionTree tree : forest)
		{
			tree.runPredictions(trainDataInstances, testDataInstances);
			
			if(tree.predictions != null)
				AllTreesPredictions.add(tree.predictions);
		}
		
		// This method aggregates the prediction from all of the trees for each test instance and takes their mode.
		
		// Go through each instance
		for(int i = 0; i< testDataInstances.size(); i++)
		{
			ArrayList<String> allPredictions = new ArrayList<String>();
			
			// Go through each tree
			for(int j=0;j< forest.size(); j++)
			{
				allPredictions.add(AllTreesPredictions.get(j).get(i));
			}
			
			String predictedModeValue = ModeofList(allPredictions);
			
			FinalPredictions.put(testDataInstances.get(i), predictedModeValue);
			
			if(predictedModeValue.equalsIgnoreCase(actualValues.get(i)))
			{
				correctness = correctness +1;
			}
		}
		
		System.out.println("The Result of Predictions :-");
		System.out.println("Total Cases : "+ testDataInstances.size());
		System.out.println("Total CorrectPredicitions  : "+correctness);
		System.out.println("Forest Accuracy :"+(correctness*100/testDataInstances.size())+"%");	
		System.out.println("this test was done in "+TimeElapsed(startTime));
	}

	/**
	 * Predicting unlabeled data
	 * 
	 * @param trees22
	 * @param data2
	 * @param testdata2
	 */
	private void TestForestForLabel(ArrayList<DecisionTree> trees,ArrayList<ArrayList<String>> traindata,ArrayList<ArrayList<String>> testdata) {
		// TODO Auto-generated method stub
		long time = System.currentTimeMillis();

		System.out.println("Predicting Labels now");
		for(DecisionTree DTC : trees)
		{
			DTC.runPredictions(traindata, testdata);
			if(DTC.predictions!=null)
				AllTreesPredictions.add(DTC.predictions);
		}
		
		for(int i = 0;i<testdata.size();i++)
		{
			ArrayList<String> Val = new ArrayList<String>();
			for(int j=0;j<trees.size();j++){
				Val.add(AllTreesPredictions.get(j).get(i));
			}
			String pred = ModeofList(Val);
			System.out.println("["+pred+"]: Class predicted for data point: "+i+1);
		}
		System.out.println("this test was done in "+TimeElapsed(time));
	}
	
	/**
	 * Predicting unlabeled data with threads
	 * 
	 * @param tree
	 * @param traindata
	 * @param testdata
	 */
	private void TestForestForLabelWT(ArrayList<DecisionTree> tree,ArrayList<ArrayList<String>> traindata,ArrayList<ArrayList<String>> testdata) {
		long time = System.currentTimeMillis();
		ExecutorService TestthreadPool = Executors.newFixedThreadPool(NUM_THREADS);int i=1;
		for(ArrayList<String> TP:testdata){
			TestthreadPool.execute(new TestTreeforLabel(TP,tree,traindata,i));i++;
		}TestthreadPool.shutdown();
		try{
			TestthreadPool.awaitTermination(10, TimeUnit.SECONDS);
		}catch(InterruptedException ignored){
			System.out.print("Interuption in testing");
		}
		System.out.println("Testing Complete");
		System.out.println("this test was done in "+TimeElapsed(time));
	}
	
	
	
	
	/** It takes majority vote amongst all the predictions. 
	 * Note: I have changed the logic of calculating mode to use hash map. */
	public String ModeofList(ArrayList<String> predictions) 
	{
		String MaxValue = null; 
		int MaxCount = 0;
		HashMap<String, Integer> freqCount = new HashMap<String, Integer>();
		
		for(String prediction : predictions)
		{
			if(freqCount.containsKey(prediction))
				freqCount.put(prediction, freqCount.get(prediction)+1);
			else
				freqCount.put(prediction, 1);
		}
		
		
		
		for(String prediction : freqCount.keySet())
		{
			int count = freqCount.get(prediction);
			if(count>MaxCount)
			{
				MaxValue=prediction;
				MaxCount=count;
			}
		}
		
		return MaxValue;
	}
	
	/**
	 * This class houses the machinery to generate one decision tree in a thread pool environment.
	 *
	 */
	private class CreateTree
	{
		/** the training data to generate the decision tree (same for all trees) */
		private ArrayList<ArrayList<String>> trainDataInstances;
	
		/** the current forest */
		private RandomForest forest;
		
		
		public CreateTree(ArrayList<ArrayList<String>> trainDataInstances)
		{
			this.trainDataInstances = trainDataInstances;
		}
		
		/** Creates the decision tree */
		/*public DecisionTree run() 
		{
			// Mapper
			DecisionTree tree = new DecisionTree(trainDataInstances, forest);
			
			// Reducer
			// treeCollection.add(tree);
			
			progress = progress + update;
			return tree;
		}*/
	}
	
	/**
	 * This class houses the machinery to test decision trees in a thread pool environment.
	 *
	 */
	public class TestTree implements Runnable
	{
		public ArrayList<String> instance;
		public ArrayList<DecisionTree> treeCollection;
		public ArrayList<ArrayList<String>> trainedDataInstances;
		
		public TestTree(ArrayList<String> instance, ArrayList<DecisionTree> treeCollection, ArrayList<ArrayList<String>> trainDataInstances)
		{
			this.instance = instance;
			this.treeCollection = treeCollection;
			this.trainedDataInstances = trainDataInstances;
		}

		@Override
		public void run() 
		{
			ArrayList<String> predictions = new ArrayList<String>();
			
			/** For each tree, check the status of instance in trained Instances. Based on the result, build prediction gotten from each tree.*/
			for(DecisionTree tree : treeCollection)
			{
				String Class = tree.Evaluate(instance, trainedDataInstances);
				
				if(Class == null)
					predictions.add("n/a");
				else
					predictions.add(Class);
			}
			
			// Take the vote from all the trees
			String finalClass = ModeofList(predictions);
			
			// Check if it predicts correctly or not. This will be used to calculate accuracy.
			if(finalClass!= null && finalClass.equalsIgnoreCase(instance.get(M)))
				correctPredictions++;
			
			
			
			FinalPredictions.put(instance, finalClass);
		}
	}
	
	public HashMap<ArrayList<String>, String> getFinalPrediction()
	{
		return FinalPredictions;
	}
	
	/**
	 * This class houses the machinery to predict class from decision trees in a thread pool environment.
	 *
	 */
	public class TestTreeforLabel implements Runnable{
		
		public ArrayList<String> testrecord;
		public ArrayList<DecisionTree> Trees;
		public ArrayList<ArrayList<String>> trainData;
		public int point;
		
		public TestTreeforLabel(ArrayList<String> dp, ArrayList<DecisionTree> dtree, ArrayList<ArrayList<String>> data,int i){
			this.testrecord = dp;
			this.Trees = dtree;
			this.trainData = data;
			this.point =i;
		}

		@Override
		public void run() {
			ArrayList<String> predictions = new ArrayList<String>();
			
			for(DecisionTree DT:Trees){
				String Class = DT.Evaluate(testrecord, trainData);
				if(Class == null)
					predictions.add("n/a");
				else
					predictions.add(Class);
			}
			
			String finalClass = ModeofList(predictions);
			System.out.println("["+finalClass+"]: Class predicted for data point: "+point);
		}
	}
	
	/** Start the timer when beginning forest creation */
	private void startTimer()
	{
		time_o = System.currentTimeMillis();
	}
	
	/**
	 * Given a certain time that's elapsed, return a string
	 * representation of that time in hr,min,s
	 * 
	 * @param timeinms	the beginning time in milliseconds
	 * @return			the hr,min,s formatted string representation of the time
	 */
	private static String TimeElapsed(long timeinms)
	{
		double s=(double)(System.currentTimeMillis()-timeinms)/1000;
		int h = (int)Math.floor(s/((double)3600));
		s-=(h*3600);
		int m = (int)Math.floor(s/((double)60));
		s-=(m*60);
		return ""+h+"hr "+m+"m "+s+"sec";
	}

}
