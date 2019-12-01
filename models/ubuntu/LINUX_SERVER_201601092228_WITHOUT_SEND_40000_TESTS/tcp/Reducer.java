package sutInterface.tcp;

import java.util.ArrayList;
import java.util.List;

public class Reducer {
	private static final int RANGE_LENGTH = 8, RANGE_START = 2, INITIAL_START = 10;
	List<ReducedIntRange> ranges = new ArrayList<>();
	int newRangeStart = INITIAL_START;
	
	public int reduce(int value) {
		if (value < INITIAL_START && value >= InvlangMapper.NOT_SET) {
			return value;
		}
		for (ReducedIntRange range : ranges) {
			if (range.isInRange(value)) {
				return range.reduce(value);
			}
		}
		ReducedIntRange range = new ReducedIntRange(newRangeStart, value - RANGE_START, RANGE_LENGTH);
		this.newRangeStart += RANGE_LENGTH;
		return range.reduce(value);
	}
	
	public int expand(int reducedValue) {
		if (reducedValue < INITIAL_START && reducedValue >= InvlangMapper.NOT_SET) {
			return reducedValue;
		}
		for (ReducedIntRange range : ranges) {
			if (range.isReducedInRange(reducedValue)) {
				return range.expand(reducedValue);
			}
		}
		throw new RuntimeException("Cannot expand a value which does not come from a reduced range");
	}
}
