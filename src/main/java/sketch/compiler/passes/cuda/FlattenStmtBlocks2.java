/**
 * 
 */
package sketch.compiler.passes.cuda;

import sketch.compiler.dataflow.preprocessor.FlattenStmtBlocks;
import sketch.compiler.passes.annotations.CompilerPassDeps;

@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class FlattenStmtBlocks2 extends FlattenStmtBlocks { }