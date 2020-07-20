package sutInterface;

// SutWrapper which connects the learner directly to the i/o sut.
public class SimpleSutWrapper implements SutWrapper {
	private SocketWrapper socket;

	public SimpleSutWrapper(int port) {
		socket = new SocketWrapper(port);
	}

	public String sendInput(String symbolicInput) {
		// Send input to SUT
		socket.writeInput(symbolicInput);

		// Receive output from SUT
		return socket.readOutput();
	}

	public void sendReset() {
		socket.writeInput("RESET");
	}

	public void close() {
		socket.close();
	}
}
