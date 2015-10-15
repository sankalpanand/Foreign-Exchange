package Assignment3;

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

	// The number of instances in this tree
	private int trainDataSize;

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
	public DecisionTree(ArrayList<ArrayList<String>> data, RandomForest forest, int treenum) 
	{
		// Attach this decision tree to the given forest
		this.forest=forest;

		// The number of instances in this tree
		trainDataSize = data.size();


		ArrayList<ArrayList<String>> training = new ArrayList<ArrayList<String>>(trainDataSize);
		ArrayList<ArrayList<String>> test = new ArrayList<ArrayList<String>>();

		// This method will take the instance data in "data". 
		// It will randomly select some of it and put it in train and the rest in test.
		BootStrapSample(data, training, test, treenum);


		// Generate the tree from training set
		root = CreateTree(training, treenum);


		FlushData(root, treenum);
	}

	/**
	 * Create a boostrap sample of a data matrix
	 * 
	 * @param data		the data matrix to be sampled
	 * @param train		the bootstrap sample
	 * @param test		the records that are absent in the bootstrap sample
	 */
	@SuppressWarnings("unchecked")
	private void BootStrapSample( 	ArrayList<ArrayList<String>> data, 
			ArrayList<ArrayList<String>> train,
			ArrayList<ArrayList<String>> test,
			int treeNum)
	{
		ArrayList<Integer> indices = new ArrayList<Integer>();

		for (int n=0; n<trainDataSize; n++)
			indices.add((int) Math.floor(Math.random()*trainDataSize));

		//have to initialize it first
		ArrayList<Boolean> in = new ArrayList<Boolean>();
		for (int n=0; n<trainDataSize; n++)
			in.add(false); 

		for (int num : indices)
		{
			ArrayList<String> k = data.get(num);
			train.add((ArrayList<String>) k.clone());
			in.set(num, true);
		}		
		System.out.println("created training-data for tree : "+ treeNum);

		for (int i=0; i<trainDataSize; i++)
			// everywhere its set to false we get those to test data
			if (!in.get(i))
				test.add(data.get(i)); 

		System.out.println("created testing-data for tree : "+ treeNum);
	}

	// This method creates the decision tree from the specifications
	private TreeNode CreateTree(ArrayList<ArrayList<String>> train, int treeNum)
	{
		// Create a root and attach data like label and actual data to it
		TreeNode root = new TreeNode();
		root.label = "|ROOT|";
		root.data = train;

		// Split the tree root
		RecursiveSplit(root, treeNum);

		// Now that the tree is generated, return it to the parent function
		return root;
	}

	
	
	private void RecursiveSplit(TreeNode parent, int treeNum)
	{
		// Step  1:  If this node is a leaf, it will mark isLeaf true and mark Class with the leaf's class.
		// This will be the base case for recursion
		if (!parent.isLeaf)
		{
			// This method will find out whether the current node is at leaf level or not.
			String Class = CheckIfLeaf(parent.data);

			// If the current node is at leaf level, then return.
			if (Class != null)
			{
				parent.isLeaf=true;
				parent.Class=Class;
				return;
			}


			/*
			 * Step 2:  Create a group of nodes and keep their references in node's fields 		
			 * Create a left and right node and keep their references in this node's left and right fields. 
			 * For debugging purposes, the generation number is also recorded. 
			 * The {@link RandomForest#Ms Ms} attributes are now chosen by the {@link #GetVarsToInclude() GetVarsToInclude} function
			 * */
			int numOfRecordsInThisNode = parent.data.size();			

			parent.ChildNode = new ArrayList<TreeNode>();
			for(TreeNode node : parent.ChildNode)
			{
				node = new TreeNode();
				node.generation = parent.generation+1;
			}

			// Randomly selects Ms attributes out of M total attributes
			ArrayList<Integer> vars = GetVarsToInclude();

			// Create a wrapper class object to hold entropy value
			DoubleWrap lowestEntropy = new DoubleWrap(Double.MAX_VALUE);

			/*
			Step 3: For all Ms variables, first sort the data records by that attribute, then look through the values from lowest to highest. 
			 * If value i is not equal to value i+1, record i in the list of "indicesToCheck." This speeds up the splitting. 

			 */
			for (int m : vars)
			{
				// which data points to be scrutinized 
				ArrayList<Integer> DataPointToCheck = new ArrayList<Integer>();

				//sorts on m'th column
				SortAtAttribute(parent.data, m);

				for (int n=1; n < numOfRecordsInThisNode; n++)
				{
					String classA = GetClass(parent.data.get(n-1));
					String classB = GetClass(parent.data.get(n));

					// If the label n-1'th label does not match with n'th label, add it to checklist.
					if(!classA.equalsIgnoreCase(classB))
						DataPointToCheck.add(n);
				}

				// If all the Y-values are same, then get the class directly
				if (DataPointToCheck.size() == 0)
				{
					parent.isLeaf = true;
					parent.Class = GetClass(parent.data.get(0));
					continue;
				}

				// If the number of indices in indicesToCheck >  MIN_SIZE_TO_CHECK_EACH, 
				// we check the entropy at every index otherwise, we check the entropy for all. 
				// The "E" variable records the entropy and we are trying to find the minimum in which to split on.
				if (DataPointToCheck.size() > MIN_SIZE_TO_CHECK_EACH)
				{
					for (int i=0; i< DataPointToCheck.size(); i+=INDEX_SKIP)
					{
						// After this function is called, lowestEntropy will be updated with lowestEntropy
						calculateEntropy(m, DataPointToCheck.get(i), numOfRecordsInThisNode, lowestEntropy, parent);
						
						// If Entropy becomes zero for the k'th point, it has reached its minimum. 
						// So, max IG will be there at this point.
						if (lowestEntropy.data == 0)
							break;
					}
				}
				
				// We check the entropy for every step
				else
				{
					for (int k : DataPointToCheck)
					{
						// After this function is called, lowestEntropy will be updated with lowestEntropy
						calculateEntropy(m, k, numOfRecordsInThisNode, lowestEntropy, parent);
						
						// If Entropy becomes zero for the k'th point, it has reached its minimum. 
						// So, max IG will be there at this point.
						if (lowestEntropy.data == 0)
							break;
					}
				}
				
				// If Entropy becomes zero for the k'th point, it has reached its minimum. 
				// So, max IG will be there at this point.
				if (lowestEntropy.data == 0)
					break;
			}
			
			
			// Step 4: The newly generated left and right nodes are now checked.
			for(TreeNode Child: parent.ChildNode)
			{
				// If the node has only one record, we mark it as a leaf and set its class equal to the class of the record. 
				if(Child.data.size()==1)
				{
					Child.isLeaf=true;
					Child.Class = GetClass(Child.data.get(0));
				}
				
				// If it has less than MIN_NODE_SIZE records, then we mark it as a leaf and set its class equal to the majority class.
				else if(Child.data.size() < MIN_NODE_SIZE)
				{
					Child.isLeaf=true;
					Child.Class = GetMajorityClass(Child.data);
				}
				
				// If it has more, then we do a manual check on its data records 
				else
				{
					Class = CheckIfLeaf(Child.data);
					if(Class==null)
					{
						Child.isLeaf=false;
						Child.Class=null;
					}
					
					//  If all have the same class, then it is marked as a leaf.
					else
					{
						Child.isLeaf=true;
						Child.Class=Class;
					}
				}
				
				// If not, then we run RecursiveSplit on that node.
				if(!Child.isLeaf)
				{
					RecursiveSplit(Child, treeNum);
				}
			}
		}
	}


	// Given a data matrix, check if all the y values are the same. 
	// If so, return that y value, null if not
	private String CheckIfLeaf(ArrayList<ArrayList<String>> data)
	{
		boolean isCLeaf = true;

		// Get the last label value for the very first record. We call it class.
		String ClassA = GetClass(data.get(0));

		// Now iterate over all the data
		for(ArrayList<String> record : data)
		{
			// If the the class for the subsequent records do not match the class of the first record,
			// it means it is not a leaf. In this case, return.
			if(! ClassA.equalsIgnoreCase(GetClass(record)))
			{
				isCLeaf = false;
				return null;
			}
		}

		// If the control fell through here, it means that all these values are leaf level.
		// Return the class
		if (isCLeaf)
			return GetClass(data.get(0));
		else
			return null;
	}

	// This method returns the last value in the record.
	// This will be the label that we are trying to predict.
	public static String GetClass(ArrayList<String> record)
	{
		return record.get(RandomForest.M).trim();
	}

	// Of the M attributes, select Ms at random.
	private ArrayList<Integer> GetVarsToInclude() 
	{
		// Create a boolean array to track the status of M attributes
		boolean[] whichVarsToInclude = new boolean[RandomForest.M];

		// Set them to false by default
		for (int i=0; i<RandomForest.M; i++)
			whichVarsToInclude[i]=false;

		while (true)
		{
			// Generate a random number. Set that to true.
			int a = (int)Math.floor(Math.random() * RandomForest.M);
			whichVarsToInclude[a]=true;

			// Total number of attributes who were set as true become our trainDataSize
			int trainDataSize=0;
			for (int i=0; i<RandomForest.M; i++)
				if (whichVarsToInclude[i])
					trainDataSize++;

			// Do this unless the count reaches till the number of attributes to be selected
			if (trainDataSize == RandomForest.Ms)
				break;
		}

		// Generate a record list of Ms size. It will hold those variables which should be included.
		ArrayList<Integer> shortRecord = new ArrayList<Integer>(RandomForest.Ms);

		for (int i=0;i<RandomForest.M; i++)
			if (whichVarsToInclude[i])
				shortRecord.add(i);

		// Return list of those variables which should be included.
		return shortRecord;
	}

	

	// This class holds the entropy value 
	private class DoubleWrap
	{
		public double data;
		public DoubleWrap(double d)
		{
			this.data = d;
		}		
	}
	/**
	 * This method will get the classes and will return the updates
	 * 
	 */
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
	public String Evaluate(ArrayList<String> record, ArrayList<ArrayList<String>> tester){
		TreeNode evalNode=root;		
		while (true) {				
			if(evalNode.isLeaf)
				return evalNode.Class;
			else{
				if(evalNode.spiltOnCategory){
					// if its categorical
					String recordCategory = record.get(evalNode.splitAttributeM);
					boolean found=false;String Res = evalNode.Missingdata.get(GetClass(record));

					for(TreeNode child:evalNode.ChildNode){

						// Check for child with label same the data point
						if(recordCategory.equalsIgnoreCase(child.label)){//what if the category is not present at all
							evalNode = child;
							found = true;
							break;
						}
					}if(!found){
						for(TreeNode child:evalNode.ChildNode){
							if(Res!=null){
								if(Res.trim().equalsIgnoreCase(child.label)){
									evalNode = child;
									break;
								}
							}else{
								return "n/a";
							}
						}
					}
				}else{
					//if its real-valued
					double Compare = Double.parseDouble(evalNode.splitValue);
					double Actual = Double.parseDouble(record.get(evalNode.splitAttributeM));
					if(Actual <= Compare){
						if(evalNode.ChildNode.get(0).label.equalsIgnoreCase("Left"))
							evalNode=evalNode.ChildNode.get(0);
						else
							evalNode=evalNode.ChildNode.get(1);
						//							evalNode=evalNode.left;
						//							System.out.println("going in child :"+evalNode.label);
					}else{
						if(evalNode.ChildNode.get(0).label.equalsIgnoreCase("Right"))
							evalNode=evalNode.ChildNode.get(0);
						else
							evalNode=evalNode.ChildNode.get(1);
						//							evalNode=evalNode.right;
						//							System.out.println("going in child :"+evalNode.label);
					}
				}
			}
		}
	}



	/**
	 * Sorts a data matrix by an attribute from lowest record to highest record
	 * 
	 * @param data			the data matrix to be sorted
	 * @param m				the attribute to sort on
	 */
	private void SortAtAttribute(ArrayList<ArrayList<String>> data, int m) 
	{
		if(forest.DataAttributes.get(m) == 'C')
			System.out.print("");
		else
			Collections.sort(data,new AttributeComparator(m));

	}

	/**
	 * This class compares two data records by numerically/categorically comparing a specified attribute
	 * 
	 *
	 */
	private class AttributeComparator implements Comparator<ArrayList<String>>{		
		/** the specified attribute */
		private int m;
		/**
		 * Create a new comparator
		 * @param m			the attribute in which to compare on
		 */
		public AttributeComparator(int m)
		{
			this.m = m;
		}
		
		/**
		 * Compare the two data records. They must be of type int[].
		 * 
		 * @param arg1		data record A
		 * @param arg2		data record B
		 * @return			-1 if A[m] < B[m], 1 if A[m] > B[m], 0 if equal
		 */
		@Override
		public int compare(ArrayList<String> arg1, ArrayList<String> arg2) 
		{
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

	/**
	 * Given a data matrix, return the most popular Y value (the class)
	 * @param data	The data matrix
	 * @return		The most popular class
	 */
	private String GetMajorityClass(ArrayList<ArrayList<String>> data){
		// find the max class for this data.
		ArrayList<String> ToFind = new ArrayList<String>();
		for(ArrayList<String> s:data){
			ToFind.add(s.get(s.size()-1));
		}
		String MaxValue = null; int MaxCount = 0;
		for(String s1:ToFind){
			int count =0;
			for(String s2:ToFind){
				if(s2.equalsIgnoreCase(s1))
					count++;
			}
			if(count > MaxCount){
				MaxValue = s1;
				MaxCount = count;
			}
		}return MaxValue;
	}
	/**
	 * Checks the entropy of an index in a data matrix at a particular attribute (m) and returns the entropy. 
	 * If the entropy is lower than the minimum to date (lowestE), it is set to the minimum.
	 * 
	 * If the attribute is real-valued (Regression) then: 
	 * The total entropy is calculated by getting the sub-entropy for below the split point and after the split point.
	 * 	The sub-entropy is calculated by first getting the proportion of each of the classes in this sub-data matrix. Then the entropy is calculated. 
	 *  The lower sub-entropy and upper sub-entropy are then weight averaged to obtain the total entropy.
	 * 
	 * If the attribute is categorical-valued (Classification) then :
	 *	The total entropy is calculated by entropy of the each of categorical-value in the attribute
	 *	The split is done by recursively calling split over each of the categorical value;
	 * 
	 * @param m							the attribute to split on
	 * @param n							the index to check
	 * @param numOfRecordsInThisNode	the number of records in the data matrix
	 * @param lowestE					the minimum entropy to date
	 * @param parent					the parent node
	 * @return							the entropy of this split
	 */
	private double calculateEntropy(int m, int n, int numOfRecordsInThisNode, DoubleWrap lowestEntropy, TreeNode parent)
	{

		double entropy =0;

		// Base cases: If index to check is less than 1
		if (n < 1) 
			return 0;
		
		// Base cases: index to check is more than the number of records in this node
		if (n > numOfRecordsInThisNode)
			return 0;

		// This is categorical thing
		if(forest.DataAttributes.get(m)=='C')
		{
			// find out the distinct values in that attribute... from parent.data
			ArrayList<String> distinctCategories = new ArrayList<String>(); //unique categories
			ArrayList<String> distinctClasses = new ArrayList<String>(); //unique classes
			
			// Class Vs Node-label
			HashMap<String, String> ChildMissingMap = new HashMap<String, String>();
			
			// Node-Label Vs frequency
			HashMap<String, Integer> ChilFreq = new HashMap<String, Integer>();

			for(ArrayList<String> s: parent.data)
			{
				if(!distinctCategories.contains(s.get(m).trim()))
				{
					distinctCategories.add(s.get(m).trim());
					ChilFreq.put(s.get(m), 0);
				}

				if(!distinctClasses.contains(GetClass(s)))
					distinctClasses.add(GetClass(s));
			}

			//data pertaining to each of the value
			HashMap<String, ArrayList<ArrayList<String>>> ChildDataMap = new HashMap<String, ArrayList<ArrayList<String>>>();
			for(String s : distinctCategories)
			{
				ArrayList<ArrayList<String>> child_data = new ArrayList<ArrayList<String>>();
				for(ArrayList<String> S:parent.data)
				{
					if(s.trim().equalsIgnoreCase(S.get(m).trim()))
						child_data.add(S);
				}
				ChildDataMap.put(s, child_data);
			}

			//can merge the above two
			//Adding missing-data-suits
			for(String S1: distinctClasses)
			{
				int max=0;
				String Resul = null;
				
				for(ArrayList<String> S2:parent.data)
				{
					if(GetClass(S2).equalsIgnoreCase(S1))
					{
						if(ChilFreq.containsKey(S2.get(m)))
							ChilFreq.put(S2.get(m), ChilFreq.get(S2.get(m))+1);
					}
					if(ChilFreq.get(S2.get(m))>max)
					{
						max=ChilFreq.get(S2.get(m));
						Resul = S2.get(m);
					}
				}
				ChildMissingMap.put(S1, Resul);
			}
			
			// Calculation of entropy
			for(Entry<String,ArrayList<ArrayList<String>>> entry : ChildDataMap.entrySet())
			{
				entropy = entropy + CalEntropy(getClassProbs(entry.getValue()))* entry.getValue().size();
			}
			
			entropy = entropy/((double)numOfRecordsInThisNode);
			
			
			// If the calculated entropy is lesser than the lowest entropy seen so far, then update the values
			if (entropy < lowestEntropy.data)
			{
				lowestEntropy.data = entropy;
				parent.splitAttributeM = m;
				parent.spiltOnCategory = true;
				parent.splitValue = parent.data.get(n).get(m);
				parent.Missingdata = ChildMissingMap;
				
				// Add data to the child
				ArrayList<TreeNode> Children = new ArrayList<TreeNode>();
				
				for(Entry<String,ArrayList<ArrayList<String>>> entry:ChildDataMap.entrySet())
				{
					TreeNode Child = new TreeNode();
					Child.data = entry.getValue();
					Child.label = entry.getKey();
					Children.add(Child);
				}
				
				// Attach the children to the parent
				parent.ChildNode=Children;
			}
		}
		
		// This is not categorical, it is something else
		else
		{
			HashMap<String, ArrayList<ArrayList<String>>> UpLo = GetUpperLower(parent.data, n, m);

			ArrayList<ArrayList<String>> lower = UpLo.get("lower"); 
			ArrayList<ArrayList<String>> upper = UpLo.get("upper"); 

			ArrayList<Double> pl=getClassProbs(lower);
			ArrayList<Double> pu=getClassProbs(upper);
			double eL=CalEntropy(pl);
			double eU=CalEntropy(pu);

			entropy =(eL*lower.size()+eU*upper.size())/((double)numOfRecordsInThisNode);

			if (entropy < lowestEntropy.data)
			{
				lowestEntropy.data = entropy;
				parent.splitAttributeM=m;
				parent.spiltOnCategory=false;
				parent.splitValue = parent.data.get(n).get(m).trim();
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
	 * Returns lower and upper data for parent.data
	 * 
	 * @param data	parent data
	 * @param n2	data point
	 * @param m		attribute value
	 * @return		map of upper and lower
	 */
	private HashMap<String, ArrayList<ArrayList<String>>> GetUpperLower(ArrayList<ArrayList<String>> data, int n2,int m) 
	{
		HashMap<String, ArrayList<ArrayList<String>>> UpperLower = new HashMap<String, ArrayList<ArrayList<String>>>();
		ArrayList<ArrayList<String>> lower = new ArrayList<ArrayList<String>>(); 
		ArrayList<ArrayList<String>> upper = new ArrayList<ArrayList<String>>(); 
		
		for(int n=0;n<n2;n++)
			lower.add(data.get(n));
		
		for(int n=n2;n<data.size();n++)
			upper.add(data.get(n));
		UpperLower.put("lower", lower);
		UpperLower.put("upper", upper);

		return UpperLower;
	}

	/**
	 * Given a data matrix, return a probabilty mass function representing 
	 * the frequencies of a class in the matrix (the y values)
	 * 
	 * @param records		the data matrix to be examined
	 * @return				the probability mass function
	 */
	private ArrayList<Double> getClassProbs(ArrayList<ArrayList<String>> record){
		double trainDataSize=record.size();
		HashMap<String, Integer > counts = new HashMap<String, Integer>();
		for(ArrayList<String> s : record){
			String clas = GetClass(s);
			if(counts.containsKey(clas))
				counts.put(clas, counts.get(clas)+1);
			else
				counts.put(clas, 1);
		}
		ArrayList<Double> probs = new ArrayList<Double>();
		for(Entry<String, Integer> entry : counts.entrySet()){
			double prob = entry.getValue()/trainDataSize;
			probs.add(prob);
		}return probs;
	}

	/**
	 *  ln(2)   
	 */
	private static final double logoftwo=Math.log(2);

	/**
	 * Given a probability mass function indicating the frequencies of class representation, calculate an "entropy" value using the method
	 * in Tan|Steinbach|Kumar's "Data Mining" textbook
	 * 
	 * @param ps			the probability mass function
	 * @return				the entropy value calculated
	 */
	private double CalEntropy(ArrayList<Double> ps)
	{
		double e=0;		
		for (double p:ps)
		{
			if (p != 0) //otherwise it will divide by zero - see TSK p159
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
	private void FlushData(TreeNode node, int treenum)
	{
		node.data=null;
		if(node.ChildNode!=null)
		{
			for(TreeNode TN : node.ChildNode)
			{
				if(TN != null)
					FlushData(TN,treenum);
			}
		}
	}

}
