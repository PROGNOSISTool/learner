package util;

import java.util.List;

public class OutputAction extends Action {

	public OutputAction(String methodName, List<Parameter> parameters) {
		super(methodName, parameters);
	}

	public OutputAction(Action action) {
		super(action);
	}
	
	public OutputAction(String action) {
		super(action);
	}
}
