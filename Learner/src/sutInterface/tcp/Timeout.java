package sutInterface.tcp;

public class Timeout implements TCPMessage {
	public String serialize() {
		return "TIMEOUT";
	}
}
