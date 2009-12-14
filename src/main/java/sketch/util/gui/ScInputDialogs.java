package sketch.util.gui;

import javax.swing.JOptionPane;

/**
 * wrappers for user input dialogs
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class ScInputDialogs {
    public static int buttons_dialog(String question, Object caller,
            Object... options)
    {
        return JOptionPane.showOptionDialog(null, question, question + " - "
                + caller.toString() + " - sketch-util user input",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[options.length - 1]);
    }

    public static boolean yesno(String question, Object caller) {
        return buttons_dialog(question, caller, "Yes", "No") == 0;
    }
}
