package sketch.util.thread;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import sketch.util.DebugOut;

/**
 * extremely inefficient reachability check for two objects from two different
 * threads checks to determine if pointers exist from one object to another
 * which are not annotated as MTSafe.
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class MTReachabilityCheck {
    public AtomicReference<Object> ref = new AtomicReference<Object>(null);
    public Semaphore lock = new Semaphore(0);
    public HashMap<Integer, NamedObject> objects_by_ref =
            new HashMap<Integer, NamedObject>();

    public void visit(String path, Object obj, Visitor v) {
        if (obj == null) {
            return;
        } else if (v.alreadyVisited(obj)) {
            return;
        }
        v.visitObject(path, obj);
        Class<?> cls = obj.getClass();
        if (cls.isPrimitive() || cls == Integer.class || cls == Long.class) {
            return;
        }
        // DebugOut.print("exploring object", obj, "of class", cls, "hash code",
        // System.identityHashCode(obj));
        if (cls.isArray()) {
            if (cls.getComponentType().isPrimitive()) {
                return;
            }
            Object[] arr = (Object[]) obj;
            for (Object elt : arr) {
                if (elt == obj) {
                    DebugOut.assertFalse("recursion");
                }
                // DebugOut.print("visit subarray of", cls.getComponentType());
                visit(path + ".[]", elt, v);
            }
        } else {
            if (cls.getPackage().getName().equals("java.lang")) {
                if (cls.getName().equals("java.lang.Integer")) {
                    return;
                } else if (cls.getName().equals("java.lang.Long")) {
                    return;
                } else if (cls.getName().equals("java.lang.Boolean")) {
                    return;
                }
            }
            HashSet<Field> fields = new HashSet<Field>();
            for (Class<?> cls2 = cls; cls2 != null; cls2 = cls2.getSuperclass())
            {
                for (Field f : cls2.getDeclaredFields()) {
                    f.setAccessible(true);
                    fields.add(f);
                }
            }
            for (Field f : fields) {
                try {
                    // simple recursion prevention
                    if (f.getClass().isPrimitive()) {
                        return;
                    }
                    if (f.get(obj) != obj && !f.getName().equals("value")) {
                        // DebugOut.print("visit field", f.getName(),
                        // "of class",
                        // cls);
                        visit(path + "." + f.getName(), f.get(obj), v);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    DebugOut.assertFalse("set access to true");
                }
            }
        }
    }

    public void check(Object obj) {
        if (ref.compareAndSet(null, obj)) {
            try {
                lock.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
                DebugOut.assertFalse("don't interrupt threads.");
            }
        } else {
            DebugOut.print_mt("checking...");
            visit("<root>", obj, new AddVisitor());
            DebugOut.print_mt("added", objects_by_ref.size(), "objects");
            CheckVisitor check_visitor = new CheckVisitor();
            visit("<root>", ref.get(), check_visitor);
            DebugOut.print_mt("num reachable", check_visitor.num_reachable);
            lock.release();
        }
        System.exit(0); // @code standards ignore
    }

    public abstract class Visitor {
        Vector<Object> visited = new Vector<Object>();

        public abstract void visitObject(String name, Object obj);

        public boolean alreadyVisited(Object obj) {
            boolean result = visited.contains(obj);
            visited.add(obj);
            return result;
        }
    }

    public class AddVisitor extends Visitor {
        @Override
        public void visitObject(String name, Object obj) {
            objects_by_ref.put(System.identityHashCode(obj), new NamedObject(
                    name, obj));
        }
    }

    public class CheckVisitor extends Visitor {
        public int num_reachable = 0;

        @Override
        public void visitObject(String name, Object obj) {
            NamedObject result =
                    objects_by_ref.get(System.identityHashCode(obj));
            if (result != null) {
                DebugOut.print_mt("reachable object", name, obj);
                DebugOut.print_mt("                ", result.name, result.obj);
                num_reachable += 1;
            }
        }
    }

    public static class NamedObject {
        public String name;
        public Object obj;

        public NamedObject(String name, Object obj) {
            this.name = name;
            this.obj = obj;
        }
    }
}
