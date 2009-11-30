package sketch.compiler.smt.partialeval;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import sketch.compiler.smt.SMTTranslator;

public abstract class FormulaPrinter extends SMTTranslator{

    protected NodeToSmtVtype mFormula;
    PrintStream out;
    HashSet<VarNode> mVarsDecalred;
    protected boolean isSynthesis;
    
    public NodeToSmtVtype getFormula() {
        return mFormula;
    }
    
    /**
     * 
     * @param var
     * @return true if var is defined in the current textual formula
     */
    private boolean isVarDeclaredInFormula(VarNode varNode) {
        return mVarsDecalred.contains(varNode);
    }
    
    private void defineVarInFormula(VarNode varNode) {
        if (!isVarDeclaredInFormula(varNode)) {
            out.println(getDefineVar(varNode));
            mVarsDecalred.add(varNode);
        }
            
    }
    
    public FormulaPrinter(NodeToSmtVtype formula, PrintStream out) {
        super();
        
        mFormula = formula;
        this.out = out;
        mVarsDecalred = new HashSet<VarNode>();
        
    }
    
    public void printInputDeclarations() {
        addComment("");
        addComment("Renamable Variable Declarations");
        addComment("");
        
        for (VarNode p : mFormula.mInputs) {              
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
    
    private void printArraySeedlDeclarations() {
        addComment("");
        addComment("Real Variable Declarations");
        addComment("");
        for (VarNode p : mFormula.mArraySeeds) {
            out.println(getDefineVar(p));
        }
    }
    
    
    public void printDAG() {
        
        addComment("");
        addComment("DAG");
        addComment("");
        
        for (NodeToSmtValue dest : mFormula.mEq) {
            
            if (dest instanceof LabelNode) {
                addComment(dest.toString());
            } else {
                NodeToSmtValue def = mFormula.mSimpleDefs.get(dest);
                String defStr = getLetLine(dest, def);
                out.println(defStr);
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
            // print disjunction of asserts
            NodeToSmtValue[] asserts = new NodeToSmtValue[mFormula.mAsserts.size()];
            int i = 0;
            for (NodeToSmtValue oneAssert : mFormula.mAsserts) {
                asserts[i] = oneAssert;
                i++;
            }
            out.println(getLetFormula(mFormula.and(asserts)));

        } else {
            if (mFormula.mAsserts.size() > 0) {

                NodeToSmtValue[] negatedAsserts = new NodeToSmtValue[mFormula.mAsserts.size()];
                
                int i = 0;
                for (NodeToSmtValue oneAssert : mFormula.mAsserts) {
                    negatedAsserts[i] = mFormula.not(oneAssert);
                    i++;
                }
                
                NodeToSmtValue finalCondition = mFormula.or(negatedAsserts);
                out.println(getLetFormula(finalCondition));
                
            } else {
                // if there is no assertion in a verification phase, that means the program
                // can never fail, in which case, verification can fail immediately

                out.println(getLetFormula(mFormula.CONST(false)));
            }
        }
        
    }
    
    public void printSynthesisFormula(ArrayList<SmtValueOracle> observations) {
        
        try {
            isSynthesis = true;
            out.println(prolog());
//          println(getComment("intbits=" + mIntBits + " cbits=" + mcbits + " inbits=" + minbits));
            
            printHoleDeclarations();
            
            for (int currObserIdx = 0; currObserIdx < observations.size(); currObserIdx++) {
                String comment = "Formula for observation " + currObserIdx;
                
                mFormula.setSuffix("");
                HashMap<NodeToSmtValue, NodeToSmtValue> valueAssignments = useOracle(observations.get(currObserIdx), true);
                mFormula.setSuffix("s" + currObserIdx);
                
                addComment(comment);
        
                printArraySeedlDeclarations();
                printValueAssignments(valueAssignments);
                printDAG();
                generateCorrectnessConditions(false);

                out.println(getLetTail(mFormula.mEq.size() + mFormula.mInputs.size()));
                
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
        isSynthesis = false;
        try {
            
            HashMap<NodeToSmtValue, NodeToSmtValue> valueAssignments = useOracle(candidate, false);
            
            mFormula.setSuffix("");
            out.println(prolog());
//            printFixedNameDeclarations();
//          println(getComment("intbits=" + mIntBits + " cbits=" + mcbits + " inbits=" + minbits));
            
            addComment("Verification formulas");
        
            printInputDeclarations();
            printArraySeedlDeclarations();
            
            printValueAssignments(valueAssignments);
            printDAG();
            generateCorrectnessConditions(true);
            
            out.println(getLetTail(mFormula.mEq.size()+ mFormula.mHoles.size()));
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
            if (BitVectUtil.isNormalArray(rhs.getType())) {
                VarNode seed = findArraySeed(rhs);
                defineVarInFormula(seed);
            }
        }
        
        out.println(getLetHead());
        
        for (NodeToSmtValue lhs :  valueAssignments.keySet()) {
            NodeToSmtValue rhs = valueAssignments.get(lhs);
            String defStr = getLetLine(lhs, rhs);
            out.println(defStr);

        }
    }
    
    /**
     * Add comment to the formula file
     * @param msg
     */
    public void addComment(String msg) {
        out.println(getComment(msg));
    }  
    
    
    /*
     * The following methods are used by the oracle to access the formula again
     */
    
    public boolean isHoleVariable(String varName) {
        return mFormula.isHoleVariable(stripVariableName(varName));
    }
    
    public boolean isInputVariable(String varName) {
        return mFormula.isInputVariable(stripVariableName(varName));
    }
    
    /**
     * Look up the NodeToSmtValue object in all the declared variables 
     * 
     * @param varName
     * @return
     */
    public VarNode getVarNode(String varName) {
        varName = stripVariableName(varName);
        for (VarNode renamable : mFormula.mInputs)
            if (renamable.getRHSName().equals(varName))
                return renamable;
        
        for (VarNode fixed : mFormula.mHoles)
            if (fixed.getRHSName().equals(varName))
                return fixed;
        
        for (VarNode fixed : mFormula.mArraySeeds)
            if (fixed.getRHSName().equals(varName))
                return fixed;
        
        return null;
    }
    
    public SmtType getTypeForVariable(String varName) {
        return getVarNode(varName).getSmtType();
    }
    
    /*
     * Helpers
     */
    private VarNode findArraySeed(NodeToSmtValue node) {
        if (node instanceof VarNode)
            return (VarNode) node;
        if (node instanceof OpNode) {
            OpNode arrUpdate = (OpNode) node;
            if (arrUpdate.getOpcode() == OpCode.ARRUPD)
            return findArraySeed(arrUpdate.getOperands()[0]);
        }
        throw new IllegalArgumentException("arr update node is expected");
    }
}


