package sutInterface;

import sutInterface.tcp.FlagSet;
import sutInterface.tcp.Symbol;

public class Serializer {
	private static final String DATA = "x";
	
	public static String concreteMessageToString(String flags, long seqNr,
			long ackNr) {
		StringBuilder result = new StringBuilder();

		String[] flagArray = flags.split("\\+");
		for (String flag : flagArray) {
			result.append(Character.toUpperCase(flag.charAt(0))); 
		}
		
		result.append(" ");
		result.append(seqNr);
		result.append(" ");
		result.append(ackNr);
		return result.toString();
	}
	
	public static String concreteMessageToString(invlang.types.FlagSet flags, long seqNr,
			long ackNr, int payloadLength) {
		StringBuilder sb = new StringBuilder();
		sb.append(flags.toInitials()).append(" ").append(seqNr).append(" ").append(ackNr).append(" [");
		for (int i = 0; i < payloadLength; i++) {
			sb.append(DATA);
		}
		sb.append("]");
		return sb.toString();
	}
	
	
	public static String concreteMessageToString(FlagSet flags, long seqNr,
			long ackNr, int payloadLength) {
		StringBuilder result = new StringBuilder();

		result.append(flags.toInitials());
		
		result.append(" ");
		result.append(seqNr);
		result.append(" ");
		result.append(ackNr);
		result.append(" [");
		for (int i = 0; i < payloadLength; i++) {
			result.append(DATA);
		}
		result.append("]");
		return result.toString();
	}

	public static String abstractMessageToString(invlang.types.FlagSet flags,
			String seqValidity, String ackValidity, int payloadLength) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (invlang.types.Flag flag : flags.getSortedFlags()) {
			if (!first) {
				sb.append("+");
			}
			first = false;
			sb.append(flag);
		}
		sb.append("(").append(seqValidity).append(",").append(ackValidity)
			.append(",").append(payloadLength).append(")");
		return sb.toString();
	}
	
	public static String abstractMessageToString(char[] flags,
			String seqValidity, String ackValidity, int payloadLength) {
		StringBuilder result = new StringBuilder();
		if (flags.length != 0) {
			result.append(charToFlag(flags[0]));
			for (int i = 1; i < flags.length; i++) {
				result.append("+");
				result.append(charToFlag(flags[i]));
			}
		} else {
			result.append("-");
		}
		
		result.append("(");
		result.append(seqValidity);
		result.append(",");
		result.append(ackValidity);
		result.append(",");
		result.append(payloadLength);
		result.append(")");
		return result.toString();
	}
	
	public static String abstractMessageToString(FlagSet flags,
			Symbol seqValidity, Symbol ackValidity, int payloadLength) {
		char[] flagInitials = flags.toInitials();
		String seqString = seqValidity.name();
		String ackString = ackValidity.name();
		String result = abstractMessageToString(flagInitials, seqString, ackString, payloadLength);
		return result;
	}
	
	public static String charToFlag(char c) {
		c = Character.toLowerCase(c);
		switch (c) {
		case 's':
			return "SYN";
		case 'a':
			return "ACK";
		case 'f':
			return "FIN";
		case 'r':
			return "RST";
		case 'p':
			return "PSH";
		default:
			return "???"; // if a flag is returned that is not listed here, use
							// "???" in the resulting model
		}
	}
}
