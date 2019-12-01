package sutInterface.tcp;

import sutInterface.Serializer;
import sutInterface.tcp.init.ClientInitOracle;
import sutInterface.tcp.init.FunctionalInitOracle;
import sutInterface.tcp.init.InitOracle;
import util.Calculator;
import util.exceptions.BugException;

/**
 * Mapper component from abs to conc and conc to abs.
 * 
 * @author paul
 */

public class TCPMapper {
	public static final long NOT_SET = -3; //Integer.MIN_VALUE;
	public static final long DATA_LENGTH = 4;
	public static final long WIN_SIZE = 8192;

	/* data variables of the mapper, determined from request/responses */
	public long seqSent, ackSent, serverSeq, clientSeq;
	public long dataAcked;
	public long ackReceived;
	public long seqReceived;
	
	/* Store the last abstract packets sent to/received from the sut */
	/* NOTE: in case of timeout or action sent, they remain unchanged*/
	public Packet packetSent;
	public Packet packetReceived;
	
	/* strings updated for all requests (actions, packets) and responses (timeout, packets) */
	public String lastMessageSent;
	public String lastMessageReceived;
	
	/* The only purpose of this is to inform the cache init oracle of the last action sent */
	public Action actionSent;

	/*
	 * boolean state variables, determined from data variables and the current
	 * values of the boolean variables
	 */
	public boolean freshSeqEnabled;
	public boolean freshAckEnabled;
	public boolean startState;
	
	
	private InitOracle oracle;
	public boolean isResponseTimeout;
	public boolean isRequestAction;

	public TCPMapper() {
		this( new ClientInitOracle());
		setDefault();
		
		//this( new FunctionalInitOracle());
		//this( new CachedInitOracle(new InitCacheManager("/home/student/GitHub/tcp-learner/output/1421437324088/cache.txt")));
	}
	
	public TCPMapper(InitOracle oracle) {
		setInitOracle(oracle);
		//by default, we assume that the start state is the listening state
		setStartState(true);  
		setDefault();
	}
	
	public TCPMapper clone() {
		TCPMapper mapper = new TCPMapper();
		mapper.setInitOracle(this.getInitOracle());
		mapper.serverSeq = this.serverSeq;
		mapper.clientSeq = this.clientSeq;
		mapper.seqSent = this.seqSent;
		mapper.ackSent = this.ackSent;
		mapper.packetSent = this.packetSent;
		mapper.packetReceived = this.packetReceived;
		mapper.actionSent = this.actionSent;
		mapper.freshSeqEnabled = this.freshSeqEnabled;
		mapper.freshAckEnabled = this.freshAckEnabled;
		mapper.startState = this.startState;
		mapper.isRequestAction = this.isRequestAction;
		mapper.isResponseTimeout = this.isResponseTimeout;
		return mapper;
	}
	
	public void setStartState(boolean isListening) {
		this.startState = isListening;
	}
	
	public InitOracle getInitOracle() {
		return this.oracle;
	}
	
	public void setInitOracle(InitOracle oracle) {
		this.oracle = oracle;
	}

	/* sets all the variables to their default values */
	public void setDefault() {
		this.seqSent = this.ackSent = NOT_SET;
		this.seqReceived = this.ackReceived = NOT_SET;
		this.serverSeq = this.clientSeq = NOT_SET;
		this.packetSent = new Packet(FlagSet.EMPTY, Symbol.INV, Symbol.INV, 0);
		this.packetReceived = new Packet(FlagSet.EMPTY, Symbol.INV, Symbol.INV, 0);
		this.freshSeqEnabled = this.startState;
		this.freshAckEnabled = this.startState;
		this.isResponseTimeout = false;
		this.isRequestAction = false;
		
		if(this.oracle != null)
			this.oracle.setDefault();
	}

	/* checks whether the abstractions are defined for the given inputs */
	public boolean isConcretizable(Symbol abstractSeq, Symbol abstractAck) {
		return !this.freshSeqEnabled || (!Symbol.INV.equals(abstractAck) && !Symbol.INV.equals(abstractSeq));
	}
	
