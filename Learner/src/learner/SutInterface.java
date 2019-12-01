package learner;

import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public class SutInterface {
	public TreeSet<Integer> constants;  // sorted constants
	public TreeMap<String, List<String>> inputInterfaces;  // sorted on method name, then param index
	public TreeMap<String, List<String>> outputInterfaces; // sorted on method name, then param index
	public String name;
}
