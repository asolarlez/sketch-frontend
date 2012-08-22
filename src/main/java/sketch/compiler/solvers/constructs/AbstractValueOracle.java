package sketch.compiler.solvers.constructs;

import java.io.IOException;
import java.io.LineNumberReader;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;

public abstract class AbstractValueOracle {

	protected HoleNameTracker holeNamer;
	
	public AbstractValueOracle(HoleNameTracker holeNamer) {
		this.holeNamer = holeNamer;
	}

	public AbstractValueOracle() {
		super();
	}

	public boolean allowMemoization() {
		return holeNamer.allowMemoization();
	}

	public HoleNameTracker getHoleNamer() {
		return holeNamer;
	}

	public String addBinding(Object node) {
		return holeNamer.getName(node);
	}

	public void initCurrentVals() {
		holeNamer.reset();
	}

    public abstract Expression popValueForNode(FENode node, Type t);

	public abstract void loadFromStream(LineNumberReader in) throws IOException;
}