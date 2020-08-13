package sutInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;

public class SutInfo {
    private static List < ActionSignature > inputSignatures;
    private static List < ActionSignature > outputSignatures;

    public static List < ActionSignature > getInputSignatures() {
        return new ArrayList < ActionSignature > (inputSignatures);
    }

    public static void setInputSignatures(Map<String, List<String>> signatures) {
    	SutInfo.inputSignatures = new ArrayList < ActionSignature > ();
    	for (Entry<String, List<String>> entry : signatures.entrySet()) {
    		SutInfo.inputSignatures.add(new ActionSignature(entry.getKey(), entry.getValue()));
    	}
    }

    public static List < ActionSignature > getOutputSignatures() {
        return new ArrayList < ActionSignature > (outputSignatures);
    }

    public static void setOutputSignatures(Map<String, List<String>> signatures) {
    	SutInfo.outputSignatures = new ArrayList < ActionSignature > ();
    	for (Entry<String, List<String>> entry : signatures.entrySet()) {
    		SutInfo.outputSignatures.add(new ActionSignature(entry.getKey(), entry.getValue()));
    	}
    }

	public static Alphabet<String> generateInputAlphabet() {
		List<String> symbolList = new ArrayList<>();
		for (ActionSignature sig : SutInfo.getInputSignatures()) {
			List<String> currentAlpha = new ArrayList<>();
			currentAlpha.add(sig.getMethodName());
			symbolList.addAll(currentAlpha);
		}
		return Alphabets.fromList(symbolList);
	}

	public static Alphabet<String> generateOutputAlphabet() {
    	List<String> symbolList = new ArrayList<>();
		for (ActionSignature sig : SutInfo.getOutputSignatures()) {
			symbolList.add(sig.getMethodName());
		}
		return Alphabets.fromList(symbolList);
	}
}
