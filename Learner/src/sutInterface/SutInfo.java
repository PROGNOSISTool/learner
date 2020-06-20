package sutInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;

public class SutInfo {
    private static int minValue = 0;
    private static int maxValue = 255;
    private static List < ActionSignature > inputSignatures;
    private static List < ActionSignature > outputSignatures;

    public static int getMinValue() {
        return minValue;
    }

    public static int getMaxValue() {
        return maxValue;
    }

    public static void setMinValue(int minValue) {
    	SutInfo.minValue=minValue;
    }

    public static void setMaxValue(int maxValue) {
    	SutInfo.maxValue=maxValue;
    }

    public static List < ActionSignature > getInputSignatures() {
        return new ArrayList < ActionSignature > (inputSignatures);
    }

    public static void setInputSignatures(Map<String, List<String>> signatures) {
    	SutInfo.inputSignatures = new ArrayList < ActionSignature > ();
    	for (Entry<String, List<String>> entry : signatures.entrySet()) {
    		SutInfo.inputSignatures.add(new ActionSignature(entry.getKey(), entry.getValue()));
    	}
    }

    public static ActionSignature getInputSignature(String methodName) {
        for (ActionSignature sig: inputSignatures) {
            if (sig.getMethodName().equals(methodName)) {
                return sig;
            }
        }
        return null;
    }

    public static void addInputSignature(String methodName, List < String > parameters) {
    	SutInfo.inputSignatures.add(new ActionSignature(methodName, parameters));
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

    public static ActionSignature getOutputSignature(String methodName) {
        for (ActionSignature sig: outputSignatures) {
            if (sig.getMethodName().equals(methodName)) {
                return sig;
            }
        }
        return null;
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


    public static void addOutputSignature(String methodName, List < String > parameters) {
        SutInfo.outputSignatures.add(new ActionSignature(methodName, parameters));
    }

}
