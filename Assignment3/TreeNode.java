package Assignment3;

import java.util.ArrayList;
import java.util.HashMap;


class TreeNode
{
	public boolean isLeaf;
	public ArrayList<TreeNode> ChildNode ;
	public HashMap<String, String> Missingdata;
	
	// Split on which attribute?
	public int splitAttributeM;
	public boolean spiltOnCategory ;
	public String Class;
	public ArrayList<ArrayList<String>> data;
	public String splitValue;//check this if it return false on splitonCateg
	
	// Label of each node
	public String label;
	public int generation;

	public TreeNode()
	{
		splitAttributeM=-99;
		splitValue="-99";
		generation=1;
	}
	
	
}
