package learner;

import de.learnlib.api.logging.LearnLogger;

import java.io.PrintStream;

/***
 * Configuration parameters used for learning TCP.
 * Note:
 * 	SUT- system under test, in the TCP case study it refers
 * 		 the listening Python adapter, which itself has the
 * 		 ip and port of the TPC server
 */
public class SULConfig {

	/**
	 * The IP address of the SUT.
	 */
	public String sutIP = "127.0.0.1";

	/**
	 * The port of the SUT.
	 */
	public int sutPort = 3333;


	public int runsPerQuery = 1;
    public int confidence = 100;
	public int maxAttempts = 100;

	/**
	 * Escape in case one of the numbers received from server cannot be matched, signaling that learning is out-of-synch
	 */
	public boolean exitIfInvalid = true;

	public void printParams(LearnLogger logger) {
		logger.logConfig(String.format("SUT endpoint: (%s,%s)\n", this.sutIP, String.valueOf(this.sutPort)));
	}
}
