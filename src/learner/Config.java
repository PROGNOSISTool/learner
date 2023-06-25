package learner;

import java.util.ArrayList;
import java.util.List;

public class Config {

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

	public List<String> inputAlphabet = new ArrayList<>();

}
