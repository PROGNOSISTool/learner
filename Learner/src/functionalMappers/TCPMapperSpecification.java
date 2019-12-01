package functionalMappers;

import sutInterface.Serializer;
import sutInterface.tcp.Action;
import sutInterface.tcp.Flag;
import sutInterface.tcp.FlagSet;
import sutInterface.tcp.Symbol;
import sutInterface.tcp.init.ClientInitOracle;
import sutInterface.tcp.init.FunctionalInitOracle;
import sutInterface.tcp.init.InitOracle;
import util.Calculator;
import util.exceptions.BugException;

/**
 * Mapper component from abs to conc and conc to abs.
 * 
 * @author paul & ramon
 */

public class TCPMapperSpecification {
	public static final long NOT_SET = -3; //Integer.MIN_VALUE;
	public static final long DATA_LENGTH = 4;
	public static final long WIN_SIZE = 8192;

	/* data variables of the mapper, determined from request/responses */
	private long lastConcSeqSent, lastConcAckSent, lastConcAckReceived, 
			lastConcSeqReceived, sutSeq, learnerSeq;
	private Symbol lastAbsSeqSent;
	private FlagSet lastFlagsSent, lastFlagsReceived;
	
	/*
	 * boolean state variables, determined from data variables and the current
	 * values of the boolean variables
	 */
	private boolean freshSeqEnabled;
	private boolean freshAckEnabled;
	private boolean isLastResponseTimeout;
	
	
	public TCPMapperSpecification() {
		this( new ClientInitOracle());
		setDefault();
		//this( new FunctionalInitOracle());
		//this( new CachedInitOracle(new InitCacheManager("/home/student/GitHub/tcp-learner/output/1421437324088/cache.txt")));
	}
	
	public TCPMapperSpecification(InitOracle oracle) {
		//by default, we assume that the start state is the listening state
		setDefault();
	}
	
	public TCPMapperSpecification clone() {
		TCPMapperSpecification mapper = new TCPMapperSpecification();
		mapper.sutSeq = this.sutSeq;
		mapper.learnerSeq = this.learnerSeq;
		mapper.lastConcSeqSent = this.lastConcSeqSent;
		mapper.lastConcAckSent = this.lastConcAckSent;
		//mapper.lastPacketSent = this.lastPacketSent;
		//mapper.lastPacketReceived = this.lastPacketReceived;
		mapper.lastAbsSeqSent = this.lastAbsSeqSent;
		mapper.lastFlagsReceived = this.lastFlagsReceived;
		mapper.lastFlagsSent = this.lastFlagsSent;
		
		mapper.freshSeqEnabled = this.freshSeqEnabled;
		mapper.freshAckEnabled = this.freshAckEnabled;
		return mapper;
	}

	/* sets all the variables to their default values */
	public void setDefault() {
		this.lastConcSeqSent = this.lastConcAckSent = NOT_SET;
		this.lastConcSeqReceived = this.lastConcAckReceived = NOT_SET;
		this.sutSeq = this.learnerSeq = NOT_SET;
		//this.lastPacketSent = new Packet(FlagSet.EMPTY, Symbol.INV, Symbol.INV);
		//this.lastPacketReceived = new Packet(FlagSet.EMPTY, Symbol.INV, Symbol.INV);			
		this.lastFlagsReceived = this.lastFlagsSent = FlagSet.EMPTY;
		this.lastAbsSeqSent = Symbol.INV;
		this.freshSeqEnabled = true;
		this.freshAckEnabled = true;
		this.isLastResponseTimeout = false;
	}

	/* checks whether the abstractions are defined for the given inputs */
	/*public boolean isConcretizable(Symbol abstractSeq, Symbol abstractAck) {
		return !this.freshSeqEnabled || (!Symbol.INV.equals(abstractAck) && !Symbol.INV.equals(abstractSeq));
	}*/
	
