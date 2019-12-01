package util.learnlib;

import java.util.Map;
import java.util.WeakHashMap;

import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.shared.SymbolImpl;

public class SymbolCache {
	public static Map<String, Symbol> cache = new WeakHashMap<String, Symbol>();

	public static Symbol getSymbol(String name) {
		Symbol sym = cache.get(name);
		if (sym == null) {
			sym = new SymbolImpl(name);
		}
		cache.put(name, sym);
		return sym;
	}
}