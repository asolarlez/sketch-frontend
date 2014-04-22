package sketch.compiler.ast.core.exprs;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;

public class ExprTuple extends Expression
{
    /** list of Expressions that are the initial elements of the tuple */
    private List<Expression> elements;
    private String name;
  

    public ExprTuple(FENode node, Expression singleElem, String name) {
        super(node);
        this.elements = new ArrayList<Expression>(1);
        elements.add(singleElem);
        this.name = name;
    }

    public ExprTuple(FENode node, List<Expression> elements, String name)
    {
        super(node);
        this.elements = elements;
        this.name = name;
    
    }

    /**
     * Creates a new ExprTuple with the specified elements.
     * 
     * @deprecated
     */
    public ExprTuple(FEContext context, List<Expression> elements, String name)
    {
        super(context);
        this.elements = elements;
        this.name = name;
   
    }

    public String getName() {
        return name;
    }

    /** Returns the components of this.  The returned list is a list
     * of expressions.  */
    public List<Expression> getElements() { return elements; }

   
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprTuple(this);
    }

    /**
     * Determine if this expression can be assigned to. Tuple can never be assigned to.
     * 
     * @return always false
     */
    public boolean isLValue()
    {
        return false;
    }

    public String toString()
    {
    StringBuffer sb = new StringBuffer();
    sb.append("(");
    for (int i=0; i<elements.size(); i++) {
        sb.append(elements.get(i));
        if (i!=elements.size()-1) {
        sb.append(",");
        }
        if( i>100){ sb.append("...");break;}
    }
    sb.append(")");
        return sb.toString();
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof ExprTuple))
            return false;
        ExprTuple ao = (ExprTuple)o;
    for (int i=0; i<elements.size(); i++) {
        if (!(elements.get(i).equals(ao.elements.get(i)))) {
        return false;
        }
    }
    return true;
    }
}

