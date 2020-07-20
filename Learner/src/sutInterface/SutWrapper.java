package sutInterface;

public interface SutWrapper {

	// process input to an output
	public String sendInput(String symbolicInput);

	// reset SUT
	public void sendReset();

}
