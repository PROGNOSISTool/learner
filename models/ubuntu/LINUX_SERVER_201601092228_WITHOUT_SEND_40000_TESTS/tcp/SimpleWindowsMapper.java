package sutInterface.tcp;

import invlang.types.Flag;
import invlang.types.FlagSet;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import sutInterface.Serializer;
import util.Calculator;
import util.Log;
import util.RangePicker;

public class SimpleWindowsMapper implements MapperInterface {

	private long sutSeq;
	private long learnerSeq;
	private long learnerSeqProposed;
	private long oldSeq = -3;
	private Random random = new Random(0);
    private long lastAckReceived;
    private FlagSet lastFlagsSent;
	
	public Map<String, Object> getState() {
		Map<String, Object> mapperState = new LinkedHashMap<String, Object>();
		mapperState.put("sutSeq", sutSeq);
		mapperState.put("learnerSeq", learnerSeq);
		mapperState.put("learnerSeqProposed", learnerSeqProposed);
		mapperState.put("lastFlagsSent", lastFlagsSent);
		return mapperState;
	}
	
	// cases for nondet in TIME_WAIT state:
	// 1. [learnerSeq, learnerSeq + MaxNum/2) -> SYN+ACK
	// 2. learnerSeq + MaxNum/2 -> RST
	// 3. (learnerSeq + MaxNum/2, learnerSeq) -> timeout
	
	
	private long generateFreshSeq() {
//		LinkedList<Long> pointsOfInterest = new LinkedList<Long>();
//		pointsOfInterest.add(0L);
//		if (sutSeq != -3)
//			pointsOfInterest.add(sutSeq);
//		if (learnerSeq != -3)
//			pointsOfInterest.add(learnerSeq);
//		long newSeq = new RangePicker(random, 0, 0xffffffffL, pointsOfInterest).getRandom();
		long newSeq;
		if (oldSeq == -3) {
			newSeq = Calculator.randWithinRange(0, Calculator.MAX_NUM);
		} else {
			// we should generate random values greater than learnerSeq that are outside of the window ( I assume WINDOW_SIZE < 10000)
			newSeq = Calculator.randWithinRange(Calculator.sum(oldSeq, 10000), Calculator.sum(oldSeq, 20000));
		}
		oldSeq = newSeq;
		
		return newSeq;
	}
	
	enum abssut{
		NEXT,
		CURRENT,
		ZERO, 
		FRESH
	}
	
	enum abslearner{
		NEXT,
		CURRENT,
		ZERO, 
		FRESH
	}

	@Override
	public String processIncomingResponse(FlagSet flagsIn, long concSeqIn, long concAckIn,
			int concDataIn) {
		String absSeqIn;
		String absAckIn;
		
		if (concSeqIn == sutSeq+1) {
			absSeqIn = abssut.NEXT.toString();
		} else { if (concSeqIn == sutSeq) {
			absSeqIn = abssut.CURRENT.toString();
		} else { if (concSeqIn == 0) {
			absSeqIn = abssut.ZERO.toString();
		} else {
			absSeqIn = abssut.FRESH.toString();
		}}}
		if ((concAckIn == learnerSeq+1) | (concAckIn == learnerSeqProposed+1)) {
			absAckIn = abslearner.NEXT.toString();
		} else { if (concAckIn == learnerSeq) {
			absAckIn = abslearner.CURRENT.toString();
		} else { if (concAckIn == 0) {
			absAckIn = abslearner.ZERO.toString();
		} else {
			absAckIn = abslearner.FRESH.toString();
		}}}
		
		updateResponse(flagsIn, concSeqIn, concAckIn, concDataIn);
		
		return Serializer.abstractMessageToString(flagsIn, absSeqIn, absAckIn, concDataIn);
	}
	
