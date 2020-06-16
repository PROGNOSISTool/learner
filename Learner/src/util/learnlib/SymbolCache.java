package util.learnlib;

import java.util.Map;
import java.util.WeakHashMap;

import net.automatalib.words.impl.Symbol;

public class SymbolCache {
	public static Map<String, Symbol> cache = new WeakHashMap<String, Symbol>();

	public static Symbol getSymbol(String name) {
		Symbol sym = cache.get(name);
		if (sym == null) {
			sym = new Symbol(name);
		}
		cache.put(name, sym);
		return sym;
	}
}
