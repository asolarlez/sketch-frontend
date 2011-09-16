/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package sketch.compiler.ast.core;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sketch.compiler.Directive;
import sketch.compiler.passes.printers.SimpleCodePrinter;
import sketch.compiler.passes.printers.CodePrinterVisitor;

import static sketch.util.DebugOut.printWarning;

/**
 * An entire StreamIt program.  This includes all of the program's
 * declared streams and structure types.  It consequently has Lists of
 * streams (as {@link sketch.compiler.nodes.StreamSpec} objects) and
 * of structures (as {@link sketch.compiler.nodes.TypeStruct} objects).
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class Program extends FENode
{
    private List<StreamSpec> streams;

    protected final Set<Directive> directives;

    public String toString() {
        String res = "";
        for (StreamSpec pkg : streams) {
            res += pkg.toString();
        }
        return res;
    }

    public static class ProgramCreator {
        private Program base;
        private List<StreamSpec> streams;
        private Set<Directive> directives;

        public ProgramCreator(Program base, List<StreamSpec> streams,
                Set<Directive> directives)
        {
            this.base = base;
            this.streams = streams;
            this.directives = directives;
        }

        public ProgramCreator streams(List<StreamSpec> streams) {
            this.streams = streams;
            return this;
        }



        public ProgramCreator directives(Set<Directive> directives) {
            this.directives = directives;
            return this;
        }

        public Program create() {
            return new Program(this.base, this.streams, this.directives);
        }
    }

    public static Program emptyProgram() {
        List<StreamSpec> streams = new java.util.ArrayList<StreamSpec>();
        Set<Directive> directives = new HashSet<Directive>();
        return new Program(null, streams, directives);
    }

    /** Creates a new StreamIt program, given lists of streams and
     * structures. */
    protected Program(FENode context, List<StreamSpec> streams,
            Set<Directive> directives)
    {
        super(context);
        this.streams = streams;

        this.directives = directives;
    }

    public ProgramCreator creator() {
        return new ProgramCreator(this, streams, directives);
    }

    /** Returns the list of streams declared in this. */
    public List<StreamSpec> getStreams()
    {
        return streams;
    }



    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
//        System.out.println("before " + v.getClass().toString());
//        (new CodePrinterVisitor()).visitProgram(this);
        Object ret = v.visitProgram(this);
//        if (ret instanceof Program) {
//            System.out.println("after " + v.getClass().toString());
//            (new CodePrinterVisitor()).visitProgram((Program)ret);
//        }
        return ret;
    }

    public void debugDump() {
        System.out.println((new SimpleCodePrinter()).visitProgram(this));
    }

    public void debugDump(String message) {
        System.out.println("\n//// " + message);
        debugDump(System.out);
        System.out.println("------------------------------\n");
    }

    public void debugDump(OutputStream out) {
        (new SimpleCodePrinter(out, true)).visitProgram(this);
    }

    public void debugDump(File file) {
        try {
            debugDump(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            printWarning("Dumping to file", file, "failed");
        }
    }

    public Set<Directive> getDirectives() {
        return this.directives;
    }
}