	public String processOutgoingRequest(FlagSet flags, Symbol abstractSeq,
			Symbol abstractAck, int payloadLength) {
		this.actionSent = null; // no action sent
		
		/* check if abstraction is defined */
		if (!isConcretizable(abstractSeq, abstractAck)) {
			return Symbol.UNDEFINED.toString();
		}
		

		this.packetSent = new Packet(flags, abstractSeq, abstractAck, payloadLength);
		this.lastMessageSent = this.packetSent.serialize();

		/* generate input numbers */
		long concreteSeq = getConcrete(abstractSeq, getNextValidSeq());
		long concreteAck = getConcrete(abstractAck, getNextValidAck());
		
		/* do updates on input */
		if(this.freshSeqEnabled == true) {
			this.clientSeq = concreteSeq;
		}
		this.seqSent = concreteSeq;
		this.ackSent = concreteAck;
		this.isRequestAction = false;
		
		/* build concrete input */
		String concreteInput = Serializer.concreteMessageToString(flags,
				concreteSeq, concreteAck, payloadLength);
		return concreteInput;
	}
	
	public String processOutgoingReset() {
		return (clientSeq == NOT_SET)? null : Serializer.concreteMessageToString(new FlagSet(Flag.RST), clientSeq, 0, 0);
	}
	
	public void processOutgoingAction(Action action) {
		this.actionSent = action;
		this.lastMessageSent = action.name();
		this.isRequestAction = true;
	}
	
	private long newInvalidWithinWindow(long refNumber) {
		return Calculator.randWithinRange(Calculator.sum(refNumber, Calculator.MAX_NUM - WIN_SIZE + 2), Calculator.sum(refNumber, Calculator.MAX_NUM/2 + 1));
	}
	
	//modSum(serverSeq, maxNum/2+2), modSum(serverSeq, maxNum - win + 1), modSum(serverSeq, -8191)
	private long newInvalidOutsideWindow(long refNumber) {
		return Calculator.randWithinRange(Calculator.sum(refNumber, Calculator.MAX_NUM/2 + 2), Calculator.sum(refNumber, Calculator.MAX_NUM - WIN_SIZE + 1));
	}
	
	private long getConcrete(Symbol absToSend, long nextValidNumber) {
		long nextNumber;
		switch (absToSend) {
		case RAND:
			nextNumber = Calculator.newValue();
		case V:
			nextNumber = nextValidNumber;
			break;
		case INV:
			nextNumber = Calculator.newOtherThan(nextValidNumber);
			break;
		case IWIN:
			nextNumber = newInvalidWithinWindow(this.serverSeq);
			break;
		case OWIN:
			nextNumber = newInvalidOutsideWindow(this.serverSeq);
			break;
		case WIN:  //not yet tried
			//nextNumber = Gen.randWithinRange(Gen.sum(1, nextValidNumber), Gen.sum(WIN_SIZE, nextValidNumber));
			nextNumber = Calculator.randWithinRange(Calculator.sub(nextValidNumber, WIN_SIZE ), Calculator.sub(nextValidNumber, 1));
			break;
		default:
			// debugging abstract input values
			if(absToSend.name().startsWith("P")) {
				Long offset = Long.valueOf(absToSend.name().substring(1));
				nextNumber = nextValidNumber + offset;
			} else {
				if(absToSend.name().startsWith("M")) {
					Long offset = Long.valueOf(absToSend.name().substring(1));
					nextNumber = nextValidNumber - offset;
				} else {
					throw new RuntimeException("Invalid parameter \"" + absToSend
							+ "\". The input-action used ");		
				}	
			}
		}
		return nextNumber;
	}

	public long getNextValidSeq() {
		long nextSeq;
		if (this.freshSeqEnabled == true) {
			nextSeq = Calculator.newValue();
		} else {
			nextSeq = this.clientSeq;
		}
		return nextSeq;
	}

	public long getNextValidAck() {
		long nextAck;
		if (this.freshAckEnabled == true) {
			nextAck = Calculator.newValue();
		} else {
			nextAck = Calculator.nth(this.serverSeq, this.packetReceived.payload() );
		}
		return nextAck;
	}
	
	public void processIncomingTimeout() {
		/* state 0 detecting condition */
		this.lastMessageReceived = "TIMEOUT";
		this.isResponseTimeout = true;
		checkInit();
	}
	