	public String processOutgoingRequest(FlagSet flags, Symbol abstractSeq,
			Symbol abstractAck, int payloadLength) {
		
		/* check if abstraction is defined */
		if (this.freshSeqEnabled && (Symbol.INV.equals(abstractAck) || Symbol.INV.equals(abstractSeq))) {
			return Symbol.UNDEFINED.toString();
		}

		/* generate input numbers */
		long concreteSeq;// = getConcrete(abstractSeq, getNextValidSeq());
		long concreteAck;// = getConcrete(abstractAck, getNextValidAck());
		if (freshSeqEnabled) {
			concreteSeq = Calculator.newValue();
		} else if (abstractSeq == Symbol.V) {
			concreteSeq = learnerSeq;
		} else {
			concreteSeq = Calculator.newOtherThan(learnerSeq);
		}
		if (freshAckEnabled) {
			concreteAck = Calculator.newValue();
		} else if (abstractAck == Symbol.V) {
			concreteAck = Calculator.nth(sutSeq, this.lastFlagsReceived.has(Flag.SYN) || this.lastFlagsReceived.has(Flag.FIN) ? 1 : 0);
		} else {
			concreteAck = Calculator.newOtherThan(sutSeq);
		}
		
		
		/* do updates on input */
		if(this.freshSeqEnabled == true) {
			this.learnerSeq = concreteSeq;
		}
		this.lastConcSeqSent = concreteSeq;
		this.lastConcAckSent = concreteAck;
		this.lastFlagsSent = flags;
		this.lastAbsSeqSent = abstractSeq;
		
		/* build concrete input */
		String concreteInput = Serializer.concreteMessageToString(flags,
				concreteSeq, concreteAck, payloadLength);
		return concreteInput;
	}
	
	public String processOutgoingReset() {
		return (learnerSeq == NOT_SET)? null : Serializer.concreteMessageToString(new FlagSet(Flag.RST), learnerSeq, 0, 0);
	}
	
	public void processOutgoingAction(Action action) {
		
	}
	
	private long newInvalidWithinWindow(long refNumber) {
		return Calculator.randWithinRange(Calculator.sum(refNumber, Calculator.MAX_NUM - WIN_SIZE + 2), Calculator.sum(refNumber, Calculator.MAX_NUM/2 + 1));
	}
	
	//modSum(serverSeq, maxNum/2+2), modSum(serverSeq, maxNum - win + 1), modSum(serverSeq, -8191)
	private long newInvalidOutsideWindow(long refNumber) {
		return Calculator.randWithinRange(Calculator.sum(refNumber, Calculator.MAX_NUM/2 + 2), Calculator.sum(refNumber, Calculator.MAX_NUM - WIN_SIZE + 1));
	}
	/*
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
			nextNumber = newInvalidWithinWindow(this.sutSeq);
			break;
		case OWIN:
			nextNumber = newInvalidOutsideWindow(this.sutSeq);
			break;
		case WIN:  //not yet tried
			//nextNumber = Gen.randWithinRange(Gen.sum(1, nextValidNumber), Gen.sum(WIN_SIZE, nextValidNumber));
			nextNumber = Calculator.randWithinRange(Calculator.sub(nextValidNumber, WIN_SIZE ), Calculator.sub(nextValidNumber, 1));
			break;
		default:
			throw new RuntimeException("Invalid parameter \"" + absToSend
					+ "\". The input-action used ");
		}
		return nextNumber;
	}*/

	/*private long getNextValidSeq() {
		long nextSeq;
		if (this.freshSeqEnabled == true) {
			nextSeq = Calculator.newValue();
		} else {
			nextSeq = this.clientSeq;
		}
		return nextSeq;
	}

	private long getNextValidAck() {
		long nextAck;
		if (this.freshAckEnabled == true) {
			nextAck = Calculator.newValue();
		} else {
			nextAck = Calculator.nth(this.serverSeq, this.lastPacketReceived.payload());
		}
		return nextAck;
	}*/
	
	public void processIncomingTimeout() {
		/* state 0 detecting condition */
		this.isLastResponseTimeout = true;
		//this.lastPacketReceived = null;
		checkInit();
	}
	
	public String processIncomingResponse(FlagSet flags, long concreteSeq,
			long concreteAck) {
		/* generate output symbols */
		Symbol abstractSeq = getAbstract(concreteSeq, true);
		Symbol abstractAck = getAbstract(concreteAck, false);
		
		/* do updates on output */
		if (abstractAck == Symbol.SNCLIENTP1 || abstractAck == Symbol.SNCLIENTPD) {
			this.learnerSeq = concreteAck;
		}
		if (abstractSeq == Symbol.FRESH || abstractSeq == Symbol.SNSERVERP1) {
			this.sutSeq = concreteSeq;
		}
		
		/* state 0 detecting condition */
		this.isLastResponseTimeout = false;
		this.lastConcSeqReceived = concreteSeq;
		this.lastConcAckReceived = concreteAck;
		//this.lastPacketReceived = new Packet(flags, abstractSeq, abstractAck);
		this.lastFlagsReceived = flags;
		
		/* build concrete output */
		/*String abstractOutput = Serializer.abstractMessageToString(
				flags, abstractSeq,
				abstractAck);
		
		checkInit();
		
		return abstractOutput;*/
		return null;
	}

