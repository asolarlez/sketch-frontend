package sketch.compiler.smt.partialeval;

import java.util.LinkedList;

public abstract class FormulaVisitor {
    NodeToSmtVtype mFormula;
    
    public FormulaVisitor(NodeToSmtVtype formula) {
        mFormula = formula;
    }
 
    public Object visitVarNode(VarNode varNode) {
        return varNode;
    }
    
    public Object visitLabelNode(LabelNode labelNode) {
        return labelNode;
    }
    
    public Object visitOpNode(OpNode opNode) {
        return opNode;
    }
    
    public Object visitLinearNode(LinearNode linearNode) {
        return linearNode;
    }
    
    public Object visitConstNode(ConstNode constNode) {
        return constNode;
    }
    
    public void visitFormula() {
        // simplfy the term definitions
        for (NodeToSmtValue var : mFormula.mEq) {
            NodeToSmtValue def = mFormula.mSimpleDefs.get(var);
            
            NodeToSmtValue newDef = (NodeToSmtValue) def.accept(this);
            mFormula.mSimpleDefs.put(var, newDef);
            
        }
        
        // simplify the correctness conditions
        LinkedList<NodeToSmtValue> oldAsserts = mFormula.mAsserts;
        mFormula.mAsserts = new LinkedList<NodeToSmtValue>(); 
        for (NodeToSmtValue predicate : oldAsserts) {
            if (predicate instanceof OpNode) {
                NodeToSmtValue newPredicate = (NodeToSmtValue) predicate.accept(this);
                mFormula.mAsserts.add(newPredicate);
            }
        }
    }
}
