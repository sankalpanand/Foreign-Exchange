package Assignment4;
// http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;

public class DecisionTree {
	/** Instead of checking each index we'll skip every INDEX_SKIP indices unless there's less than MIN_SIZE_TO_CHECK_EACH*/
	private static final int INDEX_SKIP=3;

	/** If there's less than MIN_SIZE_TO_CHECK_EACH points, we'll check each one */
	private static final int MIN_SIZE_TO_CHECK_EACH=10;

	/** If the number of data points is less than MIN_NODE_SIZE, we won't continue splitting, we'll take the majority vote */
	private static final int MIN_NODE_SIZE=5;

	/** the number of data records */
	private int numOfTrainingInstances;


	/** This keeps track of all the predictions done by this tree */
	public ArrayList<String> predictions;

	/** This is the root of the Decision Tree */
	private TreeNode root;

	/** This is a pointer to the Random Forest this decision tree belongs to */
	private RandomForest forest;

	/**
	 * This constructs a decision tree from a data matrix.
	 * It first creates a bootstrap sample, the train data matrix, as well as the left out records, 
	 * the test data matrix. Then it creates the tree, then calculates the variable importances (not essential)
	 * and then removes the links to the actual data (to save memory)
	 * 
	 * @param data		The data matrix as a List of int arrays - each array is one record, each index in the array is one attribute, and the last index is the class
	 * 					(ie [ x1, x2, . . ., xM, Y ]).
	 * @param forest	The random forest this decision tree belongs to
	 */
	public DecisionTree(ArrayList<ArrayList<String>> originalTrainingData, RandomForest forest, int treeNum) 
	{
		this.forest = forest;
		numOfTrainingInstances = originalTrainingData.size();

		ArrayList<ArrayList<String>> sampledTrainingData = new ArrayList<ArrayList<String>>(numOfTrainingInstances);
		ArrayList<ArrayList<String>> leftOutData = new ArrayList<ArrayList<String>>();

		// Step 1- Generate sampled data
		BootStrapSample(originalTrainingData, sampledTrainingData, leftOutData, treeNum);

		// Step 2- Generate tree from that sampled data
		root = CreateTree(sampledTrainingData, treeNum);
		FlushData(root, treeNum);
	}

	/**
	 * Create a boostrap sample of a data matrix
	 * 
	 * @param originalTrainingData		the data matrix to be sampled
	 * @param selectedTrainingData		the bootstrap sample
	 * @param testingData				the records that are absent in the bootstrap sample
	 */
	@SuppressWarnings("unchecked")
	private void BootStrapSample(
			ArrayList<ArrayList<String>> originalTrainingData, 
			ArrayList<ArrayList<String>> sampledTrainingData, 
			ArrayList<ArrayList<String>> leftOutData, 
			int treeNum)
	{
		ArrayList<Integer> sampleIndices = new ArrayList<Integer>();

		// Step 1- Generate N sample indices
		for (int n=0; n<numOfTrainingInstances; n++)
		{
			int index = (int)Math.floor(Math.random()*numOfTrainingInstances); 
			sampleIndices.add(index);
		}

		ArrayList<Boolean> isSelected = new ArrayList<Boolean>();
		for (int n=0; n<numOfTrainingInstances; n++)
			isSelected.add(false); //have to initialize it first

		// Step 2- Pick samples from the N random generated indices 
		for (int index : sampleIndices)
		{
			ArrayList<String> sample = originalTrainingData.get(index);
			sampledTrainingData.add((ArrayList<String>) sample.clone());
			isSelected.set(index, true);
		}

		// Step 3- Pick those indexes which were not selected. We will use it to test the error%.
		for (int i=0; i<numOfTrainingInstances; i++)
			if (!isSelected.get(i))
				leftOutData.add(originalTrainingData.get(i));

		// Testing data populated
	}

