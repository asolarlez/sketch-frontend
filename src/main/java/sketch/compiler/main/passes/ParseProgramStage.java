package sketch.compiler.main.passes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sketch.compiler.Directive;
import sketch.compiler.Directive.OptionsDirective;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstChar;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.parser.StreamItParser;
import sketch.util.exceptions.ProgramParseException;

/**
 * Parses a program; visitProgram takes <null> as an argument for now.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ParseProgramStage extends MetaStage {
    public ParseProgramStage(TempVarGen varGen, SketchOptions options) {
        super("parse", "Parse the program from a file", varGen, options);
    }

    public Program parseFiles(List<String> inputFiles) throws java.io.IOException,
            antlr.RecognitionException, antlr.TokenStreamException
    {
        Program prog = Program.emptyProgram();
        boolean useCpp = true;
        List<String> cppDefs = Arrays.asList(options.feOpts.def);
        Set<Directive> pragmas = new HashSet<Directive>();

        for (String inputFile : inputFiles) {
            StreamItParser parser = new StreamItParser(inputFile, useCpp, cppDefs);
            Program pprog = parser.parse();
            if (pprog == null)
                return null;

            List<Package> newStreams = new java.util.ArrayList<Package>();
            List<StructDef> newStructs = new java.util.ArrayList<StructDef>();
            newStreams.addAll(prog.getPackages());
            newStreams.addAll(pprog.getPackages());
            pragmas.addAll(parser.getDirectives());
            prog = prog.creator().streams(newStreams).create();
        }
        return prog.creator().directives(pragmas).create();
    }

    protected void processDirectives(Set<Directive> D) {
        for (Directive d : D)
            if (d instanceof OptionsDirective)
                options.prependArgsAndReparse(((OptionsDirective) d).options(), false);
    }

    @Override
    public Program visitProgramInner(Program prog) {
        try {
            prog = parseFiles(options.argsAsList);
            if (prog == null) {
                throw new ProgramParseException("could not parse program");
            }
            processDirectives(prog.getDirectives());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ExprConstChar.renumber();
        return prog;
    }
}
