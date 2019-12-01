package sutInterface.tcp.init;

import java.util.ArrayList;
import java.util.List;

import learner.ExtendedOracle;

import sutInterface.tcp.Flag;
import sutInterface.tcp.FlagSet;
import sutInterface.tcp.Internal;
import util.Log;
import util.exceptions.BugException;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

/**
 * Implementation of an oracle which resolves the init state of the query before executing.
 * Unfortunately, because it has no interaction with the TCP mapper, it is difficult to enable it
 * to clean used connections.
 */
public class AdaptiveTCPOracleWrapper implements Oracle {

	private static final long serialVersionUID = 1L;
	private ExtendedOracle basicOracle;
	private InitCacheManager cacheManager;
	// stores the last output before the distinguishing input is applied, so 
	// we don't have run the trace all over again
	private Word lastOutputWordBeforeExtension;
	// stores the last init value, used when processing the "non changer inputs"
	private boolean lastInitValue;

	public AdaptiveTCPOracleWrapper(ExtendedOracle oracle, InitCacheManager cacheManager) {
		this.basicOracle = oracle;
		this.cacheManager = cacheManager;
	}
	
	
	private Word runTrace(Word input) throws LearningException {
		Word output = basicOracle.processQuery(input);
		return output;
	}

	public Word processQuery(Word input) throws LearningException {
		Word output = null;
		updateInitForAllSubtraces(input);
		
		// we might already have the output word from the update init processing
		if (lastOutputWordBeforeExtension == null || 
				lastOutputWordBeforeExtension.size() < input.size()) {
			output =  runTrace(input);
		} else {
			output =  lastOutputWordBeforeExtension;
		}
		
		if(input.size() != output.size()) {
			throw new BugException(input + " has different size compared to " + output);
		}

		return output;
	}
	
	private void updateInitForAllSubtraces(Word input) throws LearningException{
		this.lastInitValue = true; // by default init is true
		this.lastOutputWordBeforeExtension = null;
		List<String> subTrace = new ArrayList<String>();
		
		/* We verify that at each point when executing the query we know the "is listening" value. 
		 * In case we don't, we obtain it by applying the distinguishing input SYN(V,V) */  
		for(Symbol inputSymbol : input.getSymbolArray()) {
			subTrace.add(inputSymbol.toString());
			Log.info("Fetching init for trace " + subTrace);
			boolean init = getInitForTrace(subTrace);
			cacheManager.storeTrace(subTrace, init); 
			Log.info("Storing: " + init);
		}
	}
	
	// return whether the server is in the listening state after executing the given trace
	private boolean getInitForTrace(List<String> subTrace) throws LearningException{
		boolean init;
		//if (isChangeCandidate(subTrace)) {;
		init = getInitForChangeCandidate(subTrace);
//		} else {
//			init = getInitForNonChanger(subTrace);
//		}
		this.lastInitValue = init;
		return init;
	}
	
	// used to filter out inputs that, when applied, can not change the "is listening" state 
	private boolean isChangeCandidate(List<String> subTrace) {
		String lastInput = subTrace.get(subTrace.size()-1);
		int flagsDelim = lastInput.indexOf('(');
		String flagStrings = lastInput.substring(0, flagsDelim);
		FlagSet flags = new FlagSet(flagStrings);
		return flags.has(Flag.SYN) || flags.has(Flag.RST) ;
	}

	// getting the "is listening" state for a non changer does not require a asking a query, thus
	// it speeds up the inefficient oracle
	private boolean getInitForNonChanger(List<String> subTrace) throws LearningException{
		boolean init = this.lastInitValue;
		return init;
	}

	private boolean getInitForChangeCandidate(List<String> subTrace) throws LearningException{
		String distInput = "SYN(V,V)";
		List<String> extendedTrace = new ArrayList<String>(subTrace);
		extendedTrace.add(distInput);
		String lastOutput = runExtendedTrace(extendedTrace);
		Log.info("extended trace output: " + lastOutput);
		String distOutputExpr = "((ACK\\+SYN)|(SYN\\+ACK)).*"; //\\(FRESH,(?!FRESH).*"; // hard
		boolean isResetting = lastOutput.matches(distOutputExpr);
		if (isResetting == false) {
			basicOracle.sendInput(Internal.REVERT.toString());
		}
		return isResetting;
	}
	
	private String runExtendedTrace(List<String> extendedTrace)  throws LearningException {
		Word extendedOutputword = buildWord(extendedTrace);
		Word outputWord = runTrace(extendedOutputword);
		if (outputWord.getSymbolArray().length < 2) { 
			throw new BugException("Invalid trace given"); 
		}
		List<Symbol> outputSymbols = new ArrayList<Symbol>(outputWord.getSymbolList());
		outputSymbols.remove(outputSymbols.size()-1);
		lastOutputWordBeforeExtension = buildWordS(outputSymbols);
		return outputWord.getSymbolByIndex(outputWord.size()-1).toString();
	}
	
	// methods used to translate between strings and LearnLib words
	
	private Word buildWord(List<String> wordInputs) {
		Word word = new WordImpl();
		for(String wordInput : wordInputs) {
			word.addSymbol(new SymbolImpl(wordInput));
		}
		return word;
	}
	
	private Word buildWordS(List<Symbol> wordInputs) {
		Word word = new WordImpl();
		for(Symbol wordInput : wordInputs) {
			word.addSymbol(new SymbolImpl(wordInput));
		}
		return word;
	}

}
