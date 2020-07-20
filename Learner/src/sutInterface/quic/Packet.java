package sutInterface.quic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Packet implements Message {
	public final PacketType packetType;
	public final HeaderOptions headerOptions;
	public final FrameSet frames;

	public Packet(PacketType packetType, HeaderOptions headerOptions, FrameSet frames) {
		super();
		this.packetType = packetType;
		this.headerOptions = headerOptions;
		this.frames = frames;
	}

	public Packet(String packetString) {
		Pattern regex = Pattern.compile("^([A-Z]+)(\\(([0-9a-zx]+)\\))?\\[([A-Z,]+)]$");
		Matcher matcher = regex.matcher(packetString);
		this.packetType = PacketType.valueOf(matcher.group(1));
		this.headerOptions = new HeaderOptions(matcher.group(3));
		this.frames = new FrameSet(matcher.group(4));
	}

	public String toString() {
		return packetType.toString() +
				"(" + headerOptions.toString() + ")" +
				"[" + frames.toString() + "]";
	}

	public boolean matches(Packet packet) {
		return this.packetType.matches(packet.packetType) &&
				this.headerOptions.matches(packet.headerOptions) &&
				this.frames.matches(packet.frames);
	}

	public boolean matches(String packetString) {
		Packet packet = new Packet(packetString);
		return packet.matches(packet);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((packetType == null) ? 0 : packetType.hashCode());
		result = prime * result + ((headerOptions == null) ? 0 : headerOptions.hashCode());
		result = prime * result + ((frames == null) ? 0 : frames.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Packet other = (Packet) obj;
		if (!packetType.equals(other.packetType))
			return false;
		if (!headerOptions.equals(other.headerOptions))
			return false;
		return frames.equals(other.frames);
	}

	public String serialize() {
		return this.toString();
	}

	public static void main(String arg []) {
		Packet obj = new Packet(PacketType.INITIAL, new HeaderOptions("0xff00001d"), new FrameSet(Frame.CRYPTO));
		Packet string = new Packet("INITIAL(0xff00001d)[CRYPTO]");
		System.out.println(obj.equals(string));
	}
}