	private Symbol getAbstract(long nrReceived, boolean isIncomingSeq) {
		Symbol checkedSymbol;
		if (nrReceived == Calculator.next(this.learnerSeq)) {
			checkedSymbol = Symbol.SNCLIENTP1;
		} else if (nrReceived == Calculator.nth(this.learnerSeq, 2)) {
			checkedSymbol = Symbol.SNCLIENTP2;
		} else if (nrReceived == this.learnerSeq) {
			checkedSymbol = Symbol.SNCLIENT;
		} else if (nrReceived == this.sutSeq) {
			checkedSymbol = Symbol.SNSERVER;
		} else if (nrReceived == Calculator.next(this.sutSeq)) {
			checkedSymbol = Symbol.SNSERVERP1;
		} else if (nrReceived == Calculator.nth(this.sutSeq, 2)) {
			checkedSymbol = Symbol.SNSERVERP2;
		} else if (nrReceived == this.lastConcSeqSent) {
			checkedSymbol = Symbol.SNSENT;
		} else if (nrReceived == this.lastConcAckSent) {
			checkedSymbol = Symbol.ANSENT;
		} else if (nrReceived == 0) {
			checkedSymbol = Symbol.ZERO;
		} else if (isIncomingSeq == true && this.freshAckEnabled) {
			checkedSymbol = Symbol.FRESH;
		} else {
			checkedSymbol = Symbol.INV;
		}
		return checkedSymbol;
	}
	
	protected void checkInit() {
		boolean sentRST = lastFlagsSent.has(Flag.RST) && this.lastAbsSeqSent.is(Symbol.V);
		boolean receivedRST = !isLastResponseTimeout && lastFlagsReceived.has(Flag.RST);
		
		
		if (!freshSeqEnabled && (receivedRST || sentRST) || this.lastConcSeqReceived == 0) {
			freshSeqEnabled = true;
		} else {
			freshSeqEnabled = !(lastFlagsSent.has(Flag.SYN)  
					&& this.lastConcAckReceived == this.lastConcSeqSent + 1)
					&& freshSeqEnabled;
		}

		if (receivedRST || sentRST) {
			freshAckEnabled = true;
		} else if (lastConcSeqReceived == lastConcAckSent || (!isLastResponseTimeout && lastFlagsReceived.is(Flag.SYN))) {
			freshAckEnabled = false;
		}
	}

	public String getState() {
		return "MAPPER [FRESH_SEQ=" + this.freshSeqEnabled + "; " +
				"FRESH_ACK=" + this.freshAckEnabled + "; " +
				"lastSeqSent=" + this.lastConcSeqSent + 
				"; lastAckSent=" + this.lastConcAckSent + 
				"; clientSeq=" + this.learnerSeq + 
				"; serverSeq=" + this.sutSeq + "]";
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
			long concreteAck) {
		
		/* generate enum classes */
		FlagSet flagSet = new FlagSet(flags.toCharArray());
		
		/* call actual method */
		String abstractOutput = processIncomingResponse(flagSet, concreteSeq, concreteAck);
		return abstractOutput;
	}

	/* compatibility version */
	public String processIncomingResponseComp(String flags, String seqReceived,
			String ackReceived) {
		long seq = Long.valueOf(seqReceived);
		long ack = Long.valueOf(ackReceived);
		String abstractOutput = processIncomingResponse(flags, seq, ack);
		return abstractOutput;
	}
	/*public static void main(String[] args) {
		TCPMapper mapper = new TCPMapper(null);
		for (int i = 0; i < 1000; i++) {
			System.out.println(mapper.newInvalidOutsideWindow(10000));
			System.out.println(mapper.newInvalidWithinWindow(10000));	
		}
	}*/
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Mapper state:\n");
		
		sb.append("serverSeq: " + sutSeq + "   ");
		sb.append("clientSeq: " + learnerSeq + "   ");
//		sb.append("packetSent: " + lastPacketSent + "   ");
//		sb.append("packetRecv: " + lastPacketReceived + "   ");
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
		sb.append("isLastResponseTimeout: " + isLastResponseTimeout + "   ");
		/*
		 * boolean state variables, determined from data variables and the current
		 * values of the boolean variables
		 */
		return sb.toString();
	}
}
