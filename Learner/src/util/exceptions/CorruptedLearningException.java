package util.exceptions;

import de.ls5.jlearn.abstractclasses.LearningException;

/**
 * Used to denote that the learning process has been corrupted with wrong
 * information, e.g. an output that was already processed by the learner
 * is found to be erroneous
 *
 */
public class CorruptedLearningException extends LearningException {
	private static final long serialVersionUID = 3618393099207301314L;
	
	public CorruptedLearningException(String msg) {
		super(msg);
	}
	
	public CorruptedLearningException() {
		super();
	}
}
