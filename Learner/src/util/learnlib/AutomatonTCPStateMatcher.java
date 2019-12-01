package util.learnlib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.State;
import de.ls5.jlearn.interfaces.Symbol;

public class AutomatonTCPStateMatcher {
	
	enum TCPState {
		CLOSED,
		LISTEN_PREACCEPT,
		LISTEN_POSTACCEPT,
		SYNRCVD_PREACCEPT,
		SYNRCVD_POSTACCEPT,
		ESTABLISHED_PREACCEPT,
		ESTABLISHED_PREACCEPT_RCV,
		CLOSEWAIT_PREACCEPT,
		ESTABLISHED_POSTACCEPT,
		ESTABLISHED_POSTACCEPT_RCV,
		CLOSEWAIT_POSTACCEPT,
		CLOSEWAIT_POSTACCEPT_RCV,
		CLOSEWAIT_PREACCEPT_RCV,
		FINWAIT1,
		TIMEWAIT,
		SERVER_CLOSED
	}

	public static Map<List<String>, TCPState> tcpMap = new HashMap<>();
	
	static {
		tcpMap.put(new ArrayList<String>(), TCPState.CLOSED);
		tcpMap.put(Arrays.asList("CLOSE"), TCPState.SERVER_CLOSED);
		tcpMap.put(Arrays.asList("LISTEN"), TCPState.LISTEN_PREACCEPT);
		tcpMap.put(Arrays.asList("LISTEN", "ACCEPT"), TCPState.LISTEN_POSTACCEPT);
		tcpMap.put(Arrays.asList("LISTEN", "SYN(V,V,0)"), TCPState.SYNRCVD_PREACCEPT);
		tcpMap.put(Arrays.asList("LISTEN", "SYN(V,V,0)", "ACCEPT"), TCPState.SYNRCVD_POSTACCEPT);
		tcpMap.put(Arrays.asList("LISTEN", "SYN(V,V,0)", "ACK(V,V,0)"), TCPState.ESTABLISHED_PREACCEPT);
		tcpMap.put(Arrays.asList("LISTEN", "SYN(V,V,0)", "ACK(V,V,0)", "ACCEPT"), TCPState.ESTABLISHED_POSTACCEPT);
		tcpMap.put(Arrays.asList("LISTEN", "SYN(V,V,0)", "ACK(V,V,0)", "ACK+PSH(V,V,1)"), TCPState.ESTABLISHED_PREACCEPT_RCV);
		tcpMap.put(Arrays.asList("LISTEN", "SYN(V,V,0)", "ACK(V,V,0)", "ACCEPT", "ACK+PSH(V,V,1)"), TCPState.ESTABLISHED_POSTACCEPT_RCV);
		tcpMap.put(Arrays.asList("LISTEN", "SYN(V,V,0)", "ACK(V,V,0)", "FIN+ACK(V,V,0)"), TCPState.CLOSEWAIT_POSTACCEPT);
		tcpMap.put(Arrays.asList("LISTEN", "SYN(V,V,0)", "ACK(V,V,0)", "FIN+ACK(V,V,0)", "ACCEPT"), TCPState.CLOSEWAIT_POSTACCEPT);
		tcpMap.put(Arrays.asList("LISTEN", "SYN(V,V,0)", "ACK(V,V,0)", "FIN+ACK(V,V,0)", "ACK+PSH(V,V,1)"), TCPState.CLOSEWAIT_PREACCEPT_RCV);
		tcpMap.put(Arrays.asList("LISTEN", "SYN(V,V,0)", "ACK(V,V,0)", "FIN+ACK(V,V,0)", "ACK+PSH(V,V,1)", "ACCEPT"), TCPState.CLOSEWAIT_POSTACCEPT_RCV);
		tcpMap.put(Arrays.asList("LISTEN", "SYN(V,V,0)", "ACK(V,V,0)", "ACCEPT"), TCPState.ESTABLISHED_POSTACCEPT);
		tcpMap.put(Arrays.asList("LISTEN", "SYN(V,V,0)", "ACK(V,V,0)", "ACCEPT"), TCPState.ESTABLISHED_POSTACCEPT);
		tcpMap.put(Arrays.asList("LISTEN", "ACCEPT"), TCPState.LISTEN_POSTACCEPT);
		tcpMap.put(Arrays.asList("LISTEN", "ACCEPT", "SYN(V,V,0)", "ACK(V,V,0)", "CLOSECONNECTION"), TCPState.FINWAIT1);
		tcpMap.put(Arrays.asList("LISTEN", "ACCEPT", "SYN(V,V,0)", "ACK(V,V,0)", "CLOSECONNECTION", "FIN+ACK(V,V,0)"), TCPState.TIMEWAIT);
	}

	public static Map<State, String> produceTCPStateMap(Automaton automaton) {
		Map<State, String> stateMap = new HashMap<State, String>();
		for (List<String> accessSeq : tcpMap.keySet()) {
			List<Symbol> traceToState = AutomatonUtils.buildSymbols(accessSeq);
			State state = AutomatonUtils.getState(automaton, traceToState);
			if (!stateMap.containsKey(state)) {
				stateMap.put(state, tcpMap.get(accessSeq).name());
			}
		}
		
		return stateMap;
	}
	
}
