package sutInterface;

import util.InputAction;
import util.OutputAction;

public interface SutWrapper {
	
	// process input to an output 
	public OutputAction sendInput(InputAction symbolicInput);

	// reset SUT
	public void sendReset();

}
