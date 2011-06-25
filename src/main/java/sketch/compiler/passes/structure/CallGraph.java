package sketch.compiler.passes.structure;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.util.datastructures.HashmapSet;
import sketch.util.datastructures.ObjPairBase;
import sketch.util.datastructures.TypedHashMap;
import sketch.util.datastructures.TypedHashSet;
import sketch.util.fcns.IsChanging;
import static sketch.util.Misc.nonnull;

/**
 * determines which functions call which functions, and the closure. debug print code
 * commented out in GlobalsToParams visitor (if it's not working correctly, or you want to
 * see output).
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class CallGraph extends FEReplacer {
    public CGEdgeSet<CallEdge> edges = new CGEdgeSet<CallEdge>();
    public CGEdgeSet<CallEdge> closureEdges = new CGEdgeSet<CallEdge>();
    public TypedHashSet<Function> allFcns = new TypedHashSet<Function>();

    /** functions in which the call is made */
    protected final TypedHashMap<ExprFunCall, Function> fcnCallEnclosing =
            new TypedHashMap<ExprFunCall, Function>();
    protected final TypedHashMap<String, Function> fcnDefs =
            new TypedHashMap<String, Function>();
    protected final TypedHashMap<Function, Function> sketchOfSpec =
            new TypedHashMap<Function, Function>();

    /** for the visitor only */
    protected Function enclosing;

    public CallGraph(Program prog) {
        init(prog);
    }

    /** subclasses should call this [implicitly] so they get their fields initialized before buildEdges */
    protected CallGraph() {}

    protected void init(Program prog) {
        prog.accept(this);
        buildEdges();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("=== edges ===\n");
        for (CallEdge edge : edges) {
            result.append("    " + edge.caller().getName() + " -> " +
                    edge.target().getName() + "\n");
        }

        result.append("\n=== closure edges ===\n");
        for (CallEdge edge : closureEdges.edges) {
            result.append("    " + edge.caller().getName() + " -> " +
                    edge.target().getName() + "\n");
        }
        return result.toString();
    }

    @Override
    public Object visitExprFunCall(ExprFunCall exp) {
        assert enclosing != null : "function call not within a function?";
        fcnCallEnclosing.put(exp, enclosing);
        return exp;
    }

    @Override
    public Object visitFunction(Function func) {
        final Function putResult = fcnDefs.put(func.getName(), func);
        assert putResult == null;
        allFcns.add(func);
        enclosing = func;
        return super.visitFunction(func);
    }

    public Function getEnclosing(ExprFunCall call) {
        return nonnull(fcnCallEnclosing.get(call));
    }

    public Function getTarget(ExprFunCall call) {
        return nonnull(fcnDefs.get(call.getName()));
    }

    public Function getByName(String name) {
        return nonnull(fcnDefs.get(name));
    }

    public Function getSketchOfSpec(Function spec) {
        return nonnull(sketchOfSpec.get(spec));
    }

    public boolean isSketchOrSpec(Function fcn) {
        return (fcn.getSpecification() != null) || (sketchOfSpec.containsKey(fcn));
    }

    protected void buildEdges() {
        for (Entry<ExprFunCall, Function> ent : fcnCallEnclosing.entrySet()) {
            final Function caller = ent.getValue();
            final String name = ent.getKey().getName();
            final Function target =
                    nonnull(fcnDefs.get(name), "Unknown function \"" + name + "\" called");
            edges.add(new CallEdge(caller, target));
        }

        // compute closure
        IsChanging ic = new IsChanging();
        while (ic.cond(edges.edges.size())) {
            for (CallEdge edge : edges) {
                addClosure(edge);
            }
        }

        // compute sketch relations
        for (Function ent : fcnDefs.values()) {
            if (ent.getSpecification() != null) {
                sketchOfSpec.put(getByName(ent.getSpecification()), ent);
            }
        }
    }

    protected void addClosure(CallEdge edge) {
        if (!closureEdges.edges.contains(edge)) {
            closureEdges.add(edge);
            for (Function next : edges.targetsFrom(edge.target())) {
                addClosure(new CallEdge(edge.caller(), next));
            }
        }
    }

    /**
     * see addClosure() for contains queries
     * 
     * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
     * @license This file is licensed under BSD license, available at
     *          http://creativecommons.org/licenses/BSD/. While not required, if you make
     *          changes, please consider contributing back!
     */
    public static class CGEdgeSet<T extends CallEdge> implements Iterable<T> {
        protected TypedHashSet<T> edges = new TypedHashSet<T>();
        private HashmapSet<Function, Function> outgoingEdges =
                new HashmapSet<Function, Function>();
        private HashmapSet<Function, Function> reverseEdges =
                new HashmapSet<Function, Function>();

        public boolean add(T edge) {
            this.reverseEdges.add(edge.target(), edge.caller());
            this.outgoingEdges.add(edge.caller(), edge.target());
            return this.edges.add(edge);
        }

        public Set<Function> targetsFrom(Function source) {
            return outgoingEdges.getOrEmpty(source);
        }

        public Set<Function> callersTo(Function target) {
            return reverseEdges.getOrEmpty(target);
        }

        public Iterator<T> iterator() {
            return edges.iterator();
        }
    }

    // [start] CallEdge = (Function, Function)
    public static class CallEdge extends ObjPairBase<Function, Function> {
        public CallEdge(Function caller, Function target) {
            super(caller, target);
        }

        @Override
        public String toString() {
            return caller().getName() + " -:Calls-> " + target().getName();
        }

        public Function caller() {
            return left;
        }

        public Function target() {
            return right;
        }
    }
    // [end]
}
