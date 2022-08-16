package utils;

import java.util.function.Supplier;
import java.util.logging.Logger;

public class Log {
	
	private static final Logger LOGGER = Logger.getGlobal();
	
	private static Supplier<String> qualified(Supplier<String> message) {
		return () -> "dit.jvmhost.net|zoom: " + message.get();
	}
	
	public static void severe(Supplier<String> message) {
		LOGGER.severe(qualified(message));
	}
	
	public static void warning(Supplier<String> message) {
		LOGGER.warning(qualified(message));
	}
	
	public static void info(Supplier<String> message) {
		LOGGER.info(qualified(message));
	}
	
}
