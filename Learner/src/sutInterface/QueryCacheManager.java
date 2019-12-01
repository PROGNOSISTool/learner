package sutInterface;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import util.Log;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.shared.SymbolImpl;

public class QueryCacheManager {
	private static final String SEP = " ";
	
	public void dump(String fileName,
			Map<List<Symbol>, List<Symbol>> cachedTraces) {
		try {
			FileWriter fw = new FileWriter(fileName, false);
			for (List<Symbol> inputSymbols : cachedTraces.keySet()) {
				List<Symbol> outputSymbols = cachedTraces.get(inputSymbols);
				for (int i = 0; i < inputSymbols.size(); i++) {
					fw.append(inputSymbols.get(i) + SEP + outputSymbols.get(i) + SEP);
				}
				fw.append("\n");
			}
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Map<List<Symbol>, List<Symbol>> load(String fileName) {
		HashMap<List<Symbol>, List<Symbol>> cachedTraces = new LinkedHashMap<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = reader.readLine()) != null
					&& line.isEmpty() == false) {
				String[] tokens = line.split(SEP);
				List<Symbol> inputSymbols = new ArrayList<Symbol>();
				List<Symbol> outputSymbols = new ArrayList<Symbol>();

				for (int i = 0; i < tokens.length; i = i + 2) {
					inputSymbols.add(new SymbolImpl(tokens[i]));
					outputSymbols.add(new SymbolImpl(tokens[i + 1]));
				}

				cachedTraces.put(inputSymbols, outputSymbols);
			}
			reader.close();
		} catch (FileNotFoundException e) {
			Log.err("Cache file not found. Setting it up this run");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
			
		}
		return cachedTraces;
	}

}
