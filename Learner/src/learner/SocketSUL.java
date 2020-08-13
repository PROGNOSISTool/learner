package learner;

import de.learnlib.api.SUL;
import sutInterface.SocketWrapper;

import java.io.IOException;

public class SocketSUL implements SUL<String, String> {
	private SocketWrapper socket;

	public SocketSUL(SULConfig config) {
		try {
			this.socket = new SocketWrapper(config.sutIP, config.sutPort);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void pre() {
		socket.writeInput("RESET");
		socket.readOutput();
	}

	@Override
	public void post() {
	}

	@Override
	public String step(String input) {
		socket.writeInput(input);
		return socket.readOutput();
	}

	public void stop() {
		socket.writeInput("STOP");
	}

	@Override
	public boolean canFork() {
		return false;
	}

	@Override
	public SUL<String, String> fork() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Cannot fork SocketSUL");
	}
}
