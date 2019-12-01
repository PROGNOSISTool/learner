package sutInterface.tcp;

import sutInterface.Serializer;

public class Packet implements TCPMessage {
	public final FlagSet flags;
	public final Symbol seq;
	public final Symbol ack;
	public final int payloadLength;

	public Packet(FlagSet flags, Symbol seq, Symbol ack, int payloadLength) {
		super();
		this.flags = flags;
		this.seq = seq;
		this.ack = ack;
		this.payloadLength = payloadLength;
	}
	
	public Packet(String packetString) {
		String[] parts = packetString.split("\\(|,|\\)"); 
		this.flags = new FlagSet(parts[0]);
		this.seq = Symbol.valueOf(parts[1]);
		this.ack = Symbol.valueOf(parts[2]);
		this.payloadLength = Integer.parseInt(parts[3]);
	}
	
	public int payload() {
		return flags.payload(); 
	}
	
	public String toString() {
		return flags + "(" + seq + "," + ack + ")";
	}
	
	public boolean matches(Packet packet) {
		boolean match = this.flags.matches(packet.flags) &&
				this.seq.matches(packet.seq) && this.ack.matches(packet.ack);
		return match;
	}
	
	public boolean matches(String packetString) {
		Packet packet = new Packet(packetString);
		return packet.matches(packet);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ack == null) ? 0 : ack.hashCode());
		result = prime * result + ((flags == null) ? 0 : flags.hashCode());
		result = prime * result + ((seq == null) ? 0 : seq.hashCode());
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
		if (ack != other.ack)
			return false;
		if (flags == null) {
			if (other.flags != null)
				return false;
		} else if (!flags.equals(other.flags))
			return false;
		if (seq != other.seq)
			return false;
		return true;
	}
	
	public String serialize() {
		char[] flagInitials = flags.toInitials();
		String seqString = seq.name();
		String ackString = ack.name();
		String result = Serializer.abstractMessageToString(flagInitials, seqString, ackString, this.payloadLength);
		return result;
	}
	
	public static void main(String arg []) {
		Packet a = new Packet(new FlagSet(Flag.ACK, Flag.SYN), Symbol.FRESH, Symbol.FRESH, 1);
		
		Packet b = new Packet(new FlagSet(Flag.ACK, Flag.RST), Symbol.FRESH, Symbol.FRESH, 0);
		
		System.out.println(a.flags.has(Flag.SYN));
		System.out.println(a.flags.has(Flag.FIN));
		System.out.println(b.flags.has(Flag.FIN));
		System.out.println(b.flags.has(Flag.RST));
		System.out.println(a);
	}
}
