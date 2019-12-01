package util;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import learner.Main;

import util.exceptions.InconsistencyException;
import util.exceptions.CacheInconsistencyException;
import util.learnlib.WordConverter;

import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

public class ObservationTree implements Serializable {
	private static final long serialVersionUID = 6001736L;
	private final ObservationTree parent;
	private final Symbol parentOutput;
	private final Map<Symbol, ObservationTree> children;
	private final Map<Symbol, Symbol> outputs;
	
	public ObservationTree() {
		this(null, null);
	}
	
	private ObservationTree(ObservationTree parent, Symbol parentSymbol) {
		this.children = new HashMap<>();
		this.outputs = new HashMap<>();
		this.parent = parent;
		this.parentOutput = parentSymbol;
	}
	
	public Symbol getOutput(Symbol input) {
		return this.outputs.get(input);
	}
	
	public ObservationTree getState(Symbol input) {
		return this.children.get(input);
	}
	
	/**
	 * @return The outputs observed from the root of the tree until this node
	 */
	private List<Symbol> getOutputChain() {
		if (this.parent == null) {
			return new LinkedList<Symbol>();
		} else {
			List<Symbol> parentChain = this.parent.getOutputChain();
			parentChain.add(parentOutput);
			return parentChain;
		}
	}
	
	public List<Symbol> getInputOutputChain() {
	    if (this.parent == null) {
	        return new LinkedList<Symbol>();
    	} else {
    	    List<Symbol> parentChain = this.parent.getInputOutputChain();
    	    parentChain.add(getInputFromParent());
    	    parentChain.add(parentOutput);
    	    return parentChain;
    	}
	}
	
	private Symbol getInputFromParent() {
	    if (this.parent != null) {
	        Set<Entry<Symbol, ObservationTree>> set = this.parent.children.entrySet();
	        for (Entry<Symbol, ObservationTree> a : set) {
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
	public ObservationTree addObservation(Symbol input, Symbol output, boolean overwriteOnInconsistency) throws InconsistencyException {
		Symbol previousOutput = this.outputs.get(input);
		boolean createNewBranch = previousOutput == null || overwriteOnInconsistency;
		if (createNewBranch) {
			// input hasn't been queried before, make a new branch for it and traverse
			this.outputs.put(input, output);
			ObservationTree child = new ObservationTree(this, output);
			this.children.put(input, child);
			return child;
		} else if (!previousOutput.equals(output)) {
			// input is inconsistent with previous observations, throw exception
			List<Symbol> oldOutputChain = this.children.get(input).getOutputChain();
			List<Symbol> newOutputChain = this.getOutputChain();
			newOutputChain.add(output);
			/*boolean action = removeOnNonDet; //askForRemoval(oldOutputChain, newOutputChain);
			if (action) {
			    Main.writeCacheTree(this, false);
    			this.children.remove(input);
    			this.outputs.remove(input);
			}*/
			throw new InconsistencyException(WordConverter.toWord(oldOutputChain), WordConverter.toWord(newOutputChain));
		} else {
			// input is consistent with previous observations, just traverse
			return this.children.get(input);
		}
	}
	
	private boolean askForRemoval(List<Symbol> oldOutputChain, List<Symbol> newOutputChain) {
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
	public void addObservation(Word inputs, Word outputs) throws CacheInconsistencyException {
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
	public void addObservation(Word inputs, Word outputs, boolean overwriteOnInconsistency) throws CacheInconsistencyException {
		addObservation(WordConverter.toSymbolList(inputs), WordConverter.toSymbolList(outputs), overwriteOnInconsistency);
	}
	
	public void addObservation(List<Symbol> inputs, List<Symbol> outputs, boolean overwriteOnInconsistency) throws CacheInconsistencyException {
		if (inputs.isEmpty() && outputs.isEmpty()) {
			return;
		} else if (inputs.isEmpty() || outputs.isEmpty()) {
			throw new RuntimeException("Input and output words should have the same length:\n" + inputs + "\n" + outputs);
		} else {
			Symbol firstInput = inputs.get(0), firstOutput = outputs.get(0);
			try {
				this.addObservation(firstInput, firstOutput, overwriteOnInconsistency)
					.addObservation(inputs.subList(1, inputs.size()), outputs.subList(1, outputs.size()), overwriteOnInconsistency);
			} catch (InconsistencyException e) {
				throw new CacheInconsistencyException(WordConverter.toWord(inputs), e.oldWord, WordConverter.toWord(outputs));
			} catch (CacheInconsistencyException e) {
				throw new CacheInconsistencyException(WordConverter.toWord(inputs), e.getOldOutput(), WordConverter.toWord(outputs));
			}
		}
	}
	
	public Word getObservation(Word inputs) {
		List<Symbol> outputs = getObservation(new LinkedList<>(inputs.getSymbolList()));
		if (outputs == null) {
			return null;
		}
		return WordConverter.toWord(outputs);
	}
	
	public ObservationTree getState(List<Symbol> inputs) {
		if (inputs.isEmpty()) {
			return this;
		} else {
			Symbol firstInput = inputs.get(0);
			ObservationTree child = null;
			if ((child = this.children.get(firstInput)) == null) {
				return null;
			} else {
				return child.getState(inputs.subList(1, inputs.size()));
			}
		}
	}
	
	public List<Symbol> getObservation(List<Symbol> inputs) {
		if (inputs.isEmpty()) {
			return new LinkedList<>();
		} else {
			Symbol firstInput = inputs.get(0);
			List<Symbol> observationTail = null;
			ObservationTree child = null;
			if ((child = this.children.get(firstInput)) == null
					|| (observationTail = child.getObservation(inputs.subList(1, inputs.size()))) == null) {
				return null;
			} else {
				Symbol firstOutput = this.outputs.get(firstInput);
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
		for (Symbol symbol : this.parent.children.keySet()) {
			if (this == this.parent.children.get(symbol)) {
				this.parent.children.remove(symbol);
				this.parent.outputs.remove(symbol);
				break;
			}
		}
	}
	
	public void remove(Word accesSequence) {
		this.remove(WordConverter.toSymbolList(accesSequence));
	}
	
	private Set<ObservationTree> getAllSubTrees() {
	    if (this.children.isEmpty()) {
	        return new LinkedHashSet<ObservationTree>();
	    } else {
	        LinkedHashSet<ObservationTree> childSet = new LinkedHashSet<ObservationTree>(children.values());
	        
	        for (ObservationTree child : childSet.toArray(new ObservationTree [childSet.size()])) {
	            childSet.addAll(child.getAllSubTrees());
	        }
	        
	        return childSet;
	    }
	}
	
	public void remove(List<Symbol> accessSequence) throws RemovalException {
	    
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
	
	public class RemovalException extends RuntimeException {
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
	
	public Set<Symbol> getInputs() {
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
		LinkedList<Symbol> in = new LinkedList<Symbol>(), out1 = new LinkedList<>(), out2 = new LinkedList<Symbol>();
		in.add(new SymbolImpl("a"));
		in.add(new SymbolImpl("b"));
		in.add(new SymbolImpl("c"));
		out1.add(new SymbolImpl("x"));
		out1.add(new SymbolImpl("y"));
		out1.add(new SymbolImpl("z"));
		out2.add(new SymbolImpl("x"));
		out2.add(new SymbolImpl("z"));
		out2.add(new SymbolImpl("z"));
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
