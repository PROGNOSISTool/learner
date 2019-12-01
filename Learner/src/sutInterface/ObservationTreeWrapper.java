package sutInterface;

import java.io.Serializable;

import util.ObservationTree;
import util.exceptions.InconsistencyException;
import util.exceptions.CacheInconsistencyException;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Word;

/**
 * Creates a consistent observation tree. Implies that it checks for non-determinism.
 * @author Ramon
 */
public class ObservationTreeWrapper implements Oracle, Serializable {
	private static final long serialVersionUID = 71137973L;
	
	private final Oracle oracle;
	private final ObservationTree tree;
	
	public ObservationTreeWrapper(Oracle oracle) {
		this(new ObservationTree(), oracle);
	}
	
	public ObservationTreeWrapper(ObservationTree tree, Oracle oracle) {
		this.oracle = oracle;
		this.tree = tree;
	}

	@Override
	public Word processQuery(Word input) throws LearningException {
		Word output = this.oracle.processQuery(input);
		this.tree.addObservation(input, output);
		return output;
	}
	
	public ObservationTree getObservationTree() {
		return tree;
	}
}
