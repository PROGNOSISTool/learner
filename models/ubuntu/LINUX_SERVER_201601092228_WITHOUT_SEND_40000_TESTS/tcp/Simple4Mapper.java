package sutInterface.tcp;

import invlang.types.Flag;
import invlang.types.FlagSet;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import sutInterface.Serializer;
import util.Calculator;

public class Simple4Mapper implements MapperInterface {

	private long sutSeq;
	private long learnerSeq;
	private long learnerSeqProposed;
	private Random random = new Random(0);
	
	public Map<String, Object> getState() {
		Map<String, Object> mapperState = new LinkedHashMap<String, Object>();
		mapperState.put("sutSeq", sutSeq);
		mapperState.put("learnerSeq", learnerSeq);
		mapperState.put("learnerSeqProposed", learnerSeqProposed);
		return mapperState;
	}
	
	private static long diff = -3;
	
	
	// 1. same fresh syn as before or learnerSeq  
	// 2. a fresh syn within [learnerSeq+1, learnerSeq + MaxNum/2]
	// 3. a fresh syn between [learnerSeq + MaxNum/2, learnerSeq-1]
	
	
	
	private long generateFreshSeq() {
		long newSeq;
		if (learnerSeq == -3) {
			newSeq = 0L;
			//newSeq = 4156818464L;
		} else {
			newSeq = learnerSeq; //2130382939L;
			//2140382939 (syn)  2150382939 (no syn)
			//newSeq = 1992234107L;//Calculator.randWithinRange(learnerSeq-10, learnerSeq + 10);
			newSeq = Calculator.sub(newSeq, diff);
			if (diff >= 4){
				newSeq = Calculator.sum(newSeq, Calculator.MAX_NUM/2 + 6);
			}
			
		}
		
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
		if ((flagsIn.contains(Flag.RST)) | ((learnerSeqProposed != -3) & (concAckIn != learnerSeqProposed+1))) {
			// upon reset, or if a fresh seq from the learner is not acknowledged
			this.sutSeq = -3;
			this.learnerSeq = generateFreshSeq();
		} else { if ((learnerSeqProposed != -3) | (concSeqIn == sutSeq+1)) {
			// if a fresh seq from the learner is acknowledged, or if the sequence number is valid
			if ((flagsIn.contains(Flag.SYN)) | (flagsIn.contains(Flag.FIN))) {
				this.sutSeq = concSeqIn;
			} else { if (flagsIn.contains(Flag.PSH)) {
				this.sutSeq = sutSeq + concDataIn;
			} else {
				this.sutSeq = sutSeq;
			}}
			this.learnerSeq = concAckIn;
		} else {if (flagsIn.contains(Flag.SYN)) {
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
		}}}
		learnerSeqProposed = -3;
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
			if (learnerSeq == -3) {
				concSeqOut = generateFreshSeq();
			} else {
				concSeqOut = learnerSeq;
			}
		
			if (sutSeq == -3) {
				concAckOut = 0;
			} else {
				concAckOut = sutSeq + 1;
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
		diff ++;
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
