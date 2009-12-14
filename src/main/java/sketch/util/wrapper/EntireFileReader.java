package sketch.util.wrapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * read an entire file python-style
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class EntireFileReader {
    public static String load_file(InputStream file_in) throws IOException {
        InputStreamReader in = new InputStreamReader(file_in);
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int length;
        while (true) {
            length = in.read(buffer);
            if (length <= 0) {
                break;
            }
            builder.append(buffer, 0, length);
        }
        return builder.toString();
    }

    public static String load_file(String filename) throws IOException {
        return load_file(new FileInputStream(filename));
    }
}