	private void updateResponse(FlagSet flagsIn, long concSeqIn, long concAckIn,
			int concDataIn) {
	    if (this.lastFlagsSent != null && this.lastFlagsSent.containsAll(Arrays.asList(Flag.SYN, Flag.ACK)) && flagsIn.contains(Flag.RST)) {
	        this.sutSeq = sutSeq;
	        this.learnerSeq = learnerSeq;
	        Log.err("cond active");
	    } else 
	        if ((flagsIn.contains(Flag.RST) ) | ((learnerSeqProposed != -3) & (concAckIn != learnerSeqProposed+1))) {
    			// upon reset, or if a fresh seq from the learner is not acknowledged
    			this.sutSeq = -3;
    			this.learnerSeq = generateFreshSeq();
	        } else { 
	            
	            if ((learnerSeqProposed != -3) | (concSeqIn == sutSeq+1)) {
        			// if a fresh seq from the learner is acknowledged, or if the sequence number is valid
        			if ((flagsIn.contains(Flag.SYN)) | (flagsIn.contains(Flag.FIN))) {
        				this.sutSeq = concSeqIn;
        			} else { 
        			    if (flagsIn.contains(Flag.PSH)) {
            				this.sutSeq = sutSeq + concDataIn;
            			} else {
            				this.sutSeq = sutSeq;
            			}
        			}
        		this.learnerSeq = concAckIn;
		} else {
		    if (flagsIn.contains(Flag.SYN)) {
    			// fresh sequence number
    			this.sutSeq = concSeqIn;
    			if (concAckIn == 0) {
    				this.learnerSeq = learnerSeq;
    			} else {
    				this.learnerSeq = concAckIn;
    			}
    		} else {
    			this.sutSeq = sutSeq;
    			this.learnerSeq = learnerSeq;
    		
    		}
		    }
	    }
		learnerSeqProposed = -3;
		lastAckReceived = concAckIn;
	}

	@Override
	public String processIncomingTimeout() {
		this.learnerSeqProposed = -3;
		this.sutSeq = sutSeq;
		this.learnerSeq = learnerSeq;
		return "TIMEOUT";
	}

	@Override
	public String processOutgoingRequest(FlagSet flagsOut, String absSeq,
			String absAck, int payloadLength) {
		long concSeqOut = -3, concAckOut = -3;
			if (learnerSeq != -3) {
			    concSeqOut = learnerSeq;
			} else {
			    if(flagsOut.contains(Flag.RST) && !flagsOut.contains(Flag.ACK) && this.lastAckReceived != -3) {
			        concSeqOut = this.lastAckReceived;
			    } 
			    else {
			        concSeqOut = generateFreshSeq();
			    }
			} 
		
		if(!flagsOut.contains(Flag.ACK)) {
		    concAckOut = Calculator.newValue();
		} else {
			if (sutSeq == -3) {
				concAckOut = 0;
			} else {
				concAckOut = sutSeq + 1;
			}
		}
		if (flagsOut.contains(Flag.RST)) {
			learnerSeqProposed = -3;
			sutSeq = -3;
			learnerSeq = -3;
		} else {
			if (learnerSeq == -3) {
				learnerSeqProposed = concSeqOut;
			} else {
				learnerSeqProposed = learnerSeqProposed;
			}

			sutSeq = sutSeq;
			learnerSeq = learnerSeq;
		}
		lastFlagsSent = flagsOut;
		return Serializer.concreteMessageToString(flagsOut, concSeqOut, concAckOut, payloadLength);
	}

	@Override
	public String processOutgoingAction(String action) {
		return action.toLowerCase();
	}

	@Override
	public void sendReset() {
		learnerSeqProposed = -3;
		sutSeq = -3;
		learnerSeq = -3;
		lastAckReceived = -3;
		lastFlagsSent = null;
	}

	@Override
	public String processOutgoingReset() {
		if (learnerSeq != -3) {
			return Serializer.concreteMessageToString(new FlagSet("RST"), learnerSeq,  0, 0);
		} else
			return Serializer.concreteMessageToString(new FlagSet("RST"), 0,  0, 0);
	}

	
	public static void main(String [] args) {
	}
}
