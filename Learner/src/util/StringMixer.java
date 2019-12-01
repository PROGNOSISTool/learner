package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringMixer {
	
	public List<String> generatePermutations(String [] list1, String [] list2, Combiner combiner) {
		return generatePermutations(list1,list2,combiner, new String [][]{});
	}
	
	public List<String> generatePermutations(String [] list1, String [] list2, Combiner combiner, String [][] invalidMatches) {
		List<List<String>> lists = new ArrayList<List<String>>();
		lists.add(Arrays.asList(list1));
		lists.add(Arrays.asList(list2));
		List<String> result = new ArrayList<String>();
		generatePermutations(lists, result, 0, null, combiner, invalidMatches);
		return result;
	}
	
	private void generatePermutations(List<List<String>> lists, List<String> result, int depth, String current, Combiner combiner, String [][] invalidMatches) {
		if(depth == lists.size()) {
			result.add(current);
			return;
		}
		for(int i = 0; i < lists.get(depth).size(); i ++) {
			String element = lists.get(depth).get(i);
			if(current != null){
				
				if(isValidMatch(current, element, invalidMatches)){
					generatePermutations(lists, result, depth + 1, combiner.combine(current, element), combiner, invalidMatches);
				}
			}
			else {
				generatePermutations(lists, result, depth + 1, lists.get(depth).get(i), combiner, invalidMatches);
			}
		}
	}
	
	public void printGrid(String a[][])
	{
	   for(int i = 0; i < a.length; i++)
	   {
	      for(int j = 0; j < 2; j++)
	      {
	         System.out.printf(a[i][j]);
	      }
	      System.out.println();
	   }
	}
	
	public boolean isValidMatch(String str1, String str2, String [][] invalidMatches) {
		for(int i = 0; i < invalidMatches.length; i ++) {
			String[] invalidPair = invalidMatches[i];
			if( (invalidPair[0].equalsIgnoreCase(str1) && invalidPair[1].equalsIgnoreCase(str2)) ||
					((invalidPair[0].equalsIgnoreCase(str2) && invalidPair[1].equalsIgnoreCase(str1)))) {
				System.out.println(str1 + " " + str2 + " "+ invalidPair[0]+ " "+invalidPair[1]);
				return false;
			}
		}
		return true;
	}
	
	static interface Combiner {
		String combine(String str1, String str2);
	} 
	
	static class ParamCombiner implements Combiner {

		public String combine(String str1, String str2) {
			return "(" +str1+","+str2+")";
		}
		
	}
	
	static class ConcatCombiner implements Combiner {
		private String betweenString = "";
		public ConcatCombiner() {}
		public ConcatCombiner(String betweenString) {this.betweenString = betweenString;}

		public String combine(String str1, String str2) {
			return str1 + this.betweenString + str2;
		}	
	}
	
	static class FlagCombiner extends ConcatCombiner {
		public FlagCombiner() {
			super("+");
		}
		public String combine(String str1, String str2) {
			if(str2.equals(""))
				return str1;
			else
				if(str1.equals(""))
					return str2;
				else
					return super.combine(str1, str2);
		}	
	}
	
}
