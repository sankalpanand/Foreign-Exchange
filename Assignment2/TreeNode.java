package Assignment2;

import java.util.*;

public class TreeNode {
	public TreeNode parent;
	public TreeNode[] children;
	public ArrayList<Instance> data;
	public double entropy;
	public boolean isUsed;
	public DiscreteAttributes testAttribute;
	

	public TreeNode() 
	{
		this.data = new ArrayList<Instance>();
		this.entropy = 0.0;
		this.parent = null;
		this.children = null;
		this.isUsed = false;
	}
}
