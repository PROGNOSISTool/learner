package sutInterface.quic;

public enum Internal {
	RESET;

	public static boolean isInternalCommand(String message)  {
		for(Internal command : Internal.values()) {
			if(command.name().equalsIgnoreCase(message)) {
				return true;
			}
		}
		return false;
	}
}
