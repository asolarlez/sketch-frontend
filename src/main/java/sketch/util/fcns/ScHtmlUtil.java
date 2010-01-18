package sketch.util.fcns;

/**
 * static HTML functions
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class ScHtmlUtil {
    public static String html_tag_escape(String quoted) {
        return quoted.replace("<", "&lt;").replace(">", "&gt;");
    }

    /** converts tags and maintains line formatting */
    public static String html_nonpre_code(String text) {
        text = text.replace("  ", " &nbsp;");
        text = html_tag_escape(text);
        text = text.replace("\n", "<br />");
        return text;
    }
}
