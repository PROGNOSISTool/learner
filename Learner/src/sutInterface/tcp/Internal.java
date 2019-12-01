package sutInterface.tcp;

public enum Internal{
	REVERT,
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