	/** This creates the decision tree according to the specifications of random forest trees.  */
	private TreeNode CreateTree(ArrayList<ArrayList<String>> sampledTrainingData, int treeNum)
	{
		TreeNode root = new TreeNode();
		root.label = "|ROOT|";
		root.data = sampledTrainingData;
		RecursiveSplit(root, treeNum);
		return root;
	}

	
	private void RecursiveSplit(TreeNode parent, int treeNum)
	{
		if (!parent.isLeaf)
		{
			/** Step 1- While creating a decision tree, 
			if we have come to a point where all the instances have the same value for all the labels, 
			it means we have reached a leadf and there is no need to split further.
			Check if this node is a leaf. If so, mark isLeaf=true and Class=leaf's class. 
			The function will not recurse past this point.
			 */
			String Class = CheckIfLeaf(parent.data);
			if (Class != null)
			{
				parent.isLeaf = true;
				parent.Class = Class;
				return;
			}

			// Step 2.1- Instantiate child nodes for this root
			parent.ChildNode = new ArrayList<DecisionTree.TreeNode>();
			for(TreeNode node : parent.ChildNode)
			{
				node = new TreeNode();
				node.generation = parent.generation + 1;
			}

			// Step 2.2- Get randomly Ms selected attributes out of M attributes 
			ArrayList<Integer> selectedFeatures = GetVarsToInclude();
			
			


			/* Step 3- 
			 * For all Ms variables, first sort the data records by that attribute, then look through the values from lowest to highest.
			 * If value i is not equal to value i+1, put record i in the list of "indicesToCheck." This speeds up the splitting. 
			 * If the number of indices in indicesToCheck >  MIN_SIZE_TO_CHECK_EACH, then we will only check the entropy at every INDEX_SKIP index 
			 * otherwise, we check the entropy for all. The lowestEntropy variable records the entropy and we are trying to find the minimum in which to split on
			 */
			
			int totalInstancesAtThisNode = parent.data.size();
			
			//-------------------------------Step C
			for (int m : selectedFeatures)
			{
				// which data points to be scrutinized
				ArrayList<Integer> DataPointToCheck = new ArrayList<Integer>(); 

				// Sorts on a particular column in the row
				SortAtAttribute(parent.data, m);
				
				for (int n=1; n<totalInstancesAtThisNode; n++)
				{
					String classA = getLabel(parent.data.get(n-1));
					String classB = getLabel(parent.data.get(n));
					if(!classA.equalsIgnoreCase(classB))
						DataPointToCheck.add(n);
				}

				// If all the instances at this node have the same value, then label its class directly
				if (DataPointToCheck.size() == 0)
				{
					parent.isLeaf=true;
					parent.Class = getLabel(parent.data.get(0));
					continue;
				}

				// Since the data points are different, we need to pick one among them. So, check entropy.
				DoubleWrap lowestEntropy = new DoubleWrap(Double.MAX_VALUE);
				
				if (DataPointToCheck.size() > MIN_SIZE_TO_CHECK_EACH)
				{
					// Since there too many instances with different values, instead of checking each index we'll skip every INDEX_SKIP 
					for (int i=0; i<DataPointToCheck.size(); i+=INDEX_SKIP)
					{
						CheckPosition(m, DataPointToCheck.get(i), totalInstancesAtThisNode, lowestEntropy, parent, treeNum);
						
						// If entropy has become zero, it means max IG is here. So break the loop
						if (lowestEntropy.value == 0) 
							break;
					}
				}
				
				// If there's less than MIN_SIZE_TO_CHECK_EACH points, we'll check each one 
				else
				{
					for (int k : DataPointToCheck)
					{
						CheckPosition(m, k, totalInstancesAtThisNode, lowestEntropy, parent, treeNum);
						
						// If entropy has become zero, it means max IG is here. So break the loop
						if (lowestEntropy.value == 0) 
							break;
					}
				}
				
				// For this feature, minimum entropy has been reached. We got our feature. Break out of this loop.
				if (lowestEntropy.value == 0)
					break;
			}
			
			/**
			 Step D- 
			 * The newly generated left and right nodes are now checked:
			 * If the node has only one record, we mark it as a leaf and set its class equal to the class of the record. 
			 * If it has less than MIN_NODE_SIZE records, then we mark it as a leaf and set its class equal to the majority class.
			 * If it has more, then we do a manual check on its data records and if all have the same class, then it is marked as a leaf. 
			 * If not, then we run RecursiveSplit on that node
			 */
			for(TreeNode child : parent.ChildNode)
			{
				if(child.data.size() == 1)
				{
					child.isLeaf=true;
					child.Class=getLabel(child.data.get(0));
				}
				
				else if(child.data.size()<MIN_NODE_SIZE)
				{
					child.isLeaf=true;
					child.Class = GetMajorityClass(child.data);
				}
				
				else
				{
					Class = CheckIfLeaf(child.data);
					
					// Y-values for child's data are different. So, it's not a leaf.
					if(Class==null)
					{
						child.isLeaf=false;
						child.Class=null;
					}
					
					// Y-values for child's data are same. So, it's a leaf.
					else
					{
						child.isLeaf=true;
						child.Class=Class;
					}
				}
				
				// If the child was found not to be a leaf, then split again on it.
				if(!child.isLeaf)
				{
					RecursiveSplit(child, treeNum);
				}
			}
		}
	}

