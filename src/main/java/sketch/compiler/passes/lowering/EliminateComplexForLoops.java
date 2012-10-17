package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.stmts.StmtWhile;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.spmd.SpmdTransform;
import sketch.compiler.passes.structure.ASTQuery;

@CompilerPassDeps(runsBefore = { SpmdTransform.class }, runsAfter = {})
public class EliminateComplexForLoops extends FEReplacer {
	TempVarGen vgen;
    private Map<String, Function> lookupFunc;
	
	public EliminateComplexForLoops(TempVarGen vgen){
		this.vgen = vgen;
	}
	
	static class UsedVar extends FEReplacer {
	    Set<String> result = new HashSet<String>();
        StringBuilder baseName = new StringBuilder();
	    @Override
        public Object visitExprVar(ExprVar v) {
            String name = v.getName();
            result.add(name);
            baseName.setLength(0);
            baseName.append(name);
            return v;
        }

        @Override
        public Object visitExprField(ExprField ef) {
            ef.getLeft().accept(this);
            baseName.append(".").append(ef.getName());
            result.add(baseName.toString());
            return ef;
        }
	}
	
    class VarChanged extends ASTQuery {
        Set<String> vs;
        boolean isAssignee;
        StringBuilder baseName = new StringBuilder();

        VarChanged(Set<String> s) {
            vs = s;
        }

        @Override
        public Object visitExprVar(ExprVar v) {
            if (!result) {
                String name = v.getName();
                if (isAssignee && vs.contains(name)) {
                    result = true;
                } else {
                    baseName.setLength(0);
                    baseName.append(name);
                }
            }

            return v;
        }

        @Override
        public Object visitExprField(ExprField ef) {
            if (!result) {
                boolean oldIsA = isAssignee;
                isAssignee = false;
                ef.getLeft().accept(this);
                if (result) {
                    return ef;
                }
                baseName.append(".").append(ef.getName());
                if (oldIsA) {
                    isAssignee = oldIsA;
                    if (vs.contains(baseName.toString())) {
                        result = true;
                    }
                }
            }
            return ef;
        }

        @Override
        public Object visitExprFunCall(ExprFunCall e) {
            if (result) {
                return e;
            }
            List<Expression> p = e.getParams();
            if (p.isEmpty()) {
                return e;
            }
            Function f = lookupFunc.get(e.getName());
            boolean oldIsA = isAssignee;
            isAssignee = true;
            if (f != null) {
                for (int i=0; i<p.size(); ++i) {
                    if (f.getParams().get(i).isParameterOutput()) {
                        p.get(i).accept(this);
                        if (result) {
                            return e;
                        }
                    }
                }
            }
            isAssignee = oldIsA;
            return e;
        }

        @Override
        public Object visitStmtAssign(StmtAssign s) {
            if (result) {
                return s;
            }
            assert !isAssignee : "StmtAssign cannot be in assignee position!";
            isAssignee = true;
            s.getLHS().accept(this);
            if (result) {
                return s;
            }
            isAssignee = false;
            s.getRHS().accept(this);
            return s;
        }

        @Override
        public Object visitExprArrayRange(ExprArrayRange e) {
            if (result) {
                return e;
            }
            e.getBase().accept(this);
            if (result) {
                return e;
            }
            boolean oldIsA = isAssignee;
            isAssignee = false;
            RangeLen rl = e.getSelection();
            rl.start().accept(this);
            if (!result && rl.hasLen()) {
                rl.getLenExpression().accept(this);
            }
            isAssignee = oldIsA;
            return e;
        }
    }

    private boolean isComplexFor(StmtFor sf) {
        Statement init = sf.getInit();
        Statement incr = sf.getIncr();
        Expression cond = sf.getCond();
        if (init == null || incr == null || cond == null) {
            return true;
        }
        
        String var = null;
        if (init instanceof StmtVarDecl) {
            StmtVarDecl d = (StmtVarDecl) init;
            if (d.getNumVars() != 1) {
                return true;
            }
            var = d.getName(0);
            if (d.getInit(0) == null) {
                return true;
            }
        } else if (init instanceof StmtAssign) {
            StmtAssign a = (StmtAssign) init;
            if (a.getLHS() instanceof ExprVar) {
                var = ((ExprVar) a.getLHS()).getName();
            } else {
                return true;
            }
        } else {
            return true;
        }
        
        if (incr instanceof StmtAssign) {
            StmtAssign a = (StmtAssign) incr;
            if (a.getLHS() instanceof ExprVar && ((ExprVar)a.getLHS()).getName() == var) {
                // ok
            } else {
                return true;
            }
            if (a.getRHS() instanceof ExprBinary && ((ExprBinary)a.getRHS()).getOp() == ExprBinary.BINOP_ADD) {
                ExprBinary b = (ExprBinary) a.getRHS();
                Expression v = b.getLeft();
                Expression cint;
                if (v instanceof ExprConstInt) {
                    cint = v;
                    v = b.getRight();
                } else {
                    cint = b.getRight();
                    if (!(cint instanceof ExprConstInt)) {
                        return true;
                    }
                }
                // now cint instanceof ExprConstInt
                if (! (cint.getIValue() == 1 && v instanceof ExprVar && ((ExprVar)v).getName()==var) ) {
                    return true;
                }
            } else {
                return true;
            }
        } else {
            return true;
        }
        
        if (cond instanceof ExprBinary) {
            ExprBinary bcond = (ExprBinary) cond;
            if (bcond.getOp() == ExprBinary.BINOP_LT || bcond.getOp() == ExprBinary.BINOP_LE) {
                // ok
            } else {
                return true;
            }
            if (bcond.getLeft() instanceof ExprVar && ((ExprVar)bcond.getLeft()).getName() == var) {
                UsedVar u = new UsedVar();
                bcond.getRight().accept(u);
                if (u.result.contains(var)) {
                    return true;
                }
                u.result.add(var);
                return new VarChanged(u.result).run(sf.getBody());
            } else {
                return true;
            }
        }

        return false;
	}
	
	@Override
	public Object visitStmtFor(StmtFor sf){
        if (isComplexFor(sf)) {
            String nm = vgen.nextVar();
			Expression tmpvar = new ExprVar(sf.getCond(), nm);
			List<Statement> bl = new ArrayList<Statement>();
            List<Statement> lbody = new ArrayList<Statement>();

            lbody.add((Statement) sf.getBody().accept(this));
            lbody.add(sf.getIncr());

            lbody.add(new StmtAssign(sf.getCond(), tmpvar, sf.getCond()));
            StmtBlock sb = new StmtBlock(sf, lbody);

            bl.add(sf.getInit());
            bl.add(new StmtVarDecl(sf.getCond(), TypePrimitive.bittype, nm, null));
            bl.add(new StmtAssign(new ExprVar(sf.getCond(), nm), sf.getCond()));
    			bl.add(new StmtWhile(sf, tmpvar, sb ));
			return new StmtBlock(sf, bl);
		}else{
			return super.visitStmtFor(sf);
		}
	}
    @Override
    public Object visitStreamSpec(Package p) {
        List<Function> funcs = p.getFuncs();
        int n = funcs.size();
        lookupFunc = new HashMap<String, Function>(n);
        for (Function f : funcs) {
            lookupFunc.put(f.getName(), f);
        }
        return super.visitStreamSpec(p);
    }
}
