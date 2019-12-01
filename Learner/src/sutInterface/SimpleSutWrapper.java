package sutInterface;

import util.InputAction;
import util.OutputAction;

// SutWrapper which connects the learner directly to the i/o sut. 
public class SimpleSutWrapper implements SutWrapper {
	private SocketWrapper socket;
	
	public SimpleSutWrapper(int port) {
		socket = new SocketWrapper(port);
	}

	public OutputAction sendInput(InputAction symbolicInput) {
		// Send input to SUT
		String symbolicInputString = symbolicInput.getValuesAsString();
		socket.writeInput(symbolicInputString);

		// Receive output from SUT
		String symbolicOutputString = socket.readOutput();
		OutputAction symbolicOutput = new OutputAction(symbolicOutputString);
		
		return symbolicOutput;
	}

	public void sendReset() {
		socket.writeInput("reset");
	}
}
