package learner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Statistics {
	private static Statistics stats = new Statistics();
	
	public static Statistics getStats() {
		return stats;
	}
	
	public long startTime = 0;
	public long endTime = 0;
	public int totalEquivQueries = 0;
	public int totalMemQueries = 0;
	public int totalUniqueEquivQueries = 0;
	public int totalTimeMemQueries = 0;
	public int totalTimeEquivQueries = 0;
	public int totalQueriesSavedByPartialOracle = 0;
	public int totalAdditionalQueriesByAdaptiveOracle = 0;
	public int runs = 0;
	private List<Integer> nrHypothesisEquivalenceQueries = new ArrayList<>();
	
	public void printStats(PrintStream statsOut) {
		statsOut.println("");
		statsOut.println("");
		statsOut.println("		STATISTICS SUMMARY:");
		statsOut.println("Total running time: " + (endTime - startTime)
				+ "ms.");
		statsOut.println("Total time Membership queries: "
				+ totalTimeMemQueries);
		statsOut.println("Total time Equivalence queries: "
				+ totalTimeEquivQueries);
		statsOut.println("Total number of runs: "
				+ runs);
		statsOut.println("Total Membership queries: "
				+ totalMemQueries);
		statsOut.println("Total Equivalence queries: "
				+ totalEquivQueries);
		statsOut.println("Total unique Equivalence queries: "
				+ totalUniqueEquivQueries);
		
		if (!this.nrHypothesisEquivalenceQueries.isEmpty()) {
			statsOut.println("Number of equivalence queries per hypothesis:");
			for (int i = 0; i < this.nrHypothesisEquivalenceQueries.size(); i++) {
				statsOut.println("hyp " + i + ": " + this.nrHypothesisEquivalenceQueries.get(i));
			}
			statsOut.println();
		}
		
		if (totalQueriesSavedByPartialOracle != 0) {
			statsOut
			.println("Total queries that were not executed because of the Partial Oracle: "
					+ totalQueriesSavedByPartialOracle);
		}
		
		if (totalAdditionalQueriesByAdaptiveOracle != 0) {
			statsOut
			.println("Total queries that were executed because of the Adaptive Oracle: "
					+ totalAdditionalQueriesByAdaptiveOracle);
		}
	}

	public void addNrHypothesisEquivalenceQueries(int nrHypthesisTests) {
		this.nrHypothesisEquivalenceQueries.add(nrHypthesisTests);
	}
}
