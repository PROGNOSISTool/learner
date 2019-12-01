package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

public class RangePicker {
	private static final long INTEREST_LOWER_OFFSET = 0, INTEREST_UPPER_OFFSET = 1;
	private final long min, max;
	private final List<Tuple2<Long, Long>> rangesOfInterest;
	private final Random r;
	
	public RangePicker(Random random, long min, long max, List<Long> pointsOfInterest) {
		this.r = random;
		this.min = min;
		this.max = max;
		pointsOfInterest = new ArrayList<>(pointsOfInterest);
		List<Long> boundaryValues = new ArrayList<>();
		for (long l : pointsOfInterest) {
			long low = l-INTEREST_LOWER_OFFSET, high = l+INTEREST_UPPER_OFFSET;
			if (low >= min && low <= max) {
				boundaryValues.add(low);
			}
			if (high >= min && high <= max) {
				boundaryValues.add(high);
			}
		}
		pointsOfInterest.addAll(boundaryValues);
		
		Collections.sort(pointsOfInterest);
		removeDuplicatesFromSorted(pointsOfInterest);
		if (this.min > pointsOfInterest.get(0) || this.max < pointsOfInterest.get(pointsOfInterest.size()-1)) {
			throw new RuntimeException("Range (" + pointsOfInterest +
					") should have its values between min (" + this.min + ") and max (" + this.max + ")");
		}
		this.rangesOfInterest = computeRangesFromSorted(pointsOfInterest);
	}
	
	private List<Tuple2<Long, Long>> computeRangesFromSorted(List<Long> pointsOfInterest) {
		ArrayList<Tuple2<Long, Long>> ranges = new ArrayList<>();
		long rangeStart = this.min;
		for (long i : pointsOfInterest) {
			long rangeEnd = i - 1;
			if (rangeEnd >= rangeStart) {
				ranges.add(new Tuple2<>(rangeStart, rangeEnd));
			}
			rangeStart = i + 1;
			ranges.add(new Tuple2<>(i, i));
		}
		long rangeEnd = this.max - 1;
		if (rangeEnd >= rangeStart) {
			ranges.add(new Tuple2<>(rangeStart, rangeEnd));
		}
		/*ListIterator<Long> it = pointsOfInterest.listIterator();
		while(it.hasNext()) {
			long poi = it.next();
			long rangeEnd = poi - 1;
			if (rangeStart < rangeEnd) {
				ranges.add(new Tuple2<Long, Long>(rangeStart, rangeEnd));
			} else if (rangeStart == rangeEnd) {
				it.previous();
				it.add(rangeStart);
				it.next();
			}
			rangeStart = poi + 1;
		}
		if (rangeStart < max) {
			ranges.add(new Tuple2<Long, Long>(rangeStart, max));
		} else if (rangeStart == max) {
			pointsOfInterest.add(rangeStart);
		}*/
		return ranges;
	}

	private static void removeDuplicatesFromSorted(List<Long> longs) {
		if (longs.isEmpty()) {
			return;
		}
		ListIterator<Long> it = longs.listIterator();
		long prev = it.next();
		while (it.hasNext()) {
			long next = it.next();
			if (next == prev) {
				it.remove();
			} else {
				prev = next;
			}
		}
	}
	
	public long getRandom() {
		return getRandom(r.nextInt(this.rangesOfInterest.size()));
	}
	
	/**
	 * Get a random number from a range of interest specified by this index
	 * @param i
	 * @return
	 */
	public long getRandom(int i) {
		Tuple2<Long, Long> rangeOfInterest = this.rangesOfInterest.get(i);
		return Rand.nextLong(r, rangeOfInterest.tuple1 - rangeOfInterest.tuple0 + 1) + rangeOfInterest.tuple0;
	}
	
	public Tuple2<Long, Long> getRangeOfInterest(int index) {
		return this.rangesOfInterest.get(index);
	}
	
	public boolean valueIsInRangeOfInterest(long value, int index) {
		Tuple2<Long, Long> range = getRangeOfInterest(index);
		return value >= range.tuple0 && value <= range.tuple1;
	}
	
	public boolean valueIsRangeOfInterest(long value, int index) {
		Tuple2<Long, Long> range = getRangeOfInterest(index);
		return value == range.tuple0 && value == range.tuple1;
	}
	
	/**
	 * The number of points/ranges of interest
	 * @return
	 */
	public int getNumberOfRanges() {
		return this.rangesOfInterest.size();
	}
}
