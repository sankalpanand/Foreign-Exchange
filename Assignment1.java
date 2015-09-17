import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.LocalTime;

public class Assignment1 {

	public static void main(String[] args) throws ParseException, IOException 
	{
		// TODO Auto-generated method stub
		Assignment1 obj = new Assignment1();
		obj.parseFile();
	}

	public void parseFile() throws IOException 
	{
		// String inputFile = "D:\\Carnegie Mellon University\\Sem 4\\Big Data Analytics\\2014\\EURUSD\\EURUSD-2014-01.csv";
		String inputFile = "D:\\Carnegie Mellon University\\Sem 4\\Big Data Analytics\\2014\\EURUSD\\sample.csv";
		String outputFile = "D:\\Carnegie Mellon University\\Sem 4\\Big Data Analytics\\2014\\EURUSD\\EURUSD-2014-01_Processed.csv";
		File file = new File(outputFile);
		// if file doesnt exists, then create it
		if (!file.exists()) 
		{
			file.createNewFile();
		}
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);

		BufferedReader br = null;
		String line = "";

		try {

			br = new BufferedReader(new FileReader(inputFile));
			int prevMinute = -1;
			int count = 0;
			double delta = 0;
			double totalDelta = 0;
			double totalBid = 0;
			double totalAsk = 0;			
			double prevAsk = 0;
			double prevBid = 0;

			double maxBid = Integer.MIN_VALUE;
			double minBid = Integer.MAX_VALUE;
			double maxAsk = Integer.MIN_VALUE;
			double minAsk = Integer.MAX_VALUE;
			double maxDelta = Integer.MIN_VALUE;
			double minDelta = Integer.MAX_VALUE;
			
			int bidDirection = 1;
			int askDirection = 1;
			double averageBid = 0;
			double averageAsk = 0;
			double averageDelta = 0;

			while ((line = br.readLine()) != null) 
			{
				String[] row = line.split(",");
				String conversion = row[0];
				String timeStamp = row[1];
				LocalTime t = LocalTime.parse(timeStamp.split(" ")[1]);
				int currMinute = t.getMinute();
				double currBid = Double.parseDouble(row[2]);
				double currAsk = Double.parseDouble(row[3]);
				
				
				// It means this data is for a new minute. 
				// Store previous minute values to text file. Reset the variables.
				if(prevMinute != -1 && currMinute != prevMinute)
				{
					// Compute Directionality
					if((double) totalAsk/count > averageAsk) askDirection = 1;
					else askDirection = 0;

					if((double) totalBid/count > averageBid) bidDirection = 1;
					else bidDirection = 0;

					// Update Average prices for previous block
					averageBid = (double) totalBid/count;
					averageAsk = (double) totalAsk/count;
					averageDelta = (double) totalDelta/count;
					
					// This is decimal formatting to avoid printing double values in E notation.
					DecimalFormat df = new DecimalFormat("#");
			        df.setMaximumFractionDigits(5);
					
					String printString = conversion + "," + timeStamp + "," + 
					df.format(maxBid) + "," + df.format(minBid) + "," + df.format(averageBid) + "," + 
					df.format(maxAsk) + "," + df.format(minAsk) + "," + df.format(averageAsk) + "," + 
					df.format(maxDelta) + "," + df.format(minDelta) + "," + df.format(averageDelta) + "," + 
					bidDirection + "," + askDirection + "\n";
					
					// Write to file
					bw.write(printString);
					// System.out.println(printString);
					
					// Reset Values
					count = 1;
					totalBid = currBid;
					totalAsk = currAsk;
					totalDelta = currAsk - currBid;					
					maxBid = currBid;
					minBid = currBid;
					maxAsk = currAsk;
					minAsk = currAsk;
					maxDelta = currAsk - currBid;
					minDelta = currAsk - currBid;
				}

				// It means the same minute window is going on. Update the variables.
				else
				{
					count++;
					delta = currAsk - currBid;
					
			        
					totalBid = totalBid + currBid;
					totalAsk = totalAsk + currAsk;
					totalDelta = totalDelta + delta;

					if(currBid>maxBid) maxBid = currBid;
					if(currBid<minBid) minBid = currBid;
					if(currAsk>maxAsk) maxAsk = currAsk;
					if(currAsk<minAsk) minAsk = currAsk;
					if(delta>maxDelta) maxDelta = delta;
					if(delta<minDelta) minDelta = delta;

					prevAsk = currAsk;
					prevBid = currBid;
					prevMinute = currMinute;
				}
			}
		} 

		catch (IOException e) 
		{
			e.printStackTrace();
		}

		finally 
		{
			if (br != null) 
			{
				try 
				{
					br.close();
					bw.close();
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
	}

}
