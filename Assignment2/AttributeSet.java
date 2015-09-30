package Assignment2;

import java.util.ArrayList;

public class AttributeSet 
{
	public String name;
	public ArrayList<DiscreteAttributes> attributes;
	public double entropy;
	public boolean isUsed;
	
	public AttributeSet() 
	{
		attributes = new ArrayList<DiscreteAttributes>();
		entropy = -1;
		isUsed = false;
	}
	
	
}
