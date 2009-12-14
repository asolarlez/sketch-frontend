package ec.util;

/**
 * ThreadLocal<MersenneTwisterFast> wrapper with initialValue() function
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class ThreadLocalMT extends ThreadLocal<MersenneTwisterFast> {
    public int salt = 0;
    public boolean use_current_time_millis = true;
    public static boolean disable_use_current_time_millis = false;
    public static ThreadLocalMT _mt = new ThreadLocalMT();

    public static MersenneTwisterFast mt() {
        return _mt.get();
    }

    public ThreadLocalMT() {
    }

    public ThreadLocalMT(int salt, boolean use_current_time_millis) {
        this.salt = salt;
        this.use_current_time_millis = use_current_time_millis;
    }

    @Override
    protected MersenneTwisterFast initialValue() {
        int salt_local = salt;
        if (use_current_time_millis && !disable_use_current_time_millis) {
            salt_local += System.currentTimeMillis();
        }
        return new MersenneTwisterFast(Thread.currentThread().getId()
                + salt_local);
    }
}
