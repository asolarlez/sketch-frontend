package sketch.compiler.smt.partialeval;

public class NodeReplacer extends FormulaVisitor {

    public NodeReplacer(NodeToSmtVtype formula) {
        super(formula);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public Object visitOpNode(OpNode opNode) {
        boolean changed = false;
        NodeToSmtValue[] newOpnds = new NodeToSmtValue[opNode.getOperands().length];
        
        int i = 0;
        for (NodeToSmtValue oldNode : opNode.getOperands()) {
            NodeToSmtValue newNode = (NodeToSmtValue) oldNode.accept(this);
            
            if (newNode != oldNode) {
                changed = true;
            }
            newOpnds[i] = newNode;
            
            i++;
        }
        
        if (changed) {
            OpNode newOpNode = new OpNode(opNode.getType(), opNode.getNumBits(), opNode.getOpcode(), newOpnds);
            return mFormula.checkCache(newOpNode);
        } else {
            return opNode;
        }
    }
}
