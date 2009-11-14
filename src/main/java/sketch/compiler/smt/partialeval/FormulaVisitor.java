package sketch.compiler.smt.partialeval;

public abstract class FormulaVisitor {
    
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
}
