package util;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import learner.Main;

public class TreePruneApp {
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Welcome to the observation tree pruner. Which observationTree do you want to prune?\n(note: it removes it from the original file. Making a back-up is you own task)");
		String fileName = scanner.nextLine();
		ObservationTree tree = null;
		try {
			tree = Main.readCacheTree(fileName);
			if (tree == null) {
			    scanner.close();
				throw new NullPointerException();
			}
		} catch (Exception e) {
			System.out.println("Could not read tree, aborting...");
			System.exit(1);
		}
		System.out.println("Do you want to remove an input (0), an output (1) or an input/output transition(2)");
		int option = Integer.parseInt(scanner.nextLine().trim());
		String msgType = option==1 ? "input" : option == 2? "output" : "transition";
		System.out.println("Which " + msgType + "  do you want to remove? You can use a java-regex, such as '.*PSH.*'. For transitions use input|output");
		String matchPattern = scanner.nextLine();
		BranchMatcher branchMatcher = null;
		switch(option) {
		case 0: branchMatcher = new BranchWithInput(Pattern.compile(matchPattern)); break;
		case 1: branchMatcher = new BranchWithOutput(Pattern.compile(matchPattern)); break;
		default:
		    String [] io = matchPattern.split("\\|");
		    branchMatcher = new BranchWithTransition(Pattern.compile(io[0]), Pattern.compile(io[1]));
		}
		sanitizeBranch(tree, branchMatcher);
		Main.writeCacheTree(tree, true);
		System.out.println("Tree written");
		scanner.close();
	}

	private static void sanitizeBranch(ObservationTree tree, BranchMatcher branchMatcher) {
		Set<String> inputs = new HashSet<String>(tree.getInputs());
		for (String input : inputs) {
		    ObservationTree child = tree.getState(input);
		    String inputString = input.toString();
		    String outputString = tree.getOutput(input).toString();
		    if (branchMatcher.isMatch(inputString, outputString)) {
		        System.out.println("Removing: " + tree.getInputs());
		        child.remove();
		    } else {
		        sanitizeBranch(child, branchMatcher);
		    }
		}
	}

	static class BranchWithTransition implements BranchMatcher{

        private BranchWithInput inputMatcher;
        private BranchWithOutput outputMatcher;

        public BranchWithTransition(Pattern inputMatch, Pattern outputMatch) {
	        this.inputMatcher = new BranchWithInput(inputMatch);
	        this.outputMatcher = new BranchWithOutput(outputMatch);
	    }

        public boolean isMatch(String input, String output) {
            return this.inputMatcher.isMatch(input, output) && this.outputMatcher.isMatch(input, output);
        }

	}



    static class BranchWithInput implements BranchMatcher{
        private Pattern inputMatch;

        public BranchWithInput(Pattern outputMatch) {
            this.inputMatch = outputMatch;
        }

        public boolean isMatch(String input, String output) {
            Matcher m = inputMatch.matcher(input);
            return m.matches();
        }
    }

    static class BranchWithOutput implements BranchMatcher{
	    private Pattern outputMatch;

        public BranchWithOutput(Pattern outputMatch) {
	        this.outputMatch = outputMatch;
	    }

        public boolean isMatch(String input, String output) {
            Matcher m = outputMatch.matcher(output);
            return m.matches();
        }
	}

	static interface BranchMatcher {

	    public boolean isMatch(String input, String output);
	}
}
