package sketch.compiler.passes.structure;

import static sketch.util.Misc.nonnull;

import java.util.HashMap;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.util.datastructures.HashmapSet;
import sketch.util.datastructures.ObjPairBase;
import sketch.util.datastructures.TypedHashSet;

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
    public CGEdgeSet edges = new CGEdgeSet();
    public CGEdgeSet closureEdges = new CGEdgeSet();

    protected final HashMap<ExprFunCall, Function> fcnCalls =
            new HashMap<ExprFunCall, Function>();
    protected final HashMap<String, Function> fcnDefs = new HashMap<String, Function>();

    /** for the visitor only */
    protected Function enclosing;

    public CallGraph(Program prog) {
        prog.accept(this);
        buildEdges();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("=== edges ===\n");
        for (CallEdge edge : edges.edges) {
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

    public Function getEnclosing(ExprFunCall call) {
        return nonnull(fcnCalls.get(call));
    }

    public Function getTarget(ExprFunCall call) {
        return nonnull(fcnDefs.get(call.getName()));
    }

    protected void buildEdges() {
        for (Entry<ExprFunCall, Function> ent : fcnCalls.entrySet()) {
            final Function caller = ent.getValue();
            final Function target = fcnDefs.get(ent.getKey().getName());
            edges.add(new CallEdge(caller, target));
        }

        // compute closure
        for (CallEdge edge : edges.edges) {
            addClosure(edge);
        }
    }

    protected void addClosure(CallEdge edge) {
        if (!closureEdges.edges.contains(edge)) {
            closureEdges.add(edge);
            for (Function next : edges.multiEdges.getOrEmpty(edge.target())) {
                addClosure(new CallEdge(edge.caller(), next));
            }
        }
    }

    @Override
    public Object visitExprFunCall(ExprFunCall exp) {
        assert enclosing != null : "function call not within a function?";
        fcnCalls.put(exp, enclosing);
        return exp;
    }

    @Override
    public Object visitFunction(Function func) {
        final Function putResult = fcnDefs.put(func.getName(), func);
        assert putResult == null;
        enclosing = func;
        return super.visitFunction(func);
    }

    /**
     * see addClosure() for contains queries
     * 
     * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
     * @license This file is licensed under BSD license, available at
     *          http://creativecommons.org/licenses/BSD/. While not required, if you make
     *          changes, please consider contributing back!
     */
    public static class CGEdgeSet {
        public TypedHashSet<CallEdge> edges;
        public HashmapSet<Function, Function> multiEdges;

        public CGEdgeSet() {
            this.edges = new TypedHashSet<CallEdge>();
            this.multiEdges = new HashmapSet<Function, Function>();
        }

        public void add(CallEdge edge) {
            this.edges.add(edge);
            this.multiEdges.add(edge.caller(), edge.target());
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
