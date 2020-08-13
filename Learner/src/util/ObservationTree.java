package util;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import util.exceptions.InconsistencyException;
import util.exceptions.CacheInconsistencyException;
import util.learnlib.WordConverter;

import net.automatalib.words.Word;

public class ObservationTree implements Serializable {
	private static final long serialVersionUID = 6001736L;
	private final ObservationTree parent;
	private final String parentOutput;
	private final Map<String, ObservationTree> children;
	private final Map<String, String> outputs;

	public ObservationTree() {
		this(null, null);
	}

	private ObservationTree(ObservationTree parent, String parentSymbol) {
		this.children = new HashMap<>();
		this.outputs = new HashMap<>();
		this.parent = parent;
		this.parentOutput = parentSymbol;
	}

	public String getOutput(String input) {
		return this.outputs.get(input);
	}

	public ObservationTree getState(String input) {
		return this.children.get(input);
	}

	/**
	 * @return The outputs observed from the root of the tree until this node
	 */
	private List<String> getOutputChain() {
		if (this.parent == null) {
			return new LinkedList<String>();
		} else {
			List<String> parentChain = this.parent.getOutputChain();
			parentChain.add(parentOutput);
			return parentChain;
		}
	}

	public List<String> getInputOutputChain() {
	    if (this.parent == null) {
	        return new LinkedList<String>();
    	} else {
    	    List<String> parentChain = this.parent.getInputOutputChain();
    	    parentChain.add(getInputFromParent());
    	    parentChain.add(parentOutput);
    	    return parentChain;
    	}
	}

	private String getInputFromParent() {
	    if (this.parent != null) {
	        Set<Entry<String, ObservationTree>> set = this.parent.children.entrySet();
	        for (Entry<String, ObservationTree> a : set) {
	            if (a.getValue() == this)
	                return a.getKey();
	        }
	    }
	    return null;
	}



	/**
	 * Add one input and output symbol and traverse the tree to the next node
	 * @param input
	 * @param output
	 * @return the next node
	 * @throws InconsistencyException
	 */
	public ObservationTree addObservation(String input, String output, boolean overwriteOnInconsistency) throws InconsistencyException {
		String previousOutput = this.outputs.get(input);
		boolean createNewBranch = previousOutput == null || overwriteOnInconsistency;
		if (createNewBranch) {
			// input hasn't been queried before, make a new branch for it and traverse
			this.outputs.put(input, output);
			ObservationTree child = new ObservationTree(this, output);
			this.children.put(input, child);
			return child;
		} else if (!previousOutput.equals(output)) {
			// input is inconsistent with previous observations, throw exception
			List<String> oldOutputChain = this.children.get(input).getOutputChain();
			List<String> newOutputChain = this.getOutputChain();
			newOutputChain.add(output);
			/*boolean action = removeOnNonDet; //askForRemoval(oldOutputChain, newOutputChain);
			if (action) {
			    Main.writeCacheTree(this, false);
    			this.children.remove(input);
    			this.outputs.remove(input);
			}*/
			throw new InconsistencyException(Word.fromList(oldOutputChain), Word.fromList(newOutputChain));
		} else {
			// input is consistent with previous observations, just traverse
			return this.children.get(input);
		}
	}

	private boolean askForRemoval(List<String> oldOutputChain, List<String> newOutputChain) {
	    System.out.println("Do you want the input removed from the tree? (1/0)");
	    System.out.println("Old output chain: " + oldOutputChain);
	    System.out.println("New output chain: " + newOutputChain);
	    boolean toRemove = true;
	    try{
        char answer = (char)System.in.read();
        if (answer == '1') {
            System.out.println("OK, removing");
            toRemove = true;
        } else {
            System.out.println("OK, not removing");
            toRemove = false;
        }
	    }catch(IOException e) {
	        System.err.println("IO Exception. Tree gets to leave another day.");
	        System.exit(0);
	    }

	    return toRemove;
	}

	/**
	 * Add Observation to the tree
	 * @param inputs
	 * @param outputs
	 * @throws CacheInconsistencyException Inconsistency between new and stored observations
	 */
	public void addObservation(Word<String> inputs, Word<String> outputs) throws CacheInconsistencyException {
		addObservation(inputs, outputs, false);
	}

	/**
	 * Add Observation to the tree
	 * @param inputs
	 * @param outputs
	 * @param overwriteOnInconsistency If the new observation is inconsistent with stored observations, overwrite the old
	 * ones
	 * @throws CacheInconsistencyException Inconsistency, when not set to overwrite
	 */
	public void addObservation(Word<String> inputs, Word<String> outputs, boolean overwriteOnInconsistency) throws CacheInconsistencyException {
		addObservation(WordConverter.toSymbolList(inputs), WordConverter.toSymbolList(outputs), overwriteOnInconsistency);
	}

