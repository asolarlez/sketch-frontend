package sketch.compiler.passes.structure;

import static sketch.util.Misc.nonnull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.util.Pair;
import sketch.util.datastructures.HashmapSet;

/**
 * determines which functions call which functions, and the closure.
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
        for (Pair<Function, Function> edge : edges.edges) {
            result.append("    " + edge.getFirst().getName() + " -> " +
                    edge.getSecond().getName() + "\n");
        }

        result.append("\n=== closure edges ===\n");
        for (Pair<Function, Function> edge : closureEdges.edges) {
            result.append("    " + edge.getFirst().getName() + " -> " +
                    edge.getSecond().getName() + "\n");
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
            edges.add(caller, target);
        }

        // compute closure
        for (Pair<Function, Function> edge : edges.edges) {
            addClosure(edge.getFirst(), edge.getSecond());
        }
    }

    protected void addClosure(Function first, Function target) {
        closureEdges.add(first, target);
        for (Function next : edges.multiEdges.getOrEmpty(target)) {
            addClosure(first, next);
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

    public static class CGEdgeSet {
        public HashSet<Pair<Function, Function>> edges;
        public HashmapSet<Function, Function> multiEdges;

        public CGEdgeSet() {
            this.edges = new HashSet<Pair<Function, Function>>();
            this.multiEdges = new HashmapSet<Function, Function>();
        }

        public void add(Function caller, Function target) {
            this.edges.add(new Pair<Function, Function>(caller, target));
            this.multiEdges.add(caller, target);
        }
    }
}
