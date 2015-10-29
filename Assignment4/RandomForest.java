

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
	private ArrayList<ArrayList<String>> Prediction;
	
	/** This holds the user specified layout for training data */
	public ArrayList<Character> DataAttributes;
	
	/** This holds all of the predictions of trees in a Forest Vs Actual Value */
	public HashMap<ArrayList<String>, String> FinalPredictions;
	
	/** This holds the number of correct predictions*/
	public int correctPredictions;
	

	// Initializes a Breiman random forest creation
	public RandomForest(ArrayList<Character> finalDataLayout, int numTrees,int numThreads, int numTotalFeatures, int numSelectedFeatures, int totalUniqueLabels, ArrayList<ArrayList<String>> trainDataInstances, ArrayList<ArrayList<String>> testDataList) 
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
		Prediction = new ArrayList<ArrayList<String>>();
		FinalPredictions = new HashMap<ArrayList<String>, String>();
		System.out.println("Forest initialized...");
	}
	
	/**
	 * Begins the random forest creation
	 */
	public void Start(boolean forAccuracy,boolean withThreads) 
	{
		System.out.println("Forest generation starting...");
		System.out.println("Number of threads started :"+ NUM_THREADS);
		
		
		// Step 1- Create trees in different thread pools
		treePool = Executors.newFixedThreadPool(NUM_THREADS);
		for (int t=0; t<numTrees; t++)
		{
			treePool.execute(new CreateTree(trainDataInstances, this, t+1));
		}		
		
		
		// Wait for thread pool termination
		try 
		{	
			treePool.shutdown();
			treePool.awaitTermination(10,TimeUnit.SECONDS); //effectively infinity
	    } 
		catch (InterruptedException ignored)
		{
	    	System.out.println("interrupted exception in Random Forests");
	    }
		
		System.out.println("Trees generation completed in " + TimeElapsed(time_o) + "\n");
		
		// Step 2- Testing for accuracy starts
		if(forAccuracy)
		{
			if(withThreads)
			{
				System.out.println("Testing Forest for Accuracy with threads...");
				ArrayList<DecisionTree> Tree1 = (ArrayList<DecisionTree>) treeCollection.clone();
				testForAccuracyWithThread(Tree1, trainDataInstances, testDataInstances);
			}
			else
			{
				System.out.println("Testing Forest for Accuracy without threads");
				ArrayList<DecisionTree> Tree2 = (ArrayList<DecisionTree>) treeCollection.clone();
				testForAccuracyWithoutThread(Tree2, trainDataInstances, testDataInstances);
			}
			
		}
		else
		{
			if(withThreads)
			{
				System.out.println("Testing Forest for Labels with threads");
				ArrayList<DecisionTree> Tree3 = (ArrayList<DecisionTree>) treeCollection.clone();
				TestForestForLabelWT(Tree3, trainDataInstances, testDataInstances);
			}
			else
			{
				System.out.println("Testing Forest for Labels without threads");
				ArrayList<DecisionTree> Tree4 = (ArrayList<DecisionTree>) treeCollection.clone();
				TestForestForLabel(Tree4, trainDataInstances, testDataInstances);
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
	public void testForAccuracyWithoutThread(ArrayList<DecisionTree> trees,ArrayList<ArrayList<String>> trainDataInstances, ArrayList<ArrayList<String>> testDataInstances)
	{
		long startTime = System.currentTimeMillis();
		int correctness=0;
		ArrayList<String> ActualValues = new ArrayList<String>();
		
		for(ArrayList<String> instance : testDataInstances)
		{
			ActualValues.add(instance.get(instance.size()-1));
		}
		
		int treee=1;
		System.out.println("Testing forest now ");
		
		for(DecisionTree DTC : trees)
		{
			DTC.CalculateClasses(trainDataInstances, testDataInstances, treee);
			treee++;
			if(DTC.predictions!=null)
				Prediction.add(DTC.predictions);
		}
		
		for(int i = 0;i< testDataInstances.size();i++)
		{
			ArrayList<String> Val = new ArrayList<String>();
			for(int j=0;j<trees.size();j++)
			{
				Val.add(Prediction.get(j).get(i));
			}
			String pred = ModeofList(Val);
			if(pred.equalsIgnoreCase(ActualValues.get(i)))
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
		int treee=1;
		System.out.println("Predicting Labels now");
		for(DecisionTree DTC : trees){
			DTC.CalculateClasses(traindata, testdata, treee);treee++;
			if(DTC.predictions!=null)
			Prediction.add(DTC.predictions);
		}
		for(int i = 0;i<testdata.size();i++){
			ArrayList<String> Val = new ArrayList<String>();
			for(int j=0;j<trees.size();j++){
				Val.add(Prediction.get(j).get(i));
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
	private class CreateTree implements Runnable
	{
		/** the training data to generate the decision tree (same for all trees) */
		private ArrayList<ArrayList<String>> trainDataInstances;
	
		/** the current forest */
		private RandomForest forest;
		
		/** the Tree number */
		private int treeNum;
		
		public CreateTree(ArrayList<ArrayList<String>> trainDataInstances, RandomForest forest, int treeNum)
		{
			this.trainDataInstances = trainDataInstances;
			this.forest = forest;
			this.treeNum = treeNum;
		}
		
		/** Creates the decision tree */
		public void run() 
		{
			treeCollection.add(new DecisionTree(trainDataInstances, forest, treeNum));
			progress = progress + update;
		}
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
			if(finalClass.equalsIgnoreCase(instance.get(M)))
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
