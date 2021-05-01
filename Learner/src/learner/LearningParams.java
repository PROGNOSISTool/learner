package learner;

import de.learnlib.api.logging.LearnLogger;

import java.util.List;
import java.util.TreeMap;

public class LearningParams {
	// params for learning
	public String sutInterface;
	public int maxNumTraces;
	public int minTraceLength;
	public int maxTraceLength;
    public TreeMap<String, List<String>> inputAlphabet;

	public void printParams() {
		LearnLogger logger = LearnLogger.getLogger("Learner");
		logger.logConfig("Maximum number of traces: " + this.maxNumTraces);
		logger.logConfig("Minimum length of traces: " + this.minTraceLength);
		logger.logConfig("Maximim length of traces: " + this.maxTraceLength);
        logger.logConfig("Input alphabet: " + this.inputAlphabet);
	}
}