	/** Given a data matrix, check if all the y values are the same. If not, return null otherwise return that same value. */
	private String CheckIfLeaf(ArrayList<ArrayList<String>> data)
	{
		// Get the first class
		String classOne = getLabel(data.get(0));

		// Match it with the rest
		for(ArrayList<String> record : data)
		{
			String remainingClasses = getLabel(record);
			if(! classOne.equalsIgnoreCase(remainingClasses))
			{
				return null;
			}
		}

		return classOne;
	}

	/** Returns the label (Y value) for an instance */
	public static String getLabel(ArrayList<String> record)
	{
		return record.get(RandomForest.M).trim();
	}

	public class TreeNode implements Cloneable
	{
		public boolean isLeaf;
		public ArrayList<TreeNode> ChildNode ;
		public HashMap<String, String> Missingdata;

		/** The attribute on which it will be splitted */
		public int splitAttributeM;

		public boolean spiltonCateg;
		public String Class;
		public ArrayList<ArrayList<String>> data;

		// Check this if it return false on splitonCateg
		public String splitValue;

		// Label of each node
		public String label;
		public int generation;

		public TreeNode()
		{
			splitAttributeM=-99;
			splitValue="-99";
			generation=1;
		}

		// "data" element always null in clone
		public TreeNode clone()
		{ 
			TreeNode copy=new TreeNode();
			copy.isLeaf=isLeaf;
			for(TreeNode TN : ChildNode)
			{
				if(TN != null)
				{
					copy.ChildNode.add(TN.clone());
				}
			}
			//			if (left != null) //otherwise null
			//				copy.left=left.clone();
			//			if (right != null) //otherwise null
			//				copy.right=right.clone();
			copy.splitAttributeM=splitAttributeM;
			copy.Class=Class;
			copy.splitValue=splitValue;
			copy.spiltonCateg = spiltonCateg;
			copy.label=label;
			return copy;
		}
	}
	
	/** Of the M attributes, select Ms at random. */
	private ArrayList<Integer> GetVarsToInclude() 
	{
		boolean[] whichVarsToInclude = new boolean[RandomForest.M];

		while(true)
		{
			// Keep generating random indexes until their count reaches Ms 
			int randomVal = (int) Math.floor(Math.random() * RandomForest.M);
			whichVarsToInclude[randomVal]=true;
			
			int N=0;
			for (int i=0; i<RandomForest.M; i++)
				if (whichVarsToInclude[i])
					N++;
			
			if (N == RandomForest.Ms)
				break;
		} // Ms random indexes generated.

		// Add those Ms indexes which you just generated
		ArrayList<Integer> shortRecord = new ArrayList<Integer>(RandomForest.Ms);

		for (int i=0; i<RandomForest.M; i++)
			if (whichVarsToInclude[i])
				shortRecord.add(i);
		
		// Return those Ms indexes which you just generated
		return shortRecord;
	}


