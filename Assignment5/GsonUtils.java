package Assignment5;

import com.google.gson.Gson;

public class GsonUtils {

	
	public static String serialize(DecisionTree tree) 
	{
		Gson gson = new Gson();		
		String json = gson.toJson(tree);
		return json;
	}
	
	

}
