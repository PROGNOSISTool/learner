package learner;

import java.io.PrintStream;

/***
 * Configuration parameters used for learning TCP.
 * Note:
 * 	SUT- system under test, in the TCP case study it refers
 * 		 the listening Python adapter, which itself has the
 * 		 ip and port of the TPC server
 */
public class SUTParams {

	/**
	 * The IP address of the SUT.
	 */
	public String sutIP = "127.0.0.1";

	/**
	 * The port of the SUT.
	 */
	public int sutPort = 3333;

	/**
	 * if this is true, output is written to the console,
	 * containing information about the inputs sent and outputs received
	 */
	public boolean verbose = false;

	/**
	 * this file is used to load trace init data.
	 */
	public String cacheFile = "cache.txt";

	/**
	 * If the oracle is adaptive, then for each trace of form:
	 * i1 ... in
	 * We take every subtrace:
	 * i1
	 * i1 i2
	 * ...
	 * And check if after issuing each subtrace the sut is left in the initial state (LISTENING).
	 * This is done before sending executing the membership/equiv queries.
	 */
	public String oracle = "adaptive";

	public int runsPerQuery = 1;
    public int confidence = 100;

	/**
	 * Escape in case one of the numbers received from server cannot be matched, signaling that learning is out-of-synch
	 */
	public boolean exitIfInvalid = true;

	public String logLevel = "INFO";

	public void printParams(PrintStream stdout) {
		stdout.printf("SUT endpoint: (%s,%s)\n", this.sutIP, String.valueOf(this.sutPort));
		stdout.println("Reset oracle: " + oracle);
	}
}
