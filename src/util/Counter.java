package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Counts occurences of some objects
 * @author Ramon
 * @param <T> the type of objects to count
 */
public class Counter<T> {
	private final Map<T, Integer> map;
	private int highestFrequency;
	private T mostFrequent;
	private int totalNumber;

	public Counter() {
		this.map = new HashMap<>();
		reset();
	}

	public void count(T obj) {
		Integer currentFrequency = map.get(obj);
		if (currentFrequency == null) {
			currentFrequency = 0;
		}
		int newFrequency = currentFrequency + 1;
		map.put(obj, newFrequency);
		if (newFrequency > highestFrequency) {
			highestFrequency = newFrequency;
			this.mostFrequent = obj;
		}
		totalNumber++;
	}

	public T getMostFrequent() {
		return this.mostFrequent;
	}

	public double getHighestFrequencyFraction() {
		if (totalNumber == 0) {
			throw new RuntimeException("Cannot calculate highest frequency without adding any objects");
		}
		return ((double) this.highestFrequency) / this.totalNumber;
	}

	public void reset() {
		this.map.clear();
		this.totalNumber = 0;
		this.highestFrequency = 0;
		this.mostFrequent = null;
	}

	public int getTotalNumber() {
		return this.totalNumber;
	}

	@Override
	public String toString() {
		List<Entry<T, Integer>> entries = new ArrayList<>(this.map.entrySet());
		entries.sort((arg0, arg1) -> -Integer.compare(arg0.getValue(), arg1.getValue()));
		StringBuilder sb = new StringBuilder();
		for (Entry<T, Integer> entry : entries) {
			sb.append(entry.getValue()).append(": ").append(entry.getKey()).append("\n");
		}
		return sb.toString();
	}

	public int getObjectsCounted() {
		return this.map.size();
	}
}