	public String processIncomingResponse(FlagSet flags, long concreteSeq,
			long concreteAck, int payloadLength) {
		/* generate output symbols */
		Symbol abstractSeq = getAbstract(concreteSeq, true);
		Symbol abstractAck = getAbstract(concreteAck, false);
		
		/* do updates on output */
		if (this.packetSent.payload() + this.clientSeq == concreteAck) {
            this.clientSeq = concreteAck;
	    }
	    if (abstractSeq == Symbol.FRESH || (this.packetReceived.payload() + this.serverSeq == concreteSeq)) {
	            this.serverSeq = concreteSeq;
	    }
		
		/* state 0 detecting condition */
		this.seqReceived = concreteSeq;
		this.ackReceived = concreteAck;
		this.packetReceived = new Packet(flags, abstractSeq, abstractAck, payloadLength);
		this.lastMessageReceived = this.packetReceived.serialize();
		this.isResponseTimeout = false;
		checkInit();

		/* build concrete output */
		String abstractOutput = Serializer.abstractMessageToString(
				flags, abstractSeq, abstractAck, payloadLength);
		
		return abstractOutput;
	}
	
	//select which check init function you want to call.
	protected void checkInit() {
		checkInitClient_CONNECT_MOST_V();
	}

	private Symbol getAbstract(long nrReceived, boolean isIncomingSeq) {
		Symbol checkedSymbol;
		if (nrReceived == Calculator.next(this.clientSeq)) {
			checkedSymbol = Symbol.SNCLIENTP1;
		} else if (nrReceived == Calculator.nth(this.clientSeq, 2)) {
			checkedSymbol = Symbol.SNCLIENTP2;
		} else if (nrReceived == this.clientSeq) {
			checkedSymbol = Symbol.SNCLIENT;
		} else if (nrReceived == this.serverSeq) {
			checkedSymbol = Symbol.SNSERVER;
		} else if (nrReceived == Calculator.next(this.serverSeq)) {
			checkedSymbol = Symbol.SNSERVERP1;
		} else if (nrReceived == Calculator.nth(this.serverSeq, 2)) {
			checkedSymbol = Symbol.SNSERVERP2;
		} else if (nrReceived == this.seqSent) {
			checkedSymbol = Symbol.SNSENT;
		} else if (nrReceived == this.ackSent) {
			checkedSymbol = Symbol.ANSENT;
		} else if (nrReceived == 0) {
			checkedSymbol = Symbol.ZERO;
		} else if ( isIncomingSeq == true && this.freshAckEnabled ) {
			checkedSymbol = Symbol.FRESH;
		}  
//			if ( isIncomingSeq == true && this.freshAckEnabled ) {
//			checkedSymbol = Symbol.FRESH;
//		} else if ( isIncomingSeq == false && this.freshAckEnabled ) {
//			checkedSymbol = Symbol.FRESH;
//		} 
		else {
			checkedSymbol = Symbol.INV;
		}
		return checkedSymbol;
	}
	
	// not tested, probably won't work
	protected void checkInitClient_CONNECT_MOST_V_INV() {
		boolean sentRST = packetSent.flags.has(Flag.RST) && packetSent.seq.is(Symbol.V);
		boolean receivedRST = !isResponseTimeout && packetReceived.flags.has(Flag.RST);
		
		if (!freshSeqEnabled && (receivedRST || sentRST) || seqReceived == 0) {
			freshSeqEnabled = true;
		} else {
			freshSeqEnabled = !(packetSent.flags.has(Flag.SYN) && ackReceived == seqSent + 1)
					&& freshSeqEnabled;
		}
		
		if (receivedRST || sentRST) {
			freshAckEnabled = true; // && freshSeqEnabled; //this might be needed at some point
		} else if (seqReceived == ackSent || 
				(!isResponseTimeout && packetReceived.flags.has(Flag.SYN))) {
			freshAckEnabled = false;
		}
	}
	
	// does not include RST and RST+ACK packets
	protected void checkInitClient_CONNECT_MOST_V() {
		boolean sentRST = packetSent.flags.has(Flag.RST);
		boolean receivedRST = !isResponseTimeout && packetReceived.flags.has(Flag.RST);
		
		if (!freshSeqEnabled && (receivedRST || sentRST) || seqReceived == 0) {
			freshSeqEnabled = true;
		} else {
			freshSeqEnabled = !(packetSent.flags.has(Flag.SYN) && ackReceived == seqSent + 1)
					&& freshSeqEnabled;
		}
		
		if (receivedRST || sentRST) {
			freshAckEnabled = true; // && freshSeqEnabled; //this might be needed at some point
		} else if (seqReceived == ackSent || 
				(!isResponseTimeout && packetReceived.flags.has(Flag.SYN))) {
			freshAckEnabled = false;
		}
	}
	