	/** hold the entropy */
	private class DoubleWrap
	{
		public double value;
		
		public DoubleWrap(double d)
		{
			this.value = d;
		}		
	}

	/** This method will get the classes and will return the updates */
	public ArrayList<String> CalculateClasses(ArrayList<ArrayList<String>> traindata,ArrayList<ArrayList<String>> testdata, int treenumber){
		ArrayList<String> predicts = new ArrayList<String>();
		for(ArrayList<String> record:testdata){
			String Clas = Evaluate(record, traindata);
			if(Clas==null){
				predicts.add("n/a");
			}
			else
				predicts.add(Clas);
		}
		predictions = predicts;
		return predicts;
	}
	/**
	 * Evaluates each record and traverses through the tree and returns the predictions
	 * 
	 * @param Each record the the data
	 * @return he predicted class 
	 * 
	 */
	public String Evaluate(ArrayList<String> testInstance, ArrayList<ArrayList<String>> trainedDataInstances)
	{
		// Store the root
		TreeNode evalNode = root;		

		while (true) 
		{
			// BASE CASE: Leaf is reached. Return the node class.
			if(evalNode.isLeaf)
				return evalNode.Class;
			else
			{
				// CATEGORICAL: If its categorical
				if(evalNode.spiltonCateg)
				{
					// Get the value of the splitted attribute for this instance
					String splitAttributeM = testInstance.get(evalNode.splitAttributeM);

					boolean found = false;
					String Res = evalNode.Missingdata.get(getLabel(testInstance));

					// For each child, check if any child has the same label as attribute splittid on
					for(TreeNode child : evalNode.ChildNode)
					{
						// If it is there, break out of this loop and evaluate on this child node
						if(splitAttributeM.equalsIgnoreCase(child.label))
						{
							evalNode = child;
							found = true;
							break;
						}
					}

					// If the category is not present at all, check if the res matches any child label 
					if(!found)
					{
						for(TreeNode child : evalNode.ChildNode)
						{
							if(Res!=null)
							{
								if(Res.trim().equalsIgnoreCase(child.label))
								{
									evalNode = child;
									break;
								}
							}
							else
							{
								return "n/a";
							}
						}
					}
				}

				// REAL VALUED: if eval node is split on real-valued
				else
				{
					double splitAtAttributeValue = Double.parseDouble(evalNode.splitValue);
					double actualInstanceValue = Double.parseDouble(testInstance.get(evalNode.splitAttributeM));

					// If the value to be checked is greater, go to left child
					if(actualInstanceValue <= splitAtAttributeValue)
					{
						if(evalNode.ChildNode.get(0).label.equalsIgnoreCase("Left"))
							evalNode=evalNode.ChildNode.get(0);
						else
							evalNode=evalNode.ChildNode.get(1);
					}

					// If the value to be checked is smaller, go to right child
					else
					{
						if(evalNode.ChildNode.get(0).label.equalsIgnoreCase("Right"))
							evalNode=evalNode.ChildNode.get(0);
						else
							evalNode=evalNode.ChildNode.get(1);
					}
				}
			}
		} // End of while
	}
	



	/**
	 * Sorts a data matrix by an attribute from lowest record to highest record
	 * 
	 * @param data			the data matrix to be sorted
	 * @param m				the attribute to sort on
	 */
	private void SortAtAttribute(ArrayList<ArrayList<String>> data, int m) {
		if(forest.DataAttributes.get(m) == 'C')
			System.out.print("");//Collections.sort(data,new AttributeComparatorCateg(m));
		else
			Collections.sort(data,new AttributeComparatorReal(m));

	}


