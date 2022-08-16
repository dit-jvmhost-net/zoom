package utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class J9 {

	/**
	 * @since Java 9
	 */
	public static byte[] readAllBytes(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		int read = input.read();
		while (read != -1) {
			output.write(read);
			read = input.read();
		}
		return output.toByteArray();
	}

}
