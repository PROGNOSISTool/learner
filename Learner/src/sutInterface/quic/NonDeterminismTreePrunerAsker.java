package sutInterface.quic;

import de.learnlib.api.oracle.MembershipOracle;
import learner.Main;
import net.automatalib.words.Word;
import util.ObservationTree;
import util.exceptions.CacheInconsistencyException;

import java.util.Scanner;

public class NonDeterminismTreePrunerAsker extends NonDeterminismTreePruner {
	public NonDeterminismTreePrunerAsker(ObservationTree tree, MembershipOracle<String, Word<String>> oracle) {
		super(tree, oracle);
	}

	protected void prune(CacheInconsistencyException cacheException) {
		Scanner scanner = new Scanner(System.in);
		Main.writeCacheTree(tree, false);
		loop:
		while(true) {
			cacheException.printStackTrace();
			System.err.println("Found inconsistency, remove trace from tree?");
			String line = scanner.nextLine().toLowerCase();
			switch(line) {
			case "yes": case "y":
		        tree.remove(cacheException.getShortestInconsistentInput());
				break loop;
			case "no": case "n":
				break loop;
			}
		}
		scanner.close();
	}
}
