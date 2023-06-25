package learner;

import de.learnlib.api.SUL;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SocketSUL implements SUL<String, String> {
	private SocketWrapper socket;

	public SocketSUL(Config config) {
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

	public List<String> step(List<String> inputs) {
		return Arrays.asList(step(String.join(" ", inputs)).split(" "));
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
