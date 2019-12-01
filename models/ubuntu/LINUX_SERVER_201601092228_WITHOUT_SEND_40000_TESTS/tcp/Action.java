package sutInterface.tcp;

import java.util.HashSet;


public enum Action implements TCPMessage{
	LISTEN, 
	ACCEPT,
	CONNECT,
	//CLOSECLIENT,
	//CLOSESERVER,
	EXIT,
	CLOSE,
	CLOSECONNECTION,
	SEND,
	RCV;
	
	
	public String serialize() {
		return this.name();
	}
	
	public static HashSet<String> getActionStrings() {

		  HashSet<String> values = new HashSet<String>();

		  for (Action c : Action.values()) {
		      values.add(c.name());
		  }

		  return values;
	}
	
	public static boolean isAction(String message)  {
		for(Action action : Action.values()) {
			if(action.name().equalsIgnoreCase(message)) {
				return true;
			}
		}
		return false;
	}
}
