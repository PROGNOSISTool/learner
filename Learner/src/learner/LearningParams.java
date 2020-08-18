package learner;

import de.learnlib.api.logging.LearnLogger;

import java.io.PrintStream;
import java.util.List;

public class LearningParams {
	// params for learning
	public String sutInterface;
	public String algorithm = "Angluin";
	public List<List<String>> testTraces = null;
	public String yanCommand = null;
	public String yanCommand2 = null;
	public int maxValue;
	public int minValue;
	public long seed;
	public int maxNumTraces;
	public int minTraceLength;
	public int maxTraceLength;
	public String mapper;
	public int nonDeterminismTestIterations;
	public String equivalenceCriterion;
	public String yanMode;

	public void printParams(LearnLogger logger) {
		String seedStr = Long.toString(seed) + " - Set statically";

		logger.logConfig("Maximum number of traces: " + this.maxNumTraces);
		logger.logConfig("Minimum length of traces: " + this.minTraceLength);
		logger.logConfig("Maximim length of traces: " + this.maxTraceLength);
		logger.logConfig("Mapper: " + this.mapper);
		logger.logConfig("Seed: " + seedStr);
	}
}
