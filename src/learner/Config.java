package learner;

import java.util.ArrayList;
import java.util.List;

public class Config {

	public String impIP;
	public int impPort;
	public String framework;
	public String algorithm;
	public String tester;

	public int runsPerQuery = 1;
	public int confidence = 100;
	public int maxAttempts = 100;

	public List<String> inputAlphabet = new ArrayList<>();
}
