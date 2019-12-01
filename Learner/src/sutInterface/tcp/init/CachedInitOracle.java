package sutInterface.tcp.init;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import sutInterface.tcp.TCPMapper;
import util.Log;

/**
 * An init oracle based on cache. If the init state after executing a trace is found in 
 * the cache, it is returned, otherwise the oracle returns null.
 */
public class CachedInitOracle implements InitOracle {
	private List<String> inputs = new ArrayList<String>();
	private InitCacheManager initCache;

	public CachedInitOracle(InitCacheManager initCache) {
		this.initCache = initCache;
	}

	/**
	 * Checks whether the input is resetting. If the trace is already present in
	 * the trace cache, then return the resetting state from the cache.
	 * Otherwise, use the checker to compute the reset state of the trace and
	 * store it in the cache.
	 */
	// TODO This taking the mapper and building the last input sent is not
	// pretty.
	public Boolean isResetting(TCPMapper mapper) {
		Boolean isResetting = false;
		String input;
		if (!mapper.isRequestAction)
			input = mapper.packetSent.serialize();
		else 
			input = mapper.actionSent.toString();
		append(input);
		Log.info("FETCHING init for trace " + inputs);
		isResetting = getTrace();
		if (isResetting != null) { 
			Log.info("TRACE FOUND " + isResetting);
			if (isResetting) {
				Log.info("defaulting");
				setDefault();
			}
		} else {
			Log.info("TRACE NOT FOUND");
		}
		return isResetting;
	}
	
	public void setDefault() {
		Log.info("DEFAULTED");
		inputs.clear();
	}

	private void append(String input) {
		inputs.add(input);
	}

	protected String[] getInputs() {
		return inputs.toArray(new String[inputs.size()]);
	}

	protected boolean getPreviousResettingState() {
		String[] inputs = getInputs();
		Boolean lastStoredState = null;
		while (inputs.length > 0 && initCache.getTrace(inputs) == null) {
			inputs = Arrays.copyOf(inputs, inputs.length - 1);
		}
		if (inputs.length > 0) {
			lastStoredState = initCache.getTrace(inputs);
		}
		return inputs.length == 0 || Boolean.TRUE.equals(lastStoredState);
	}

	protected Boolean getTrace() {
		return initCache.getTrace(getInputs());
	}

	protected void storeTrace(boolean value) {
		initCache.storeTrace(getInputs(), value);
		Log.info("STORING " + getTrace() + " for trace " + inputs);
	}
	
	protected void checkTrace(List<String> input, List<String> output) {
		initCache.checkTrace(input.toArray(new String[output.size()]), output.toArray(new String[output.size()]));
	}
	
	protected void checkTrace(List<String> output) {
		initCache.checkTrace(getInputs(), output.toArray(new String[output.size()]));
	}

}
