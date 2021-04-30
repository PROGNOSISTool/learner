package learner;

import de.learnlib.api.logging.LearnLogger;

import java.io.PrintStream;
import java.util.List;

public class LearningParams {
	// params for learning
	public String sutInterface;
	public long seed;
	public int maxNumTraces;
	public int minTraceLength;
	public int maxTraceLength;
	public String mapper;

	public void printParams() {
		String seedStr = seed + " - Set statically";
		LearnLogger logger = LearnLogger.getLogger("Learner");
		logger.logConfig("Maximum number of traces: " + this.maxNumTraces);
		logger.logConfig("Minimum length of traces: " + this.minTraceLength);
		logger.logConfig("Maximim length of traces: " + this.maxTraceLength);
		logger.logConfig("Mapper: " + this.mapper);
		logger.logConfig("Seed: " + seedStr);
	}
}
