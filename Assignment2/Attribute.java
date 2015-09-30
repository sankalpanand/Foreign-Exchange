package Assignment2;

abstract public class Attribute 
{
	public String name;
	public double value;
	public boolean isUnknown;
	
	
	public Attribute(String name, String value) 
	{
		this.name = name;
		try 
		{
			this.value = Double.valueOf(value);
			this.isUnknown = false;
		}
		catch(NumberFormatException nfe)
		{
			this.value = -1;
			this.isUnknown = true;
		}
	}
}