	protected void checkInitServer_NO_ACTIONS_ALL_V() {
		boolean isInit = freshSeqEnabled || freshAckEnabled;
		isInit = 
		(packetSent.flags.has(Flag.RST) )  || //t1
		(packetReceived.flags.has(Flag.RST) ) ||
		(isInit && !packetReceived.flags.is(Flag.SYN, Flag.ACK));
		
		freshSeqEnabled = isInit;
		freshAckEnabled = isInit;
	}
	
	protected void checkInitServer_NO_ACTIONS_MOST_V_INV() {
		boolean isInit = freshSeqEnabled || freshAckEnabled;
		isInit = 
		(packetSent.flags.has(Flag.ACK) && packetReceived.flags.is(Flag.RST) && packetSent.seq.is(Symbol.V) && packetSent.ack.is(Symbol.V))  || //t1
		(packetSent.flags.has(Flag.RST) && packetSent.seq.is(Symbol.V)) || // covers RST[+ACK](V,_)->
		(packetSent.flags.has(Flag.SYN) && packetReceived.flags.is(Flag.RST,Flag.ACK) && packetSent.seq.is(Symbol.V)) ||// || 
		(isInit && !packetReceived.flags.is(Flag.SYN, Flag.ACK));
		
		freshSeqEnabled = isInit;
		freshAckEnabled = isInit;
	}

	public String getState() {
		return "MAPPER [FRESH_SEQ=" + this.freshSeqEnabled + "; " +
				"FRESH_ACK=" + this.freshAckEnabled + "; " +
				"lastSeqSent=" + this.seqSent + 
				"; lastAckSent=" + this.ackSent + 
				"; clientSeq=" + this.clientSeq + 
				"; serverSeq=" + this.serverSeq + "]";
	}

	public String processOutgoingRequest(String flags, String abstractSeq,
			String abstractAck, int payloadLength) {

		/* generate enum classes */
		Symbol seqSymbol = Symbol.toSymbol(abstractSeq);
		Symbol ackSymbol = Symbol.toSymbol(abstractAck);
		FlagSet flagSet = new FlagSet(flags);
		
		/* call actual method */
		String concreteInput = processOutgoingRequest(flagSet, seqSymbol, ackSymbol, payloadLength);
		return concreteInput;
	}	
	
	public String processIncomingResponse(String flags, long concreteSeq,
			long concreteAck, int payloadLength) {
		
		/* generate enum classes */
		FlagSet flagSet = new FlagSet(flags.toCharArray());
		
		/* call actual method */
		String abstractOutput = processIncomingResponse(flagSet, concreteSeq, concreteAck, payloadLength);
		return abstractOutput;
	}

	/* compatibility version */
	public String processIncomingResponseComp(String flags, String seqReceived,
			String ackReceived, int payloadLength) {
		long seq = Long.valueOf(seqReceived);
		long ack = Long.valueOf(ackReceived);
		String abstractOutput = processIncomingResponse(flags, seq, ack, payloadLength);
		return abstractOutput;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Mapper state:\n");
		sb.append("serverSeq: " + serverSeq + "   ");
		sb.append("clientSeq: " + clientSeq + "   ");
		sb.append("packetSent: " + packetSent + "   ");
		sb.append("packetRecv: " + packetReceived + "   ");
//		sb.append("lastSeqSent: " + lastSeqSent + "   ");
//		sb.append("lastAckSent: " + lastAckSent + "   ");
//		sb.append("dataAcked: " + dataAcked + "   ");
//		sb.append("lastFlagsSent: " + lastFlagsSent + "   ");
//		sb.append("lastFlagsReceived: " + lastFlagsReceived + "   ");
//		sb.append("lastAbstractSeqSent: " + lastAbstractSeqSent + "   ");
//		sb.append("lastAbstractAckSent: " + lastAbstractAckSent + "   ");
//		sb.append("lastAbstractSeqReceived: " + lastAbstractSeqReceived + "   ");
//		sb.append("lastAbstractAckReceived: " + lastAbstractAckReceived + "   ");
		sb.append("isInit: " + freshSeqEnabled + "   ");
		/*
		 * boolean state variables, determined from data variables and the current
		 * values of the boolean variables
		 */
		return sb.toString();
	}
}
