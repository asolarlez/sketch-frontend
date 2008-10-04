package streamit.frontend.tosbit;

import java.io.IOException;
import java.io.LineNumberReader;

import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.FENode;

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

	public abstract ExprConstInt popValueForNode(FENode node);

	public abstract void loadFromStream(LineNumberReader in) throws IOException;
}