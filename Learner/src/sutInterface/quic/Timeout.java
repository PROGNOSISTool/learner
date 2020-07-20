package sutInterface.quic;

public class Timeout implements Message {
	public String serialize() {
		return "TIMEOUT";
	}
}
