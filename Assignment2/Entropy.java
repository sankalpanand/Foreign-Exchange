package Assignment2;

import java.util.ArrayList;

public class Entropy 
{
	static int setSize =  11;
			
	public static double calculateEntropy(ArrayList<Instance> data) 
	{
		double entropy = 0;

		if(data.size() == 0) 
		{		
			return 0;
		}

		for(int i = 0; i < setSize; i++) 
		{
			int count = 0;
			for(int j = 0; j < data.size(); j++) 
			{
				Instance record = data.get(j);

				if(record.attributes.get(4).value == i) 
				{
					count++;
				}
			}

			double probability = count / (double)data.size();
			if(count > 0) 
			{
				entropy += -probability * (Math.log(probability) / Math.log(2));
			}
		}

		return entropy;
	}

	public static double calculateGain(double rootEntropy, ArrayList<Double> subEntropies, ArrayList<Integer> setSizes, int data) 
	{
		double gain = rootEntropy; 

		for(int i = 0; i < subEntropies.size(); i++) 
		{
			gain += -((setSizes.get(i) / (double)data) * subEntropies.get(i));
		}

		return gain;
	}
}