	public void addObservation(List<String> inputs, List<String> outputs, boolean overwriteOnInconsistency) throws CacheInconsistencyException {
		if (inputs.isEmpty() && outputs.isEmpty()) {
			return;
		} else if (inputs.isEmpty() || outputs.isEmpty()) {
			throw new RuntimeException("Input and output words should have the same length:\n" + inputs + "\n" + outputs);
		} else {
			String firstInput = inputs.get(0), firstOutput = outputs.get(0);
			try {
				this.addObservation(firstInput, firstOutput, overwriteOnInconsistency)
					.addObservation(inputs.subList(1, inputs.size()), outputs.subList(1, outputs.size()), overwriteOnInconsistency);
			} catch (InconsistencyException e) {
				throw new CacheInconsistencyException(Word.fromList(inputs), e.getOldWord(), Word.fromList(outputs));
			} catch (CacheInconsistencyException e) {
				throw new CacheInconsistencyException(WordConverter.toWord(inputs), e.getOldOutput(), WordConverter.toWord(outputs));
			}
		}
	}

	public Word<String> getObservation(Word<String> inputs) {
		List<String> outputs = getObservation(new LinkedList<>(inputs.asList()));
		if (outputs == null) {
			return null;
		}
		return WordConverter.toWord(outputs);
	}

	public ObservationTree getState(List<String> inputs) {
		if (inputs.isEmpty()) {
			return this;
		} else {
			String firstInput = inputs.get(0);
			ObservationTree child = null;
			if ((child = this.children.get(firstInput)) == null) {
				return null;
			} else {
				return child.getState(inputs.subList(1, inputs.size()));
			}
		}
	}

	public List<String> getObservation(List<String> inputs) {
		if (inputs.isEmpty()) {
			return new LinkedList<>();
		} else {
			String firstInput = inputs.get(0);
			List<String> observationTail = null;
			ObservationTree child = null;
			if ((child = this.children.get(firstInput)) == null
					|| (observationTail = child.getObservation(inputs.subList(1, inputs.size()))) == null) {
				return null;
			} else {
				String firstOutput = this.outputs.get(firstInput);
				observationTail.add(0, firstOutput);
				return observationTail;
			}
		}
	}

	public void remove() {
		if (this.parent == null) {
			throw new RuntimeException("Cannot remove root node");
		}
		for (ObservationTree subTree : this.getAllSubTrees()) {
            System.out.println(subTree.getInputOutputChain());
        }
		for (String symbol : this.parent.children.keySet()) {
			if (this == this.parent.children.get(symbol)) {
				this.parent.children.remove(symbol);
				this.parent.outputs.remove(symbol);
				break;
			}
		}
	}

	public void remove(Word<String> accessSequence) {
		this.remove(WordConverter.toSymbolList(accessSequence));
	}

	private Set<ObservationTree> getAllSubTrees() {
	    if (this.children.isEmpty()) {
	        return new LinkedHashSet<ObservationTree>();
	    } else {
			LinkedHashSet<ObservationTree> childSet = new LinkedHashSet<>(children.values());
	        for (ObservationTree child : new LinkedHashSet<>(childSet)) {
	            childSet.addAll(child.getAllSubTrees());
	        }

	        return childSet;
	    }
	}

	public void remove(List<String> accessSequence) throws RemovalException {

		if (accessSequence.isEmpty()) {
		    System.out.println("Removing: " + getInputOutputChain());
		    System.out.println("Also removing subtrees branches");
		    for (ObservationTree subTree : this.getAllSubTrees()) {
		        System.out.println(subTree.getInputOutputChain());
		    }
			this.remove();
		} else {
			ObservationTree child = this.children.get(accessSequence.get(0));
			if (child == null) {
				throw new RemovalException("Cannot remove branch which is not present for input\n" + accessSequence);
			}
			try {
				child.remove(accessSequence.subList(1, accessSequence.size()));
			} catch (RemovalException e) {
				throw new RemovalException("Cannot remove branch which is not present for input\n" + accessSequence, e);
			}
		}
	}

	public static class RemovalException extends RuntimeException {
		/**
         *
         */
        private static final long serialVersionUID = 1L;

        public RemovalException() {
			super();
		}

		public RemovalException(String msg) {
			super(msg);
		}

		public RemovalException(String msg, Exception e) {
			super(msg, e);
		}
	}

	public Set<String> getInputs() {
		return this.children.keySet();
	}

	public int getDepth() {
		int max = 1;
		for (ObservationTree child : this.children.values()) {
			max = Math.max(max, child.getDepth());
		}
		return max + 1;
	}

	public static void main(String[] args) {
		ObservationTree tree = new ObservationTree();
		LinkedList<String> in = new LinkedList<String>(), out1 = new LinkedList<>(), out2 = new LinkedList<String>();
		in.add("a");
		in.add("b");
		in.add("c");
		out1.add("x");
		out1.add("y");
		out1.add("z");
		out2.add("x");
		out2.add("z");
		out2.add("z");
		try {
			tree.addObservation(WordConverter.toWord(in), WordConverter.toWord(out1));
			tree.addObservation(WordConverter.toWord(in), WordConverter.toWord(out2));
		} catch (CacheInconsistencyException e) {
			System.err.println(e.getInput());
			System.err.println(e.getOldOutput());
			System.err.println(e.getNewOutput());
			System.err.println(e.getShortestInconsistentInput());
			e.printStackTrace();
		}
	}
}
