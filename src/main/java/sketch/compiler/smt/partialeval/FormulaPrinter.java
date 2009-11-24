package sketch.compiler.smt.partialeval;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.smt.SMTTranslator;

public abstract class FormulaPrinter extends SMTTranslator{
    
    public static final boolean USE_LET = CommandLineParamManager.getParams().hasFlag("uselet");

    protected NodeToSmtVtype mFormula;
    PrintStream out;
    
    public FormulaPrinter(NodeToSmtVtype formula, PrintStream out) {
        super();
        
        mFormula = formula;
        this.out = out;
        
    }
    
    public void printRenamableDeclarations() {
        addComment("");
        addComment("Renamable Variable Declarations");
        addComment("");
        
        for (VarNode p : mFormula.mRenamableVars) {              
            out.println(getDefineVar(p));
        }
    }
    
    public void printFixedNameDeclarations() {

        addComment("");
        addComment("Fixed Name Variable Declarations");
        addComment("");
        for (VarNode p : mFormula.mFixedNameVars) {
            out.println(getDefineVar(p));
        }
    
    }

    private void printHoleDeclarations() {
        addComment("");
        addComment("Holes Declarations");
        addComment("");
        for (VarNode p : mFormula.mHoles) {
            out.println(getDefineVar(p));
        }
    }

    private void printInputDeclarations() {
        addComment("");
        addComment("Inputs Declarations");
        addComment("");
        for (VarNode p : mFormula.mInputs) {
            out.println(getDefineVar(p));
        }
    }
    
    
    public void printDAG() {
        
        addComment("");
        addComment("DAG");
        addComment("");
        
        if (USE_LET) {
            
            out.println(getLetHead());
        }
        
        for (NodeToSmtValue dest : mFormula.mEq) {
            
            if (dest instanceof LabelNode) {
                addComment(dest.toString());
            } else {
                NodeToSmtValue def = mFormula.mSimpleDefs.get(dest);
                if (USE_LET) {
                    String defStr = getLetLine(dest, def);
                    out.println(defStr);    
                } else {
                    String defStr = getAssert(getStr(mFormula.assign(dest, def)));
                    out.println(defStr);
                }
            }
        }       
    }
    
    /**
     * Helper that generates the correctness condition for the entire program
     * at the end.
     */
    protected void generateCorrectnessConditions(boolean negated) {
        // print out the correctness conditions
        addComment("");
        addComment("Correctness Conditions");
        addComment("");
        

        if (!negated) {
            if (USE_LET) {
                NodeToSmtValue[] asserts = new NodeToSmtValue[mFormula.mAsserts.size()];
                int i = 0;
                for (NodeToSmtValue oneAssert : mFormula.mAsserts) {
                    asserts[i] = oneAssert;
                    i++;
                }
                out.println(getLetFormula(mFormula.and(asserts)));
                
            } else {
                            
                // output a disjunction of all asserts
                for (NodeToSmtValue predicate : mFormula.mAsserts) {
                    out.println(getAssert(getStr(predicate)));
                }
            
            }
        
            

        } else {
            if (mFormula.mAsserts.size() > 0) {

                NodeToSmtValue[] negatedAsserts = new NodeToSmtValue[mFormula.mAsserts.size()];
                
                int i = 0;
                for (NodeToSmtValue oneAssert : mFormula.mAsserts) {
                    negatedAsserts[i] = mFormula.not(oneAssert);
                    i++;
                }
                
                NodeToSmtValue finalCondition = mFormula.or(negatedAsserts);
                if (USE_LET) {
                    out.println(getLetFormula(finalCondition));
                } else {
                    out.println(getAssert(getStr(finalCondition)));    
                }
                
            } else {
                // if there is no assertion in a verification phase, that means the program
                // can never fail, in which case, verification can fail immediately
                if (USE_LET) {
                    out.println(getLetFormula(mFormula.CONST(false)));
                        
                } else {
                    out.println(getAssert(getStr(mFormula.CONST(false))));
                }
            }
        }
        if (USE_LET)
            out.println(getLetTail(mFormula.mEq.size()));
    }
    
    public void printSynthesisFormula(ArrayList<SmtValueOracle> observations) {
        
        try {
            
            out.println(prolog());
//          println(getComment("intbits=" + mIntBits + " cbits=" + mcbits + " inbits=" + minbits));
            
            if (USE_LET)
                printHoleDeclarations();
            else
                printFixedNameDeclarations();
            
            
            for (int currObserIdx = 0; currObserIdx < observations.size(); currObserIdx++) {
                String comment = "Formula for observation " + currObserIdx;
                
                mFormula.setSuffix("");
                HashMap<NodeToSmtValue, NodeToSmtValue> valueAssignments = useOracle(observations.get(currObserIdx), true);
                mFormula.setSuffix("s" + currObserIdx);
                
                addComment(comment);
        
                if (USE_LET)
                    printInputDeclarations();
                else
                    printRenamableDeclarations();
                
                printValueAssignments(valueAssignments);
                printDAG();
                generateCorrectnessConditions(false);
                
            }
            
            out.println(epilog());
            
        } catch (AssertionFailedException e) {
            throw e;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw e;
        } finally {
            
        }
    }
    
    public void printVerificaitonFormula(SmtValueOracle candidate) {
    
        try {
            
            HashMap<NodeToSmtValue, NodeToSmtValue> valueAssignments = useOracle(candidate, false);
            
            mFormula.setSuffix("");
            out.println(prolog());
            printFixedNameDeclarations();
//          println(getComment("intbits=" + mIntBits + " cbits=" + mcbits + " inbits=" + minbits));
            
            addComment("Verification formulas");
        
            
            if (USE_LET)
                printInputDeclarations();
            else
                printRenamableDeclarations();
            
            printValueAssignments(valueAssignments);
            printDAG();
            generateCorrectnessConditions(true);
            out.println(epilog());
            
    
        } catch (AssertionFailedException e) {
            throw e;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw e;
        } finally {

        }
    
    }
    
    public HashMap<NodeToSmtValue, NodeToSmtValue> useOracle(SmtValueOracle oracle, boolean isSynthesis) {
        
        Collection<VarNode> inlets;
        if (isSynthesis) {
//          mSharedFalseValue.obj = 0;
            inlets = mFormula.mInputs;
        } else {
//          mSharedFalseValue.obj = 1;
            inlets = mFormula.mHoles;
        }
        
        HashMap<NodeToSmtValue, NodeToSmtValue> valueAssignments = new HashMap<NodeToSmtValue, NodeToSmtValue>();
        
        
        for (VarNode var : inlets) {
            // CAUTION, for synthesis, oracle is from verification, no suffix
            // the way to solve this problem is to retrieve the values from the oracles first.
            
            // for verification, oracle is from synthesis, has suffix, but we
            // only care about the hole variables, which don't have suffix, so
            // it's ok.
            NodeToSmtValue rhs = oracle.getValueForVariable(var);
            
            NodeToSmtValue lhs = var;
            valueAssignments.put(lhs, rhs);
        }
        
        return valueAssignments;
    }
    
    public void printValueAssignments(HashMap<NodeToSmtValue, NodeToSmtValue> valueAssignments) {
        addComment("");
        addComment("Input/Hole assignments");
        addComment("");
        for (NodeToSmtValue lhs :  valueAssignments.keySet()) {
            NodeToSmtValue rhs = valueAssignments.get(lhs);
            out.println(getAssert(getStr(mFormula.eq(lhs, rhs))));
        }
    }
    
    /**
     * Add comment to the formula file
     * @param msg
     */
    public void addComment(String msg) {
        out.println(getComment(msg));
    }   
}


