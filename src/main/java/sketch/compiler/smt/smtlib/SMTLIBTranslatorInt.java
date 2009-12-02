package sketch.compiler.smt.smtlib;

import java.io.PrintStream;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtType;

public class SMTLIBTranslatorInt extends SMTLIBTranslatorBV {

    public SMTLIBTranslatorInt(NodeToSmtVtype formula, PrintStream ps)
    {
        super(formula, ps, -1);
    }
    
    @Override
    public String prolog() {
        StringBuffer sb = new StringBuffer();
        sb.append("(benchmark BenchmarkName.smt"); sb.append('\n');
        sb.append(":logic QF_AUFLIA"); sb.append('\n');
        return sb.toString();
    }
    
    @Override
    public String getIntLiteral(int i, int numBits) {
        return intToSmtLibInt(i);
    }
    
    public static String intToSmtLibInt(int i) {
        if (i >= 0 )
            return i + "";
        else
            return "(- " + -i + ")";
    }
    
    @Override
    protected void initOpCode() {

        opStrMap.put(OpCode.PLUS, "+");
        opStrMap.put(OpCode.MINUS, "-");
        opStrMap.put(OpCode.TIMES, "*");
        opStrMap.put(OpCode.OVER, "/");
        opStrMap.put(OpCode.MOD, "bvsmod");

        opStrMap.put(OpCode.EQUALS, "="); // logical equals
        // TODO maybe i need a separate numerical equals here

        opStrMap.put(OpCode.LT, "<");
        opStrMap.put(OpCode.LEQ, "<=");
        opStrMap.put(OpCode.GT, ">");
        opStrMap.put(OpCode.GEQ, ">=");

        
        opStrMap.put(OpCode.OR, "or");
        opStrMap.put(OpCode.AND, "and");
        opStrMap.put(OpCode.NOT, "not"); 
        opStrMap.put(OpCode.XOR, "xor");
        
        opStrMap.put(OpCode.NUM_NOT, "bvnot");
        opStrMap.put(OpCode.NUM_OR, "bvor");
        opStrMap.put(OpCode.NUM_AND, "bvand");
        opStrMap.put(OpCode.NEG, "bvneg");
        opStrMap.put(OpCode.NUM_EQ, "bvcomp");
        opStrMap.put(OpCode.NUM_XOR, "bvxor");
        opStrMap.put(OpCode.LSHIFT, "bvshl");
        opStrMap.put(OpCode.RSHIFT, "bvlshr");
        
        opStrMap.put(OpCode.CONCAT, "concat");
        opStrMap.put(OpCode.EXTRACT, "extract");
        opStrMap.put(OpCode.REPEAT, "repeat");
        opStrMap.put(OpCode.IF_THEN_ELSE, "ite");
        
        opStrMap.put(OpCode.ARRUPD, "store");
        opStrMap.put(OpCode.ARRACC, "select");
        // not equals needs to be done with special case in binary operator
    }
    
    @Override
    public String getTypeForSolver(SmtType type) {
        Type t = type.getRealType();
        if (t instanceof TypePrimitive) {
            if (t == TypePrimitive.bittype) {
                return "Int";
            } else if (t ==TypePrimitive.booltype) {
                return "BitVec[1]";
            } else if (t == TypePrimitive.nulltype || 
                    t == TypePrimitive.inttype ||
                    t instanceof TypeStruct || 
                    t instanceof TypeStructRef) {
                return "Int";
            }
            
        } else if (t instanceof TypeArray) {
            return "Int Int";
        } 
        assert false : "NOT implemented";
        return null;
    }
}
