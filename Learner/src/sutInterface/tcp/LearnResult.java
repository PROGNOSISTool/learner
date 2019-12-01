package sutInterface.tcp;

import de.ls5.jlearn.interfaces.Automaton;

public class LearnResult {
	public long startTime, endTime,  totalTimeMemQueries, totalTimeEquivQueries;
	public int totalMemQueries, totalEquivQueries;
	public Automaton learnedModel;
	
}
