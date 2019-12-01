package sutInterface.tcp.init;

import sutInterface.tcp.Action;
import sutInterface.tcp.Flag;
import sutInterface.tcp.Symbol;
import sutInterface.tcp.TCPMapper;
import util.Log;

public class ClientInitOracle  implements InitOracle{
	
	/***
	 *  resetting function for Windows 8
	 */
	public Boolean isResetting(TCPMapper mapper) {
		Boolean result = null;
		
		//if (mapper.isMessageOutgoing) {
			boolean isFreshSeqEnabled = false;
			// when can you send a fresh seq?
			isFreshSeqEnabled = mapper.freshSeqEnabled && mapper.ackReceived != 0;
			result = isFreshSeqEnabled;
		//} 
		
		
		//else {
			boolean isFreshAckEnabled = false;
			// when can you send a fresh ack?
			if(mapper.isResponseTimeout) {
	        	//isInitial = mapper.isIncomingInit && mapper.lastPacketSent.flags.has(Flag.SYN);
			} else {
				//isFreshAckEnabled = mapper.freshAckEnabled && mapper.;
			}		
			result = isFreshAckEnabled;
		//}
		Log.info("Is initial state: " + result);
		return result;
	}

	/***
	 *  the resetting function is stateless 
	 */
	public void setDefault() {
		
	}
}
