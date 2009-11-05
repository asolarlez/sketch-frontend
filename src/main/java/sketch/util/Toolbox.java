package sketch.util;

import java.io.IOException;

public class Toolbox {

	public static void pause(String message) {
		try {
			System.out.println(message);
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
