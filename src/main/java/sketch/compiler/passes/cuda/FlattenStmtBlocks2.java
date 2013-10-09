/**
 * 
 */
package sketch.compiler.passes.cuda;

import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.FlattenStmtBlocks;

@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class FlattenStmtBlocks2 extends FlattenStmtBlocks { }