	/** This class compares two data records by numerically/categorically comparing a specified attribute */
	private class AttributeComparatorReal implements Comparator<ArrayList<String>>
	{		
		/** the specified attribute */
		private int m;
		/**
		 * Create a new comparator
		 * @param m			the attribute in which to compare on
		 */
		public AttributeComparatorReal(int m){
			this.m=m;
		}
		/**
		 * Compare the two data records. They must be of type int[].
		 * 
		 * @param arg1		data record A
		 * @param arg2		data record B
		 * @return			-1 if A[m] < B[m], 1 if A[m] > B[m], 0 if equal
		 */
		@Override
		public int compare(ArrayList<String> arg1, ArrayList<String> arg2) {//compare value of strings
			double a2 = Double.parseDouble(arg1.get(m));
			double b2 = Double.parseDouble(arg2.get(m));
			if(a2<b2)
				return -1;
			else if(a2>b2)
				return 1;
			else
				return 0;
		}		
	}

	/** Given a data matrix, return the most popular Y value (the class) */
	private String GetMajorityClass(ArrayList<ArrayList<String>> instances)
	{
		// Store all the Y-values in a list
		ArrayList<String> ToFind = new ArrayList<String>();
		for(ArrayList<String> instance : instances)
		{
			ToFind.add(instance.get(instance.size()-1));
		}
		
		// Start counting their frequency
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		String MaxValue = null; 
		int MaxCount = 0;
		
		for(String s : ToFind)
		{
			if(map.containsKey(s))
				map.put(s, map.get(s)+1);
			else
				map.put(s, 1);
		}
		
		// Get the Y-value with maximum occurrance
		for(String s : map.keySet())
		{
			if(map.get(s) > MaxCount)
			{
				MaxValue = s;
				MaxCount = map.get(s);
			}
		}
		
		return MaxValue;
	}
	/**
	 * Checks the entropy of an index in a data matrix at a particular attribute (m) and returns the entropy. 
	 * If the entropy is lower than the minimum to date (lowestE), it is set to the minimum.
	 *  
	 * If the attribute is real-valued then :  
	 *	 The total entropy is calculated by getting the sub-entropy for below the split point and after the split point.
	 * 	The sub-entropy is calculated by first getting the {@link #getLabelProbs(List) proportion} of each of the classes
	 *	 in this sub-data matrix. Then the entropy is {@link #CalcEntropy(double[]) calculated}. The lower sub-entropy
	 *	 and upper sub-entropy are then weight averaged to obtain the total entropy.
	 * 
	 * If the attribute is categorical-valued then :
	 *	The total entropy is calculated by entropy of the each of categorical-value in the attribute
	 *	The split is done by recursively calling split over each of the categorical value;
	 * 
	 * @param m				the attribute to split on
	 * @param n				the index to check
	 * @param Nsub			the number of records in the data matrix
	 * @param lowestE		the minimum entropy to date
	 * @param parent		the parent node
	 * @return				the entropy of this split
	 */
	private double CheckPosition(int splitOnThisFeature, int n, int totalInstancesAtThisNode, DoubleWrap lowestE, TreeNode parent, int nTre)
	{

		double entropy =0;

		if (n < 1 || n > totalInstancesAtThisNode) //exit conditions
			return 0;
		

		// CATEGORICAL
		if(forest.DataAttributes.get(splitOnThisFeature)=='C')
		{
			// find out the distinct values in that attribute...from parent.data
			ArrayList<String> uniqueFeature = new ArrayList<String>(); // unique categories
			ArrayList<String> uniqueClasses = new ArrayList<String>(); // unique classes
			
			HashMap<String, Integer> ChilFreq = new HashMap<String, Integer>();// Node-Label Vs frequency

			// Get all the unique labels and selected feature in this parent's data instances
			for(ArrayList<String> s : parent.data)
			{
				String selectedFeatureForThis = s.get(splitOnThisFeature).trim();
				if(!uniqueFeature.contains(selectedFeatureForThis))
				{
					uniqueFeature.add(selectedFeatureForThis);
					ChilFreq.put(selectedFeatureForThis, 0);
				}

				if(!uniqueClasses.contains(getLabel(s)))
					uniqueClasses.add(getLabel(s));
			}

			// For each unique feature, separate all those instances that have that unique feature
			HashMap<String, ArrayList<ArrayList<String>>> ChildDataMap = new HashMap<String, ArrayList<ArrayList<String>>>();
			
			for(String feature : uniqueFeature)
			{
				ArrayList<ArrayList<String>> child_data = new ArrayList<ArrayList<String>>();
				
				for(ArrayList<String> instance : parent.data)
				{
					if(feature.trim().equalsIgnoreCase(instance.get(splitOnThisFeature).trim()))
						child_data.add(instance);
				}
				ChildDataMap.put(feature, child_data);
			}

			//Adding missing-data-suits
			HashMap<String, String> ChildMissingMap = new HashMap<String, String>();// Class Vs Node-label
			for(String Class : uniqueClasses)
			{
				int max=0;
				String maxFoundFeature = null;
				
				for(ArrayList<String> instance : parent.data)
				{
					// If this unique class equals this instance's class, increase its count
					String thisFeature = instance.get(splitOnThisFeature);
					
					if(getLabel(instance).equalsIgnoreCase(Class))
					{
						if(ChilFreq.containsKey(thisFeature))
							ChilFreq.put(thisFeature, ChilFreq.get(thisFeature)+1);
					}
					
					// Keep track of the class which has most instances
					if(ChilFreq.get(thisFeature) > max)
					{
						max=ChilFreq.get(thisFeature);
						maxFoundFeature = thisFeature;
					}
				}
				
				// It keeps track of each unique class had which of the most found feature
				ChildMissingMap.put(Class, maxFoundFeature);
			}
			
			// Calculate Entropy
			// This map contains all unique features and all those instances that have that unique feature
			for(Entry<String, ArrayList<ArrayList<String>>> entry : ChildDataMap.entrySet())
			{
				ArrayList<ArrayList<String>> instances = entry.getValue();
				ArrayList<Double> probabilities = getLabelProbabilities(instances);
				entropy = entropy + calculateEntropy(probabilities) * instances.size();
			}
			
			entropy = entropy/((double)totalInstancesAtThisNode);
			
			//if its the least...
			if (entropy < lowestE.value)
			{
				lowestE.value =entropy;
				parent.splitAttributeM=splitOnThisFeature;
				parent.spiltonCateg = true;
				parent.splitValue = parent.data.get(n).get(splitOnThisFeature);
				parent.Missingdata= ChildMissingMap;
				
				// Generating children
				ArrayList<TreeNode> Children = new ArrayList<TreeNode>();
				for(Entry<String,ArrayList<ArrayList<String>>> entry : ChildDataMap.entrySet())
				{
					TreeNode Child = new TreeNode();
					Child.data = entry.getValue();
					Child.label = entry.getKey();
					Children.add(Child);
				}
				parent.ChildNode=Children;
			}
		}
		
		else
		{

			// this is a real valued thing

			HashMap<String, ArrayList<ArrayList<String>>> UpLo = GetUpperLower(parent.data, n, splitOnThisFeature);

			ArrayList<ArrayList<String>> lower = UpLo.get("lower"); 
			ArrayList<ArrayList<String>> upper = UpLo.get("upper"); 

			ArrayList<Double> pl=getLabelProbabilities(lower);
			ArrayList<Double> pu=getLabelProbabilities(upper);
			double eL=calculateEntropy(pl);
			double eU=calculateEntropy(pu);

			entropy =(eL*lower.size()+eU*upper.size())/((double)totalInstancesAtThisNode);

			if (entropy < lowestE.value){
				lowestE.value = entropy;
				parent.splitAttributeM=splitOnThisFeature;
				parent.spiltonCateg=false;
				parent.splitValue = parent.data.get(n).get(splitOnThisFeature).trim();
				/**
				 * Adding Data to Left/Right Child
				 * 
				 */
				ArrayList<TreeNode> Children2 = new ArrayList<TreeNode>();
				TreeNode Child_left = new TreeNode();
				Child_left.data=lower;
				Child_left.label="Left";
				Children2.add(Child_left);
				TreeNode Child_Right = new TreeNode();
				Child_Right.data=upper;
				Child_Right.label="Right";
				Children2.add(Child_Right);
				parent.ChildNode=Children2;//clone karo....
			}
		}
		return entropy;
	}

