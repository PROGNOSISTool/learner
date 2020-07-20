package sutInterface.quic;

public class ReducedIntRange {
	public final int reducedStart, start, length;


	public ReducedIntRange(int reducedStart, int start, int length) {
		this.reducedStart = reducedStart;
		this.length = length;
		this.start = start;
	}

	public int reduce(int value) {
		if (!isInRange(value)) {
			throw new RuntimeException("Reduced int range misused: requested value is not in range [" + start + ", " + (start + length) + ")");
		}
		int reducedValue = value - start;
		return reducedValue;
	}

	public int expand(int reducedValue) {
		if (!isReducedInRange(reducedValue)) {
			throw new RuntimeException("Cannot expand " + reducedValue + ": not in range [" + start + "," + (start+length) + ")");
		}
		return start + (reducedValue - reducedStart);
	}

	public boolean isInRange(int value) {
		return value >= start && value < start + length;
	}

	public boolean isReducedInRange(int reducedValue) {
		return reducedValue >= reducedStart && reducedValue < reducedStart + length;
	}
}
