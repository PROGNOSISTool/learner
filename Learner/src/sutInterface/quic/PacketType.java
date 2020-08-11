package sutInterface.quic;

import java.util.HashSet;

public enum PacketType {
	VERNEG,
	INITIAL,
	RETRY,
	HANDSHAKE,
	ZERO,
	SHORT;

	public String serialize() {
		return this.name();
	}

	public static HashSet<String> getPacketTypeStrings() {

		HashSet<String> values = new HashSet<String>();

		for (PacketType c : PacketType.values()) {
			values.add(c.name());
		}

		return values;
	}

	public static boolean isPacketType(String message)  {
		for(PacketType packetType : PacketType.values()) {
			if(packetType.name().equalsIgnoreCase(message)) {
				return true;
			}
		}
		return false;
	}

	public boolean matches(PacketType packetType) {
		return this.equals(packetType);
	}

	public boolean matches(String packetType) {
		return this.equals(PacketType.valueOf(packetType));
	}
}
