package sketch.util.thread;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import sketch.util.DebugOut;

/**
 * A thread with the run() method separated into three sections: init(),
 * run_inner(), and finish(). run_inner() will be called continuously until a
 * stop request is enqueued on the thread.
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public abstract class InteractiveThread extends Thread {
    public Semaphore stop_event = new Semaphore(0);
    public int wait_time_millis;
    public float thread_time = 0.f;
    public final long start_millis;

    public InteractiveThread(float wait_secs) {
        wait_time_millis = (int) (wait_secs * 1000.f);
        start_millis = System.currentTimeMillis();
    }

    /** override if the thread needs to do anything at initialization */
    public void init() {
    }

    public void update_time() {
        thread_time = (System.currentTimeMillis() - start_millis) / 1000.f;
    }

    public abstract void run_inner();

    /** override if the thread needs to do anything before dying */
    public void finish() {
    }

    @Override
    public final void run() {
        init();
        try {
            while (!stop_event.tryAcquire(wait_time_millis,
                    TimeUnit.MILLISECONDS))
            {
                update_time();
                run_inner();
            }
        } catch (InterruptedException e) {
            DebugOut.print_exception(
                    "Interactive thread crashed with exception", e);
        }
        update_time();
        finish();
    }

    /** threadsafe signal to stop the thread */
    public final void set_stop() {
        stop_event.release();
    }
}