	/**
	 * Returns lower and upper data for paret.data
	 * 
	 * @param data	parent data
	 * @param n2	data point
	 * @param m		attribute value
	 * @return		map of upper and lower
	 */
	private HashMap<String, ArrayList<ArrayList<String>>> GetUpperLower(ArrayList<ArrayList<String>> data, int n2,int m) {

		HashMap<String, ArrayList<ArrayList<String>>> UpperLower = new HashMap<String, ArrayList<ArrayList<String>>>();
		ArrayList<ArrayList<String>> lowerr = new ArrayList<ArrayList<String>>(); 
		ArrayList<ArrayList<String>> upperr = new ArrayList<ArrayList<String>>(); 
		for(int n=0;n<n2;n++)
			lowerr.add(data.get(n));
		for(int n=n2;n<data.size();n++)
			upperr.add(data.get(n));
		UpperLower.put("lower", lowerr);
		UpperLower.put("upper", upperr);

		return UpperLower;
	}

	/**
	 * Given a data matrix, return a probabilty mass function representing the frequencies of a class in the matrix (the y values)
	 */
	private ArrayList<Double> getLabelProbabilities(ArrayList<ArrayList<String>> record)
	{
		double N = record.size();
		HashMap<String, Integer> counts = new HashMap<String, Integer>();
		
		// Generate a count for how many times a class appears in the instances
		for(ArrayList<String> s : record)
		{
			String clas = getLabel(s);
			if(counts.containsKey(clas))
				counts.put(clas, counts.get(clas)+1);
			else
				counts.put(clas, 1);
		}
		
		// Calculate probability by dividing count/size
		ArrayList<Double> probs = new ArrayList<Double>();		
		for(Entry<String, Integer> entry : counts.entrySet())
		{
			double prob = entry.getValue()/N;
			probs.add(prob);
		}
		
		return probs;
	}

	/**
	 *  ln(2)   
	 */
	private static final double logoftwo=Math.log(2);

	/**
	 * Given a probability mass function indicating the frequencies of class representation, calculate an "entropy" value using the method
	 * in Tan|Steinbach|Kumar's "Data Mining" textbook
	 */
	private double calculateEntropy(ArrayList<Double> ps)
	{
		double e=0;		
		for (double p:ps)
		{
			// otherwise it will divide by zero - see TSK p159
			if (p != 0) 
				e+=p*Math.log(p)/logoftwo;
		}
		return -e; //according to TSK p158
	}

	




	/**
	 * Recursively deletes all data records from the tree. This is run after the tree
	 * has been computed and can stand alone to classify incoming data.
	 * 
	 * @param node		initially, the root node of the tree
	 * @param treenum 
	 */
	private void FlushData(TreeNode node, int treenum){
		node.data=null;
		if(node.ChildNode!=null){
			for(TreeNode TN : node.ChildNode){
				if(TN != null)
					FlushData(TN,treenum);
			}
		}
	}

}
