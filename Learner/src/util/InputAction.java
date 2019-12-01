package util;

import java.util.List;

public class InputAction extends Action {

	public InputAction(String methodName, List<Parameter> parameters) {
		super(methodName, parameters);
	}

	public InputAction(Action action) {
		super(action);
	}
	
	public InputAction(String action) {
		super(action);
	}
}
