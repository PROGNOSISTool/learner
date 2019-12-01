package util;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * Class used to generate TCP yaml files by combining flags with abstract values for sequence and acknowledgment numbers. 
 */
public class YamlWriter {

	public static final String [] synInputAbs = {"V","INV"};
	public static final String [] ackInputAbs = {"V", "INV"};
	public static final String [] synOutputAbs = {"FRESH","SRECV","S+1RECV","ARECV","ZERO","INV"};
	public static final String [] ackOutputAbs = {"S+1SENT","S+DSENT","S+D+1SENT","ASENT","ARECV","INV"};
	public static final String [] flags = {"","SYN","ACK","RST", "FIN"};
	public static final String [][] invalidFlagPairs = {{"SYN","FIN"},{"SYN","RST"},{"FIN","RST"},
	{"SYN","SYN"},{"RST","RST"},{"FIN","FIN"},{"ACK","ACK"},{"",""}};
	
	public YamlWriter() {
	}
	
	public static void main(String args[]) throws Exception {
		YamlWriter yaml = new YamlWriter();
		String filePath = (args.length == 1)? args[0]: "sutinfo.yaml";
		yaml.writeTCPYaml(filePath);
	}
	
	public void writeTCPYaml(String filePath) throws Exception {
		Map<String,Object> yamlMap = buildTCPMap();
		System.out.println(yamlMap);
		buildFile(yamlMap, filePath);
	}
	
	public Map<String, Object> buildTCPMap() {
		List<String> inputCombos = generatePermutations(synInputAbs, ackInputAbs, new ParamCombiner());
		List<String> flagCombos = removeDupFlags(generatePermutations(flags, flags, new FlagCombiner(),invalidFlagPairs));
		System.out.println(flagCombos);
		List<String> flagInputCombos = generatePermutations(arr(flagCombos), arr(inputCombos), new ConcatCombiner());
		List<String> outputCombos = generatePermutations(synOutputAbs, ackOutputAbs, new ParamCombiner());
		List<String> flagOutputCombos = generatePermutations(arr(flagCombos), arr(outputCombos), new ConcatCombiner());
		Map<String, Object> tcpMap = buildLearnerMap("TCP", flagInputCombos, flagOutputCombos, new ArrayList<String>());
		return tcpMap;
	}
	
	public Map<String,Object> buildLearnerMap(String name, List<String> inputInterfaces, List<String> outputInterfaces, List<String> constants) {
		Map<String,Object> yamlMap = new LinkedHashMap<String,Object>();
		yamlMap.put("name", name);
		yamlMap.put("inputInterfaces", toSortedMap(inputInterfaces));
		yamlMap.put("outputInterfaces", toSortedMap(outputInterfaces));
		yamlMap.put("constants", new ArrayList<String>());
		return yamlMap;
	}
	
	private static Map<Object,List<String>> toSortedMap(List<String> strings) {
		Map<Object,List<String>> stringMap = new LinkedHashMap<Object, List<String>>();
		Collections.sort(strings, new Comparator<String>() {
			public int compare (String str1, String str2) {
				return str1.compareTo(str2);
			}
		});
		for(String string : strings) {
			stringMap.put(string, new ArrayList<String>());
		}
		return stringMap;
	}
	
	private static List<String> removeDupFlags(List<String> strings) {
		for(int i = 0; i < flags.length; i ++) 
			for(int j = i + 1; j < flags.length; j ++)
				strings.remove(flags[j]+"+"+flags[i]);
		return strings;
	}
	
	private static String [] arr(List<String> list) {
		return list.toArray(new String [list.size()]);
	}
	
	private List<String> generatePermutations(String [] list1, String [] list2, Combiner combiner) {
		return generatePermutations(list1,list2,combiner, new String [][]{});
	}
	
	private List<String> generatePermutations(String [] list1, String [] list2, Combiner combiner, String [][] invalidMatches) {
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
	
	public void buildFile(Map<String, Object> yamlMap, String filePath) throws Exception{
		File file = new File(filePath);
		if(file.exists() == false) {
			file.createNewFile();
		}
		Yaml yaml = new Yaml();
		yaml.dump(yamlMap, new PrintWriter(file));
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
