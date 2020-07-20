package sutInterface.quic;

import java.util.*;

public class FrameSet {
	public static final FrameSet EMPTY = new FrameSet();
	private final Set<Frame> FrameSet;
	public FrameSet() {
		this.FrameSet = new TreeSet<Frame>();
	}
	public FrameSet(Frame... frames) {
		this();
		this.FrameSet.addAll(Arrays.asList(frames));
	}

	public FrameSet(String frames) {
		this();
		this.FrameSet.addAll(Frame.parseFrames(frames));
	}

	public Frame[] toFrameArray() {
		return this.FrameSet.toArray(new Frame[0]);
	}

	public String toString() {
		StringBuilder result = new StringBuilder();

		for (Frame frame : FrameSet) {
			result.append(frame.name());
			result.append(",");
		}
		result.setLength(result.length()-1);
		result.trimToSize();
		return result.toString();
	}

	public boolean has(Frame... frames) {
		return this.FrameSet.containsAll(Arrays.asList(frames));
	}

	public boolean is(Frame... frames) {
		return has(frames) && frames.length == this.FrameSet.size();
	}

	public int size() {
		return this.FrameSet.size();
	}

	public boolean matches(FrameSet frames) {
		boolean match = true;
		if(frames != null && this.size() == frames.size()) {
			Iterator<Frame> otherFrames = frames.FrameSet.iterator();
            for (Frame thisFrames : this.FrameSet) {
                match = thisFrames.matches(otherFrames.next());
                if (!match)
                	break;
            }
		} else {
			match = false;
		}
		return match;
	}

}
