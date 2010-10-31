package sketch.util.cuda;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.Expression;

public class CudaThreadBlockDim {
    public final int x;
    public final int y;
    public final int z;

    public CudaThreadBlockDim(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        check();
    }

    @Override
    public String toString() {
        return String.format("ThreadBlock[%d, %d, %d]", x, y, z);
    }

    public CudaThreadBlockDim(String spec) {
        Pattern re = Pattern.compile("\\d+");
        Matcher m = re.matcher(spec);
        Vector<Integer> values = new Vector<Integer>();
        while (m.find()) {
            values.add(Integer.parseInt(m.group()));
        }
        while (values.size() < 3) {
            values.add(1);
        }
        this.x = values.get(0);
        this.y = values.get(1);
        this.z = values.get(2);
        check();
    }

    private void check() {
        assert x * y * z > 0;
    }

    public int all() {
        return x * y * z;
    }

    public Expression getXFromAll(Expression all) {
        if (y * z > 1) {
            return new ExprBinary(all, "%", new ExprConstInt(x));
        } else {
            return all;
        }
    }

    public Expression getYFromAll(Expression all) {
        if (z > 1) {
            return new ExprBinary(new ExprBinary(all, "/", new ExprConstInt(x)), "%",
                    new ExprConstInt(y));
        } else if (y == 1) {
            return new ExprConstInt(0);
        } else {
            return new ExprBinary(all, "/", new ExprConstInt(x));
        }
    }

    public Expression getZFromAll(Expression all) {
        if (z > 1) {
            return new ExprBinary(all, "/", new ExprConstInt(x * y));
        } else {
            return new ExprConstInt(0);
        }
    }
}
