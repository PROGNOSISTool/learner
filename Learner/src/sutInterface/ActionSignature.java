package sutInterface;

import java.util.ArrayList;
import java.util.List;

public class ActionSignature {
	private String methodName;
	private List<String> parameterTypes;

	public ActionSignature(String methodName, List<String> parameterTypes) {
		this.methodName = methodName;
		this.parameterTypes = new ArrayList<String>(parameterTypes);
	}

	public String getMethodName() {
		return methodName;
	}

	public List<String> getParameterTypes() {
		return new ArrayList<String>(parameterTypes);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (!(obj instanceof ActionSignature))
			return false;

		ActionSignature that = (ActionSignature) obj;

		if (!methodName.equals(that.methodName)) {
			return false;
		}
		
		if(!parameterTypes.equals(that.parameterTypes)) {
			return false;
		}

		return true;
	}
	
	@Override
	public int hashCode() {
		return methodName.hashCode() + parameterTypes.hashCode();
	}
}
