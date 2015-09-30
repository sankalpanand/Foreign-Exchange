package Assignment2;
import java.util.*;

public class DecisionTree 
{
	public TreeNode buildTree(ArrayList<Instance> records, TreeNode root, LearningSet learningSet) 
	{
		int bestAttribute = -1;
		double bestGain = 0;
		root.entropy = Entropy.calculateEntropy(root.data);

		if (root.entropy == 0) 
		{
			return root;
		}

		for (int i = 0; i < 9 - 2; i++) 
		{
			if (!Assignment2.isAttributeUsed(i)) 
			{
				double entropy = 0;
				ArrayList<Double> entropies = new ArrayList<Double>();
				ArrayList<Integer> setSizes = new ArrayList<Integer>();

				for (int j = 0; j < 9 - 2; j++) 
				{
					ArrayList<Instance> subset = subset(root, i, j);
					setSizes.add(subset.size());

					if (subset.size() != 0) 
					{
						entropy = Entropy.calculateEntropy(subset);
						entropies.add(entropy);
					}
				}

				double gain = Entropy.calculateGain(root.entropy, entropies, setSizes, root.data.size());

				if (gain > bestGain) 
				{
					bestAttribute = i;
					bestGain = gain;
				}
			}
		}
		if (bestAttribute != -1) 
		{
			int setSize = 2;
			root.testAttribute = (new DiscreteAttributes(Assignment2.attributeMap.get(bestAttribute), 0+""));
			root.children = new TreeNode[setSize];
			root.isUsed = true;
			Assignment2.usedAttributes.add(bestAttribute);

			for (int j = 0; j < setSize; j++) {
				root.children[j] = new TreeNode();
				root.children[j].parent = root;
				root.children[j].data = subset(root, bestAttribute, j);
				root.children[j].testAttribute.name = "";
				root.children[j].testAttribute.value = j;
			}

			for (int j = 0; j < setSize; j++) 
			{
				buildTree(root.children[j].data, root.children[j], learningSet);
			}

			root.data = null;
		} 
		else 
		{
			return root;
		}

		return root;
	}

	public ArrayList<Instance> subset(TreeNode root, int attr, int value) 
	{
		ArrayList<Instance> subset = new ArrayList<Instance>();

		for (int i = 0; i < root.data.size(); i++) 
		{
			Instance record = root.data.get(i);

			if (record.attributes.get(attr).value == value) 
			{
				subset.add(record);
			}
		}
		return subset;
	}

}
