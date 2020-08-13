package debug;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;

import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.shared.SymbolImpl;

import learner.Main;
import util.ObservationTree;

public class CacheViewer {
	private static ObservationTree root, current;
	private static boolean exit = false;
	private static Scanner scanner;

	public static void main(String[] args) {
	 	try {
	 		scanner = new Scanner(System.in);
	 		System.out.println("Please give the filename of the tree (empty line for standard file)");
	 		String fileName = scanner.nextLine();
	 		if (fileName.isEmpty()) {
				root = Main.readCacheTree(Main.CACHE_FILE);
	 		} else {
	 			root = Main.readCacheTree(fileName);
	 		}
			if (root == null) {
				System.err.println("Cannot read tree, aborting");
				System.exit(1);
			} else {
				System.out.println("Succesfully read file");
			}
			current = root;
			while(!exit && scanner.hasNextLine()) {
				process(scanner.nextLine());
			}
		} finally {
			System.out.println("Aborting");
			scanner.close();
		}
	}

	private static void process(String arg) {
		switch(arg) {
		case "exit":
		case "quit":
		case "stop":
			exit = true;
			break;
		case "reset":
			current = root;
			break;
		case "remove":
			if (current == root) {
				System.out.println("Cannot remove root");
			} else {
				current.remove();
				current = root;
			}
			break;
		case "save":
		case "store":
			//System.out.println("Does not currently work, cache viewer doesn't support multiple trees yet");
			Main.writeCacheTree(root, true);
			break;
		case "view":
			System.out.println(current.getInputs());
			break;
		/*case "find":
			System.out.println("Type a regex to search for");
			if (scanner.hasNextLine()) {
				current.find(scanner.nextLine());
			} else {
				System.err.print("no regex supplied");
			}
			break;*/
		case "depth":
			System.out.println(current.getDepth());
			break;
	    case "trace":
	        List<String> trace = TraceRunner.readTraceAndIterations().tuple0;
	       // Predicate<String> nonInputFilter = s -> s.startsWith("!") || s.startsWith("#");
	      //  trace.removeIf(nonInputFilter);
	        for (String input : trace) {
	            if ( ! input.startsWith("!") && ! input.startsWith("#")) {
	                if (! applyInput(input)) {
	                    break;
	                }
	            }
	        }
	        break;
		default:
		    applyInput(arg);
			break;
		}
	}



	private static boolean applyInput(String arg) {
	    LinkedList<String> input = new LinkedList<>();
	    input.add(arg);
        ObservationTree child = current.getState(input);
        if (child == null) {
            System.out.println("No input '" + arg + "' in the current node");
            return false;
        } else {
            System.out.println("output '" + current.getObservation(input).get(0) + "'");
            current = child;
            return true;
        }
	}
}
