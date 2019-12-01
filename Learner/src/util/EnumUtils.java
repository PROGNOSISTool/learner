package util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class EnumUtils {
	public static boolean is(Class<?> enumClass, String test) {

	    for (String c : getEnumStrings(enumClass)) {
	        if (c.equals(test)) {
	            return true;
	        }
	    }

	    return false;
	}
	
	public static List<String> getEnumStrings(Class<?> c) {
		ArrayList<String> enumStrings = new ArrayList<String>();
		for(Field field : c.getFields()) {
			if(field.isEnumConstant()) {
				enumStrings.add(field.getName());
			}
		}
		return enumStrings;
	}
}
