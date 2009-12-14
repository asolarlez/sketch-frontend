package sketch.util.thread;

import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import sketch.util.DebugOut;

/**
 * Asynchronous callback for events. Unlike e.g. condition variables, when set()
 * is called, all events enqueued before and after will be called. Semaphores
 * are similar but require blocking.
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class AsyncMTEvent {
    public final static long DONE_V = 10000000;
    public AtomicLong info;
    public ConcurrentLinkedQueue<QueueEntry> events;

    public void set_done() {
        info.addAndGet(DONE_V);
        execute_done();
    }

    /**
     * since this is executed at the end of both set_done() and enqueue(), no
     * schedule can result in
     * 
     * <pre>
     * enqueue()
     * info := DONE_V
     * &lt;exit by all threads&gt;
     * </pre>
     */
    protected void execute_done() {
        if (info.incrementAndGet() >= DONE_V) {
            try {
                while (true) {
                    events.remove().execute();
                }
            } catch (NoSuchElementException e) {
            }
        }
    }

    public void enqueue(Object target, String method, Object... args) {
        QueueEntry ent = new QueueEntry(target, method, args);
        if (info.get() >= DONE_V) {
            ent.execute();
        } else {
            events.add(ent);
        }
        execute_done();
    }

    public static class QueueEntry {
        private Method method;
        private Object target;
        private Object[] args;

        public QueueEntry(Object target, String method, Object[] args) {
            Class<?>[] param_types = new Class<?>[args.length];
            for (int a = 0; a < args.length; a++) {
                param_types[a] = args[a].getClass();
            }
            try {
                this.target = target;
                this.args = args;
                for (Method m : target.getClass().getMethods()) {
                    if (m.getName() == method
                            && m.getParameterTypes().length == args.length)
                    {
                        this.method = m;
                    }
                }
            } catch (Exception e) {
                DebugOut.print((Object[]) target.getClass().getMethods());
                DebugOut.assertFalse("QueueEntry with invalid "
                        + "method (not public?)", target, method, args);
            }
        }

        public void execute() {
            try {
                method.invoke(target, args);
            } catch (Exception e) {
                DebugOut.assertFalse("QueueEntry execute() invalid "
                        + "method (not public?)", "\ntarget:", target,
                        "\nmethod:", method, "\nargs:", args);
            }
        }
    }

    public void reset() {
        info = new AtomicLong(0);
        events = new ConcurrentLinkedQueue<QueueEntry>();
    }

    public boolean is_done() {
        return info.get() >= DONE_V;
    }
}
