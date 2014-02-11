package sketch.compiler.codegenerators;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;

public class OutputHoleFunc extends FEReplacer {
    private FileWriter writer;

    public OutputHoleFunc(String fname) {
        try {
            writer = new FileWriter(fname);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public Object visitFunction(Function func) {
        List<Parameter> params = func.getParams();
        if (params.size() != 1) {
            return func;
        }
        Parameter p = params.get(0);
        if (!p.isParameterOutput()) {
            return func;
        }
        Type typ = p.getType();
        if (!(typ instanceof TypePrimitive)) {
            return func;
        }

        String outname = p.getName();

        Statement body = func.getBody();
        if (body.isBlock()) {
            List<Statement> list = ((StmtBlock) body).getStmts();
            if (list.isEmpty()) {
                return func;
            }
            int val = -22;
            for (int i = 0; i < list.size(); i++) {
                Statement stmt = list.get(i);
                if (stmt instanceof StmtReturn) {
                    break;
                }
                if (!(stmt instanceof StmtAssign)) {
                    return func;
                }
                StmtAssign sa = (StmtAssign) stmt;
                if (!(sa.getLHS() instanceof ExprVar && sa.getRHS() instanceof ExprConstInt))
                {
                    return func;
                }

                ExprVar var = (ExprVar) sa.getLHS();
                if (!var.getName().equals(outname)) {
                    return func;
                }
                val = ((ExprConstInt) sa.getRHS()).getVal();
            }
            try {
                writer.write(func.getName() + " " + val + "\n");
                writer.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return func;
    }
}
