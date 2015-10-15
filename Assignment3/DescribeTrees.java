package Assignment3;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import Assignment2.Record;

public class DescribeTrees 
{
	//method to take the txt fle as input and pass those values to random forests
	BufferedReader BR = null;
	String path;
	String layout;

	

	public ArrayList<ArrayList<String>> CreateInputCateg(String path) 
	{
		ArrayList<ArrayList<String>> records = new ArrayList<ArrayList<String>>();
		try 
		{
			String line;
			BR = new BufferedReader(new FileReader(path));

			while ((line = BR.readLine()) != null) 
			{
				if (line != null) 
				{
					ArrayList<String> DataPoint = new ArrayList<String>();
					String[] tokens = line.split(" ");

					// i=2 because 0=currency pair and 1=timestamp
					for (int i = 2; i < tokens.length; i++) 
					{
						DataPoint.add(tokens[i]);
					}
					records.add(DataPoint);
				}
			}
		}

		catch (IOException e) 
		{
			e.printStackTrace();
		}

		finally 
		{
			try 
			{
				if (BR != null)
					BR.close();
			} 
			catch (IOException ex) 
			{
				ex.printStackTrace();
			}
		}
		return records;
	}

	public static ArrayList<Record> buildTransactionsObject(String path) 
	{
		BufferedReader reader = null;
		DataInputStream dis = null;
		ArrayList<Record> records = new ArrayList<Record>();

		try 
		{ 
			File f = new File(path);
			FileInputStream fis = new FileInputStream(f); 
			reader = new BufferedReader(new InputStreamReader(fis));

			// read the first record of the file
			String line;
			Record r = null;
			while ((line = reader.readLine()) != null) 
			{
				r = new Record(line.split(","));
				records.add(r);
			}
		} 

		catch (Exception e) 
		{
			e.printStackTrace(); 
		}

		finally 
		{ 
			if (dis != null) 
			{
				try 
				{
					dis.close();
				} 
				catch (IOException e) 
				{
					System.out.println("IOException while trying to close the file: " + e.getMessage()); 
				}
			}
		}
		return records;
	}

	/**
	 * Breaks the run length code for data layout
	 * N-Nominal/Number/Real
	 * C-Categorical/Alphabetical/Numerical
	 * I-Ignore Attribute
	 * L-Label - last of the fields
	 *
	 * @param dataInfo
	 * @return
	 */
	public char[] CreateLayout(String dataIn)
	{
		char[] lay = dataIn.trim().toCharArray();
		ArrayList<Character> layo = new ArrayList<Character>();

		ArrayList<Character> LaySet = new ArrayList<Character>();
		LaySet.add('N');
		LaySet.add('C');
		LaySet.add('I');
		LaySet.add('L');

		ArrayList<Integer> number = new ArrayList<Integer>();
		for(char ch:lay){
			if(ch!=',')
				layo.add(ch);
		}
		lay = new char[layo.size()];
		for(int i=0;i<layo.size();i++)
			lay[i] = layo.get(i);
		layo.clear();
		for(int i=0;i<lay.length;i++){
			if(LaySet.contains(lay[i])){
				if(convertonumb(number)<=0)
					layo.add(lay[i]);
				else{
					for(int j=0;j<convertonumb(number);j++){
						layo.add(lay[i]);
					}
				}
				number.clear();
			}
			else
				number.add(Character.getNumericValue(lay[i]));					
		}
		lay = new char[layo.size()];
		for(int i=0;i<layo.size();i++)
			lay[i] = layo.get(i);
		return lay;
	}

	/**
	 * converts arraylist to numbers
	 * 
	 * @param number
	 * @return
	 */
	private int convertonumb(ArrayList<Integer> number) {
		// TODO Auto-generated method stub
		int numb=0;
		if(number!=null){
			for(int ij=0;ij<number.size();ij++){
				numb=numb*10+number.get(ij);
			}
		}
		return numb;
	}

	/**
	 * Creates final list that Forest will use as reference...
	 * @param dataIn
	 * @return
	 */
	public ArrayList<Character> CreateFinalLayout(String dataIn) 
	{
		// "N 3 C 2 N C 4 N C 8 N 2 C 19 N L I";
		char[] lay =dataIn.toCharArray();
		ArrayList<Character> layo = new ArrayList<Character>();
		ArrayList<Character> LaySet = new ArrayList<Character>();
		LaySet.add('N');LaySet.add('C');LaySet.add('I');LaySet.add('L');
		ArrayList<Integer> number = new ArrayList<Integer>();
		for(char ch:lay){
			if(ch!=',')
				layo.add(ch);
		}
		lay = new char[layo.size()];
		for(int i=0;i<layo.size();i++)
			lay[i] = layo.get(i);
		layo.clear();
		for(int i=0;i<lay.length;i++){
			if(LaySet.contains(lay[i])){
				if(convertonumb(number)<=0)
					layo.add(lay[i]);
				else{
					for(int j=0;j<convertonumb(number);j++){
						layo.add(lay[i]);
					}
				}
				number.clear();
			}
			else
				number.add(Character.getNumericValue(lay[i]));					
		}
		for(int i=0;i<layo.size();i++)
			if(layo.get(i)=='I'||layo.get(i)=='L'){
				layo.remove(i);
				i=i-1;
			}
		layo.add('L');
		System.out.println("Final Data Layout Parameters "+layo);
		return layo;
	}
}
