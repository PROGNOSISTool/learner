package sutInterface.quic;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class HeaderOptions {
	public final Long QUICVersion;

	public HeaderOptions(Long QUICVersion) {
		this.QUICVersion = QUICVersion;
	}

	public HeaderOptions(String QUICVersion) {
		// TODO: Check if "" -> null
		this.QUICVersion = Long.decode(QUICVersion);
	}

	public String toString() {
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putLong(this.QUICVersion);
		byte[] cut = Arrays.copyOfRange(bytes, 4, 8);
		StringBuilder QUICVersionString = new StringBuilder();
		QUICVersionString.append("0x");
		for (byte section : cut) {
			QUICVersionString.append(String.format("%02X", section));
		}
		return QUICVersionString.toString();
	}

	public boolean matches(HeaderOptions headerOptions) {
		return this.equals(headerOptions);
	}

	public boolean matches(String headerOptions) {
		return this.equals(new HeaderOptions(headerOptions));
	}
}
