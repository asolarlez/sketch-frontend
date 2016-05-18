package sketch.compiler.passes.bidirectional;

import sketch.compiler.ast.core.FEIdentityVisitor;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.stmts.Statement;

public class BidirectionalPass extends FEIdentityVisitor {
    BidirectionalAnalysis driver;

    NameResolver nres() {
        return driver.getNres();
    }

    void addFunction(Function f) {
        driver.addFunction(f);
    }

    public SymbolTable symtab() {
        return driver.getSymbolTable();
    }

    public TopDownState tdstate() {
        return driver.tdstate;
    }

    void registerDriver(BidirectionalAnalysis driver) {
        this.driver = driver;
    }
    void addStatement(Statement s) {
        driver.doStatement(s);
    }

}
