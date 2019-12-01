package functionalMappers;

import java.util.HashMap;
import java.util.Map;

import sutInterface.SocketWrapper;
import sutInterface.SutWrapper;
import sutInterface.tcp.Action;
import sutInterface.tcp.Internal;
import sutInterface.tcp.Symbol;
import util.InputAction;
import util.Log;
import util.OutputAction;

// SutWrapper used for learning TCP (uses abstraction) 
// Unlike SimpleSutWrapper, all communication is directed through a mapper component
public class TCPSutWrapperSpecification implements SutWrapper{

	private final static Map<Integer,SocketWrapper> socketWrapperMap = new HashMap<Integer, SocketWrapper>();
	private TCPMapperSpecification mapper;
	private TCPMapperSpecification previousMapper;
	private final SocketWrapper socketWrapper;
	
	public TCPSutWrapperSpecification(int tcpServerPort, TCPMapperSpecification mapper) {
		this(tcpServerPort);
		this.mapper = mapper;
	}
	
	public TCPSutWrapperSpecification(int tcpServerPort) {
		if(socketWrapperMap.containsKey(tcpServerPort)) {
			this.socketWrapper = socketWrapperMap.get(tcpServerPort); 
		} else {
			this.socketWrapper = new SocketWrapper(tcpServerPort);
			socketWrapperMap.put(tcpServerPort,this.socketWrapper);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					Log.fatal("Detected shutdown, commencing connection "+ 
							socketWrapper + " termination");
					if (socketWrapper != null) {
						try {
							Log.fatal("Sending an exit message to the adapter");
							socketWrapper.writeInput("exit");
						} finally {
							Log.fatal("Closing the socket");
							socketWrapper.close();
						}
					}
				}
			});
		}
		
	}
	
	public TCPSutWrapperSpecification(int tcpServerPort, TCPMapperSpecification mapper, boolean exitIfInvalid) {
		this(tcpServerPort, mapper);
	}
	
	private static TCPSutWrapperSpecification tcpWrapper = null;
	public static void setTCPSutWrapper(TCPSutWrapperSpecification tcpWrapper) {
		TCPSutWrapperSpecification.tcpWrapper  = tcpWrapper;
	}
	public static TCPSutWrapperSpecification getTCPSutWrapper() {
		return TCPSutWrapperSpecification.tcpWrapper;
	}
	
	public OutputAction sendInput(InputAction symbolicInput) {
		OutputAction symbolicOutput;
		previousMapper = mapper.clone();
		
		// Build concrete input
		String abstractRequest = symbolicInput.getValuesAsString(); 
		String concreteRequest;
		
		// Internal inputs are handled first. They have no effect on the system
		if(Internal.isInternalCommand(abstractRequest)) {
			if(Internal.valueOf(abstractRequest) == Internal.REVERT) {
				mapper = previousMapper;
				sendReset();
				return null;
			} 
		}

		
		Log.info("MAPPER BEFORE:" + mapper.getState());
		// processing of action-commands
		// note: mapper is not updated with action commands
		
		if(Action.isAction(abstractRequest)) {
			this.mapper.processOutgoingAction(Action.valueOf(abstractRequest));
			concreteRequest = abstractRequest.toLowerCase();
		}
		// only processing of packet-requests
		else {
			concreteRequest = processOutgoingPacket(abstractRequest);
		}

		Log.info("MAPPER INTER:" + mapper.getState());
		
		// Handle non-concretizable abstract input case
		if(concreteRequest.equalsIgnoreCase(Symbol.UNDEFINED.name())) {
			symbolicOutput = new OutputAction(Symbol.UNDEFINED.name());
		}
		// Send concrete input, receive output from SUT and make abs
		else {
			String concreteResponse = sendPacket(concreteRequest);
			String abstractResponse = processIncomingPacket(concreteResponse);
			symbolicOutput = new OutputAction(abstractResponse);
		}
		Log.info("MAPPER AFTER:" + mapper.getState());
		
		return symbolicOutput;
	}

	
	/**
	 * called by the learner to reset the automaton
	 */
	public void sendReset() {
		Log.info("******** RESET ********");
		String rstMessage = mapper.processOutgoingReset();
		// mapper made a pertinent reset message
		if(rstMessage != null) {
			socketWrapper.writeInput(rstMessage);
			socketWrapper.readOutput();
		}
		socketWrapper.writeInput("reset");
		mapper.setDefault();
	}
	
	/**
	 * Updates seqToSend and ackToSend correspondingly.
	 * 
	 * @param abstract input e.g. "FA(V,INV)"
	 * @return concrete output of the form "flags seqNr ackNr" describing a
	 *         packet, e.g. "FA 651 814", through the socket.
	 */
	private String processOutgoingPacket(String input) {
		String[] inputValues = input.split("\\(|,|\\)"); // of form {flags, seq,
															// ack}, e.g.
															// {"FIN+ACK", "V",
															// "INV"}
		String flags = inputValues[0];
		String abstractSeq = inputValues[1];
		String abstractAck = inputValues[2];
		int payloadLength = Integer.parseInt(inputValues[3]);
		String concreteInput = mapper.processOutgoingRequest(flags,
				abstractSeq, abstractAck, payloadLength);
		return concreteInput;
	}

	private String sendPacket(String concreteRequest) {
		if (concreteRequest == null) {
			socketWrapper.writeInput("nil");
		} else {
			socketWrapper.writeInput(concreteRequest);
		}
		String concreteResponse = socketWrapper.readOutput();
		return concreteResponse;
	}
	
	/**
	 * 
	 * @param concreteResponse 
	 *            of the form "flags seqNr ackNr", e.g. "FA 1000 2000"
	 * @return output, e.g. "FA(V,INV)"
	 */
	private String processIncomingPacket(String concreteResponse) {
		String abstractResponse;
		if (concreteResponse.equals("timeout")) {
			mapper.processIncomingTimeout();
			abstractResponse = "TIMEOUT";
		} else {
			String[] inputValues = concreteResponse.split(" ");
			String flags = inputValues[0];

			long seqReceived = Long.parseLong(inputValues[1]);
			long ackReceived = Long.parseLong(inputValues[2]);
			abstractResponse = mapper.processIncomingResponse(flags,
					seqReceived, ackReceived);
		}
		return abstractResponse;
	}
	
	public void close() {
		this.socketWrapper.close();
	}
}
