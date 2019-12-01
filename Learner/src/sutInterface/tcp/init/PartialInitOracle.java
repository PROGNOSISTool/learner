package sutInterface.tcp.init;

import sutInterface.tcp.Flag;
import sutInterface.tcp.Symbol;
import sutInterface.tcp.TCPMapper;

/**
 * Similar to the functional init oracle, but unlike it, the partial oracle doesn't always know the 
 * isResetting value
 */
public class PartialInitOracle implements InitOracle{

	public Boolean isResetting(TCPMapper mapper) {
		Boolean isInitial = null;
		
		// if we send a reset with a valid seq, then we should be in the init state
		if( mapper.isResponseTimeout && mapper.packetSent.flags.is(Flag.RST) && 
				mapper.packetSent.seq.is(Symbol.V)) {
            isInitial =  true;
	    } else {
	    	// if we send a packet without SYN or a RST flags, the init status shouldn't change
	    	if( (!mapper.packetSent.flags.has(Flag.SYN)) && (!mapper.packetSent.flags.has(Flag.RST)) ) {
	    		isInitial = mapper.freshSeqEnabled;
	    	} else {
	    		isInitial = null;
	    	}
	    }
		return isInitial;
	}

	public void setDefault() {
	}

}
