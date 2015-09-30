package Assignment2;

import java.util.ArrayList;

public class Instance {

	String conversion;
	String timeStamp;
	
	// Features
	double maxBid; 
	double minBid;
	double averageBid;
	double maxAsk;
	double minAsk;
	double averageAsk;
	double maxDelta;
	double minDelta;
	double averageDelta;
	
	// Labels
	int bidDirection; 
	int askDirection;	
	
	public ArrayList<DiscreteAttributes> attributes;
	
	public Instance(String[] line) 
	{
		this.conversion = line[0];
		this.timeStamp = line[1];
		this.maxBid = Double.parseDouble(line[2]);
		this.minBid = Double.parseDouble(line[3]);
		this.averageBid = Double.parseDouble(line[4]);
		
		this.maxAsk = Double.parseDouble(line[5]);
		this.minAsk = Double.parseDouble(line[6]);
		this.averageAsk = Double.parseDouble(line[7]);
		
		this.maxDelta = Double.parseDouble(line[8]);
		this.minDelta = Double.parseDouble(line[9]);
		this.averageDelta = Double.parseDouble(line[10]);
		
		this.bidDirection = Integer.parseInt(line[11]);
		this.askDirection = Integer.parseInt(line[12]);
		
		attributes = new ArrayList<DiscreteAttributes>();
		attributes.add(new DiscreteAttributes("maxBid", maxBid+""));
		attributes.add(new DiscreteAttributes("minBid", minBid+""));
		attributes.add(new DiscreteAttributes("averageBid", averageBid+""));
		
		attributes.add(new DiscreteAttributes("maxAsk", maxAsk+""));
		attributes.add(new DiscreteAttributes("minAsk", minAsk+""));
		attributes.add(new DiscreteAttributes("averageAsk", averageAsk+""));
		
		attributes.add(new DiscreteAttributes("maxDelta", maxDelta+""));
		attributes.add(new DiscreteAttributes("minDelta", minDelta+""));
		attributes.add(new DiscreteAttributes("averageDelta", averageDelta+""));
		
		attributes.add(new DiscreteAttributes("bidDirection", bidDirection+""));
		attributes.add(new DiscreteAttributes("askDirection", askDirection+""));
		
	}
		
}
