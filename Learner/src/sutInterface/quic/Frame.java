package sutInterface.quic;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public enum Frame {
	CONNECTION_CLOSE,
	APPLICATION_CLOSE,
	PATH_RESPONSE,
	ACK,
	ACK_ECN,
	HANDSHAKE_DONE,
	CRYPTO,
	PING,
	NEW_CONNECTION_ID,
	RETIRE_CONNECTION_ID,
	PATH_CHALLENGE,
	RESET_STREAM,
	STOP_SENDING,
	MAX_DATA,
	MAX_STREAM_DATA,
	MAX_STREAMS_UNI,
	MAX_STREAMS_BIDI,
	DATA_BLOCKED,
	STREAM_DATA_BLOCKED,
	STREAMS_BLOCKED_UNI,
	STREAMS_BLOCKED_BIDI,
	NEW_TOKEN,
	STREAM,
	PADDING;

	public String toString() {
		return this.name();
	}

	public static HashSet<String> getFrameStrings() {

		HashSet<String> values = new HashSet<String>();

		for (Frame c : Frame.values()) {
			values.add(c.name());
		}

		return values;
	}

	public static boolean isFrame(String message)  {
		for(Frame frame : Frame.values()) {
			if(frame.name().equalsIgnoreCase(message)) {
				return true;
			}
		}
		return false;
	}

	public static Set<Frame> parseFrames(String frames) {
		Set<Frame> frameSet = new LinkedHashSet<Frame>();
		if(frames != null) {
			String uppedFrames = frames.toUpperCase();
			String[] frameStrings = uppedFrames.split(",");
			for (String frameString : frameStrings) {
				frameSet.add(Frame.valueOf(frameString));
			}
		}
		return frameSet;
	}

	public boolean matches(Frame frame) {
		return this.equals(frame);
	}

	public boolean matches(String frame) {
		return this.equals(Frame.valueOf(frame));
	}
}
