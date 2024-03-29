package learner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

// Wrapper around the java Socket so we have clear segmentation of inputs and outputs
public class SocketWrapper {
	private static final Map<Integer, Socket> socketMap = new HashMap<>();
	protected Socket sock;
	protected PrintWriter sockout;
	protected BufferedReader sockin;


	public SocketWrapper(String sutIP, int sutPort) throws IOException {
		if(socketMap.containsKey(sutPort)) {
			sock = socketMap.get(sutPort);
		} else {
			sock = new Socket(sutIP, sutPort);
			socketMap.put(sutPort, sock);
		}
		sockout = new PrintWriter(sock.getOutputStream(), true);
		sockin = new BufferedReader(new InputStreamReader(
				sock.getInputStream()));
	}

    public void writeInput(String input) {
	    if (sockout != null) {
		sockout.println(input);
		sockout.flush();
	    }
	}

	public String readOutput() {
		String output = null;
		try {
			output = sockin.readLine();
			if (output == null) {
				throw new RuntimeException("socket closed!");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}
}
