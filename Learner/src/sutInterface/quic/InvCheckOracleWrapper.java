package sutInterface.quic;

import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Word;
import util.exceptions.BugException;

import java.util.ArrayList;
import java.util.List;

public class InvCheckOracleWrapper implements Oracle{
	private static final long serialVersionUID = 1L;
	private Oracle oracle;

	public InvCheckOracleWrapper(Oracle oracle) {
		this.oracle = oracle;
	}

	@Override
	public Word processQuery(Word input) throws LearningException {
		Word output =  this.oracle.processQuery(input);
		if(!invCheck(output)) {
			throw new BugException("Detected invalid in output trace " + output);
		}
		return output;
	}

	private boolean invCheck(Word word) {
		boolean noInv = true;
		for(String message : toMessages(word)) {
			if(message.contains("INV")) {
				noInv = false;
				break;
			}
		}
		return noInv;
	}

	private List<String> toMessages(Word word) {
		List<String> inputs = new ArrayList<String>();
		for (de.ls5.jlearn.interfaces.Symbol symbol : word.getSymbolArray()) {
			inputs.add(symbol.toString());
		}
		return inputs;
	}
}
