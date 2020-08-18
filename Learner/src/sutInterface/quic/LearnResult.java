package sutInterface.quic;

import net.automatalib.automata.transducers.MealyMachine;

public class LearnResult {
	public long startTime, endTime,  totalTimeMemQueries, totalTimeEquivQueries;
	public int totalMemQueries, totalEquivQueries;
	public MealyMachine<?, String, ?, String> learnedModel;

}
