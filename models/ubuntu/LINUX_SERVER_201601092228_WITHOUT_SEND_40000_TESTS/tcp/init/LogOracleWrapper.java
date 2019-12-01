package sutInterface.tcp.init;

import util.Container;
import learner.Main;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Word;

public class LogOracleWrapper  implements Oracle{
	private static final long serialVersionUID = 1L;
	private final Oracle oracle;
	
	public LogOracleWrapper(Oracle oracle) {
		this.oracle = oracle;
	}
	
	@Override
	public Word processQuery(Word input) throws LearningException {
		Word output = this.oracle.processQuery(input);
		for(int i = 0; i < input.getSymbolArray().length; i ++) {
			Main.absTraceOut.println(input.getSymbolArray()[i].toString());
			Main.absTraceOut.println("!" + output.getSymbolArray()[i].toString());
		}
		Main.absTraceOut.println("reset");
		return output;
	}
}
