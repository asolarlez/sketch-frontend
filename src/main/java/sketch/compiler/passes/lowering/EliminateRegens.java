/**
 *
 */
package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.regens.ExprAlt;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceBinary;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect.SelectChain;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect.SelectField;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect.SelectOrr;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect.SelectorVisitor;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceUnary;
import sketch.compiler.ast.core.exprs.regens.ExprParen;
import sketch.compiler.ast.core.exprs.regens.ExprRegen;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtLoop;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.spmd.exprs.SpmdNProc;
import sketch.compiler.ast.spmd.exprs.SpmdPid;
import sketch.util.Misc;


/**
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class EliminateRegens extends SymbolTableVisitor {
    protected TempVarGen varGen;
    boolean lhs = false;

    public EliminateRegens (TempVarGen varGen)  {
        super (null);
        this.varGen = varGen;
    }
    protected List<Statement> globalDecls;
    @Override
     public Object visitFunction(Function func)
    {
        globalDecls = new ArrayList<Statement>();
        Function f = (Function) super.visitFunction(func);
        if(globalDecls.size()>0){
            globalDecls.add(f.getBody());
            return f.creator().body(new StmtBlock(globalDecls)).create();
        }
        return f;        
    }
    
    public Object visitStmtLoop(StmtLoop pl){
        List<Statement> tmp = globalDecls;
        globalDecls = new ArrayList<Statement>();
        StmtLoop nl = (StmtLoop) super.visitStmtLoop(pl);
        if(globalDecls.size()>0){
            globalDecls.add(nl.getBody());
            nl = new StmtLoop(nl, nl.getIter(), new StmtBlock(globalDecls));
        }
        globalDecls = tmp;
        return nl;
    }
    
    
    
    public Object visitExprRegen (ExprRegen er) {
        List<Expression> exps = explodeRegen (er);
        if (exps.size () == 1)
            return exps.get (0);

        ExprVar which = makeNDChoice (exps.size (), er, "_whichexpr");
        return toConditional (which, exps, 0);
    }

    private Expression toConditional (ExprVar which, List<Expression> exps, int i) {
        if ((i+1) == exps.size ())
            return exps.get (i);
        else {
            Expression cond = new ExprBinary (which, "==", ExprConstant.createConstant (which, ""+i));
            return new ExprTernary ("?:", cond, exps.get (i), toConditional (which, exps, i+1));
        }
    }

    public Object visitStmtAssign (StmtAssign sa) {
        if (sa.getLHS () instanceof ExprRegen)
            return translateRegenAssn ((ExprRegen) sa.getLHS (),
                    sa.getRHS ()/*(Expression) sa.getRHS ().accept (this)*/);
        else
            return super.visitStmtAssign (sa);
    }
    
    
    

    
    StmtIfThen recITC(ExprVar whichLhs, ExprRegen lhs, Expression rhs, List<Expression> lhses, int i){
        StmtIfThen tmp = null;
        if( (i +1 )< lhses.size()){
            tmp = recITC(whichLhs, lhs,rhs, lhses, i+1);
        }
        Expression l = lhses.get (i);
        Expression cond = new ExprBinary (whichLhs, "==",
                ExprConstant.createConstant (whichLhs, ""+ i));

        List<Statement> oldStmts = newStatements;
        newStatements = new ArrayList<Statement> ();

        newStatements.add (new StmtAssign (l, rhs));
        StmtIfThen s = new StmtIfThen (lhs, cond, new StmtBlock (lhs, newStatements), tmp);

        newStatements = oldStmts;
        return s;
    }


    public Object translateRegenAssn (ExprRegen lhs, Expression rhs) {
        List<Expression> lhses = explodeRegen (lhs);
        if (lhses.size () == 1)
            return new StmtAssign (lhses.get (0), (Expression) rhs.accept (this));

        // General idea:
        //   1.) Build set of possible LHSes
        //   2.) [OPTIONAL] Minimize possible LHSes
        //   3.) [OPTIONAL] Save the RHS to a temporary variable; dangerous
        //       because it might introduce new program behaviors
        //   4.) Make a "switch" statement to choose to which LHS to assign
        ExprVar whichLhs = makeNDChoice (lhses.size (), lhs, "_whichlhs");

        String tmpvname = varGen.nextVar("_tmprhs");
        addStatement( new StmtVarDecl( rhs, getTypeReal(lhses.get(0)), tmpvname , (Expression) rhs.accept (this) ));
        Expression newRHS = new ExprVar(rhs, tmpvname);
        
        for (int i = 0; i < lhses.size (); ++i) {
            Expression l = lhses.get (i);
            Expression cond = new ExprBinary (whichLhs, "==",
                    ExprConstant.createConstant (whichLhs, ""+ i));

            List<Statement> oldStmts = newStatements;
            newStatements = new ArrayList<Statement> ();

            newStatements.add (new StmtAssign (l, newRHS));
            StmtIfThen s = new StmtIfThen (lhs, cond, new StmtBlock (lhs, newStatements), null);

            newStatements = oldStmts;

            addStatement (s);
        }

        return null;
    }

    protected ExprVar makeNDChoice (int n, FENode cx, String pfx) {
        ExprVar which = new ExprVar (cx, varGen.nextVar (pfx));

        globalDecls.add(new StmtVarDecl (cx, TypePrimitive.inttype, which.getName (), null));
        ExprHole es = new ExprHole (which, Misc.nBitsBinaryRepr (n));
        globalDecls.add(new StmtAssign (which, es));
        globalDecls.add(new StmtAssert (cx,
            new ExprBinary (
                new ExprBinary (ExprConstInt.zero, "<=", which),
                "&&",
                new ExprBinary (which, "<", ExprConstant.createConstant (which, ""+ n))), "regen " + es.getHoleName(), StmtAssert.UBER));

        return which;
    }

    protected List<Expression> explodeRegen (ExprRegen r) {
        // Steps:
        //   1.) Find all paths through regen, keeping ExprParens
        //   2.) Fix up associativity problems
        //   3.) Remove ExprParens
        List<Expression> exps = (List<Expression>) r.accept (new DoExprGen ());
        for (int i = 0; i < exps.size (); ++i) {
            Expression e = exps.get (i);
            e = (Expression) e.accept (new FixOpPrec ());
            e = (Expression) e.accept (new StripExprParens ());
            exps.set (i, e);
        }
        return exps;
    }

    @SuppressWarnings("unchecked")
    private class DoExprGen extends FEReplacer {
        public Object visitExprRegen (ExprRegen er) {
            return er.getExpr ().accept (this);
        }

        public Object visitExprAlt (ExprAlt ea) {
            List<Expression> e = (List<Expression>) ea.getThis ().accept (this);
            e.addAll ((List<Expression>) ea.getThat ().accept (this));
            return e;
        }

        @Override
        public Object visitExprArrayRange(ExprArrayRange ear){
            List<Expression> exps = new ArrayList<Expression> ();
            List<Expression> bases = (List<Expression>) ear.getBase().accept(this);
            List<Expression> offsets = (List<Expression>) ear.getOffset().accept(this);
            for(Expression base: bases)
                for(Expression offset: offsets){
                    exps.add(new ExprArrayRange(base, offset));
                }
            return exps;
        }
        
        private void fcconstruct(ExprFunCall efc, List<Expression> plist, int lidx, List<Expression> actuals, List<Expression> exprs){
            if(plist.size() > lidx){
                Expression cur = plist.get(lidx);
                List<Expression> tmp = (List<Expression>)cur.accept(this);
                for(Expression e: tmp){
                    List<Expression> nl = new ArrayList<Expression>(actuals);
                    nl.add(e);
                    fcconstruct(efc, plist, lidx+1, nl, exprs);
                }
            }else{
                exprs.add(new ExprFunCall(efc, efc.getName(), actuals, doCallTypeParams(efc)));
            }
        }
        
        private void structConstruct(ExprNew exp, List<ExprNamedParam> plist, int lidx,
                List<ExprNamedParam> actuals, List<Expression> exprs)
        {
            if (plist.size() > lidx) {
                ExprNamedParam cur = plist.get(lidx);
                List<Expression> tmp = (List<Expression>) cur.getExpr().accept(this);
                for (Expression e : tmp) {
                    List<ExprNamedParam> nl = new ArrayList<ExprNamedParam>(actuals);
                    nl.add(new ExprNamedParam(cur.getContext(), cur.getName(), e));
                    structConstruct(exp, plist, lidx + 1, nl, exprs);
                }
            } else {
                exprs.add(new ExprNew(exp, exp.getTypeToConstruct(), actuals,
                        exp.isHole()));
            }
        }

        @Override
        public Object visitExprFunCall(ExprFunCall efc){
            List<Expression> plist = efc.getParams();
            List<Expression> exprs = new ArrayList<Expression>();
            fcconstruct(efc, plist,0, new ArrayList<Expression>(), exprs);          
            return exprs;
        }
        
        @Override
        public Object visitExprNew(ExprNew exp) {
            List<ExprNamedParam> plist = exp.getParams();
            List<Expression> exprs = new ArrayList<Expression>();
            structConstruct(exp, plist, 0, new ArrayList<ExprNamedParam>(), exprs);
            return exprs;
        }

        public Object visitExprChoiceBinary (ExprChoiceBinary ecb) {
            List<Expression> lefts = (List<Expression>) ecb.getLeft ().accept (this);
            List<Expression> rights = (List<Expression>) ecb.getRight ().accept (this);

            List<Expression> exps = new ArrayList<Expression> ();
            for (Expression left : lefts)
                for (Expression right : rights)
                    for (int op : ecb.opsAsExprBinaryOps ())
                        exps.add (new ExprBinary (op, left, right));
            return exps;
        }

        public Object visitExprChoiceUnary (ExprChoiceUnary ecu) {
            List<Expression> exprs = (List<Expression>) ecu.getExpr ().accept (this);

            List<Expression> exps = new ArrayList<Expression> ();
            for (Expression expr : exprs) {
                if (ecu.opOptional ())
                    exps.add (expr);
                for (int op : ecu.opsAsExprUnaryOps ())
                    exps.add (new ExprUnary (expr, op, expr));
            }
            return exps;
        }

        public Object visitExprChoiceSelect (ExprChoiceSelect ecs) {
            Object o = ecs.accept (new SelectorVisitor () {
                public Object visit (SelectField sf) {
                    List<String> f = new ArrayList<String> ();
                    Set<List<String>> s = new HashSet<List<String>> ();
                    f.add (sf.getField ());
                    s.add (f);
                    return s;
                }

                public Object visit (SelectOrr so) {
                    Set<List<String>> s1 = (Set<List<String>>) so.getThis ().accept (this);
                    Set<List<String>> s2 = (Set<List<String>>) so.getThat ().accept (this);
                    s1.addAll (s2);
                    if (so.getThis ().isOptional () && so.getThat ().isOptional ())
                        s1.add (new ArrayList<String> ());
                    return s1;
                }

                public Object visit (SelectChain sc) {
                    Set<List<String>> sf = (Set<List<String>>) sc.getFirst ().accept (this);
                    Set<List<String>> sn = (Set<List<String>>) sc.getNext ().accept (this);
                    Set<List<String>> rets = new HashSet<List<String>> ();
                    boolean firstOpt = sc.getFirst ().isOptional ();
                    boolean nextOpt = sc.getNext ().isOptional ();
                    for (List<String> f : sf) {
                        if (firstOpt)
                            rets.add (f);
                        for (List<String> n : sn) {
                            List<String> fn = new ArrayList<String> (f);
                            fn.addAll (n);
                            rets.add (fn);
                        }
                    }
                    for (List<String> n : sn)
                        if (nextOpt)
                            rets.add (n);
                    if (firstOpt && nextOpt)
                        rets.add (new ArrayList<String> ());
                    return rets;
                }
            });
            Set<List<String>> sf = (Set<List<String>>) o;

            List<Expression> objs = (List<Expression>) ecs.getObj ().accept (this);
            List<Expression> exps = new ArrayList<Expression> ();
            boolean fieldOpt = ecs.getField ().isOptional ();

            for (Expression obj : objs) {
                if (fieldOpt)
                    exps.add (obj);
                for (List<String> s : sf) {
                    Expression e = obj;
                    for (String f : s) {
                        e = new ExprField(e, f, f.equals(""));
                    }
                    exps.add (e);
                }
            }

            return exps;
        }

        public Object visitExprField(ExprField exp) {
            List<Expression> exps = new ArrayList<Expression>();
            exps.add(exp);
            return exps;
        }

        public Object visitExprConstInt (ExprConstInt exp) {
            List<Expression> exps = new ArrayList<Expression> ();
            exps.add (exp);
            return exps;
        }

        public Object visitExprConstFloat(ExprConstFloat exp) {
            List<Expression> exps = new ArrayList<Expression>();
            exps.add(exp);
            return exps;
        }

        public Object visitExprNullPtr (ExprNullPtr exp) {
            List<Expression> exps = new ArrayList<Expression> ();
            exps.add (exp);
            return exps;
        }
        
        public Object visitSpmdPid(SpmdPid exp) {
            List<Expression> exps = new ArrayList<Expression>();
            exps.add(exp);
            return exps;
        }

        public Object visitSpmdNProc(SpmdNProc exp) {
            List<Expression> exps = new ArrayList<Expression>();
            exps.add(exp);
            return exps;
        }

        public Object visitExprParen (ExprParen ep) {
            List<Expression> exps = (List<Expression>) ep.getExpr ().accept (this);
            for (int i = 0; i < exps.size (); ++i)
                exps.set (i, new ExprParen (ep, exps.get (i)));
            return exps;
        }

        public Object visitExprTernary (ExprTernary et) {
            List<Expression> as = (List<Expression>) et.getA ().accept (this);
            List<Expression> bs = (List<Expression>) et.getB ().accept (this);
            List<Expression> cs = (List<Expression>) et.getC ().accept (this);

            List<Expression> exps = new ArrayList<Expression> ();
            for (Expression a : as)
                for (Expression b : bs)
                    for (Expression c : cs)
                        exps.add (new ExprTernary ("?:", a, b, c));
            return exps;
        }

        public Object visitExprStar (ExprHole exp) {
            List<Expression> exps = new ArrayList<Expression> ();
            exps.add(exp);
            return exps;
        }
        
        /**
         * Visit method for local variables expressions that does the same logic
         * as the visit method for a star.
         */
		public Object visitExprLocalVariables(ExprLocalVariables expr) {
			List<Expression> exprs = new ArrayList<Expression>();
			exprs.add(expr);
			return exprs;
		}

        public Object visitExprVar (ExprVar exp) {
            List<Expression> exps = new ArrayList<Expression> ();
            exps.add (exp);
            return exps;
        }

		public Object visitExprConstChar(ExprConstChar exprConstChar) {
			List<Expression> exps = new ArrayList<Expression>();
			exps.add(exprConstChar);
			return exps;
		}
    }

    /**
     * Assumes that:
     *  - the parser has taken care of prefix/postfix operator
     *      precedence/associativity, and that
     *  - all binary operators are left associative.
     */
    private class FixOpPrec extends FEReplacer {
        class opinfo {
            int op, prec;
            opinfo (int op, int prec) { this.op = op; this.prec = prec; }
        }

        public Object visitExprBinary (ExprBinary eb) {
            // Idea:
            //  1.) Do an inorder traversal of child ExprBinaries to pick up
            //      full expr, visiting "leaf" nodes along the way
            //  2.) Rebalance the tree
            Queue<opinfo> opQ = new LinkedList<opinfo> ();
            Queue<Expression> opndQ = new LinkedList<Expression> ();
            flatten (opQ, opndQ, eb);

            Stack<opinfo> ops = new Stack<opinfo> ();
            Stack<Expression> opnds = new Stack<Expression> ();
            opQ.add (new opinfo (-1, -1));  // "flush" operator
            while (!opndQ.isEmpty ()) {
                opnds.push (opndQ.remove ());
                opinfo oi = opQ.remove ();
                while (!ops.isEmpty () && oi.prec <= ops.peek ().prec) {
                    Expression r = opnds.pop ();
                    Expression l = opnds.pop ();
                    opnds.push (new ExprBinary (ops.pop ().op, l, r));
                }
                ops.push (oi);
            }
            assert opnds.size () == 1;
            return opnds.pop ();
        }

        void flatten (Queue<opinfo> opQ, Queue<Expression> opndQ, Expression e) {
            if (!(e instanceof ExprBinary))
                opndQ.add ((Expression) e.accept (this));
            else {
                ExprBinary eb = (ExprBinary) e;
                flatten (opQ, opndQ, eb.getLeft ());
                opQ.add (new opinfo (eb.getOp (), eb.getOpPrec ()));
                flatten (opQ, opndQ, eb.getRight ());
            }
        }
    }


    private class StripExprParens extends FEReplacer {
        public Object visitExprParen (ExprParen ep) {
            return ep.getExpr ().accept (this);
        }
    }
}
