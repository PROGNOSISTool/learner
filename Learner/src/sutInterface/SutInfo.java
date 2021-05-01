package sutInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;

public class SutInfo {
    private static List < ActionSignature > inputAlphabet;

    public static List < ActionSignature > getInputAlphabet() {
        return new ArrayList<>(inputAlphabet);
    }

    public static void setInputAlphabet(Map<String, List<String>> signatures) {
    	SutInfo.inputAlphabet = new ArrayList<>();
    	for (Entry<String, List<String>> entry : signatures.entrySet()) {
    		SutInfo.inputAlphabet.add(new ActionSignature(entry.getKey(), entry.getValue()));
    	}
    }

	public static Alphabet<String> generateInputAlphabet() {
		List<String> symbolList = new ArrayList<>();
		for (ActionSignature sig : SutInfo.getInputAlphabet()) {
			List<String> currentAlpha = new ArrayList<>();
			currentAlpha.add(sig.getMethodName());
			symbolList.addAll(currentAlpha);
		}
		return Alphabets.fromList(symbolList);
	}
}
