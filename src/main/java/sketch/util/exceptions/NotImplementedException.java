package sketch.util.exceptions;

/**
 * Non-proprietary API
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class NotImplementedException extends RuntimeException {
    private static final long serialVersionUID = -5696479711686997772L;

    public NotImplementedException() {}

    public NotImplementedException(String msg) {
        super(msg);
    }
}
