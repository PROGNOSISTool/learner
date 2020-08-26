package util;

import net.automatalib.words.Word;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SetOps {
	public static Set<Word<String>> difference(Set<Word<String>> A, Set<Word<String>> B) {
		Set<Word<String>> result = new HashSet<>(A);
		result.removeAll(B);
		return result;
	}

	public static Set<Word<String>> intersection(Set<Word<String>> A, Set<Word<String>> B) {
		Set<Word<String>> result = new HashSet<>(A);
		result.retainAll(B);
		return result;
	}

	public static Set<Word<String>> intersection(Collection<Set<Word<String>>> sets) {
		Set<Word<String>> result = new HashSet<>(sets.iterator().next());
		for (Set<Word<String>> set : sets) {
			result = intersection(result, set);
		}
		return result;
	}

	public static Set<Word<String>> union(Set<Word<String>> A, Set<Word<String>> B) {
		Set<Word<String>> result = new HashSet<>(A);
		result.addAll(B);
		return result;
	}

	public static Set<Word<String>> union(Collection<Set<Word<String>>> sets) {
		Set<Word<String>> result = new HashSet<>(sets.iterator().next());
		for (Set<Word<String>> set : sets) {
			result = union(result, set);
		}
		return result;
	}

	public static Set<Word<String>> deltaStar(Collection<Set<Word<String>>> sets) {
		Set<Word<String>> union = union(sets);
		Set<Word<String>> intersection = intersection(sets);
		return difference(union, intersection);
	}
}
