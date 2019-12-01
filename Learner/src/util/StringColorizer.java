package util;

public class StringColorizer {
	private static final boolean COLORS_ARE_ACTUALLY_WORKING = false;
	
	public enum TextColor {
		BLACK(ANSI_BLACK),
		RED(ANSI_RED),
		GREEN(ANSI_GREEN),
		YELLOW(ANSI_YELLOW),
		BLUE(ANSI_BLUE),
		PURPLE(ANSI_PURPLE),
		CYAN(ANSI_CYAN),
		WHITE(ANSI_WHITE);
		
		private final String ansiCode;
		
		private TextColor(String ansiCode) {
			this.ansiCode = ansiCode;
		}
	}
	private static final String
		ANSI_RESET = "\u001B[0m",
		ANSI_BLACK = "\u001B[30m",
		ANSI_RED = "\u001B[31m",
		ANSI_GREEN = "\u001B[32m",
		ANSI_YELLOW = "\u001B[33m",
		ANSI_BLUE = "\u001B[34m",
		ANSI_PURPLE = "\u001B[35m",
		ANSI_CYAN = "\u001B[36m",
		ANSI_WHITE = "\u001B[37m";
	
	public static String toColor(String string, TextColor color) {
		if (COLORS_ARE_ACTUALLY_WORKING) {
			return color.ansiCode + string + StringColorizer.ANSI_RESET;
		} else {
			return string;
		}
	}
}
