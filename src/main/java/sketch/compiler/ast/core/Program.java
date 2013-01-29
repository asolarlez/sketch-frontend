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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import sketch.compiler.Directive;
import sketch.compiler.passes.printers.SimpleCodePrinter;

import static sketch.util.DebugOut.printWarning;

/**
 * An entire Sketch program. This includes all of the program's declared
 * <code>Package</code>s and optionally some directives. It consequently has Lists of
 * packages (as {@link sketch.compiler.ast.core.Package} objects) and some compiler
 * directives.
 * 
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class Program extends FENode
{
    private List<Package> packages;

    protected final Set<Directive> directives;

    public String toString() {
        String res = "";
        for (Package pkg : packages) {
            res += pkg.toString();
        }
        return res;
    }

    public static class ProgramCreator {
        private Program base;
        private List<Package> streams;
        private Set<Directive> directives;

        public ProgramCreator(Program base, List<Package> streams,
                Set<Directive> directives)
        {
            this.base = base;
            this.streams = streams;
            this.directives = directives;
        }

        public ProgramCreator streams(List<Package> streams) {
            this.streams = streams;
            return this;
        }



        public ProgramCreator directives(Set<Directive> directives) {
            this.directives = directives;
            return this;
        }

        public Program create() {
            Map<String, Package> namedStreams = new HashMap<String, Package>();
            for (Package pkg : streams) {
                String name = pkg.getName();
                if (namedStreams.containsKey(name)) {
                    Package prevpkg = namedStreams.get(name);
                    namedStreams.put(name, prevpkg.merge(pkg));
                } else {
                    namedStreams.put(name, pkg);
                }
            }
            streams = new ArrayList<Package>();
            for (Entry<String, Package> ns : namedStreams.entrySet()) {
                streams.add(ns.getValue());
            }
            return new Program(this.base, this.streams, this.directives);
        }
    }

    public static Program emptyProgram() {
        List<Package> streams = new java.util.ArrayList<Package>();
        Set<Directive> directives = new HashSet<Directive>();
        return new Program(null, streams, directives);
    }

    public boolean hasFunctions() {
        for (Package s : packages) {
            if (s.getFuncs().size() > 0) {
                return true;
            }
        }
        return false;
    }

    /** Creates a new StreamIt program, given lists of streams and
     * structures. */
    protected Program(FENode context, List<Package> streams,
            Set<Directive> directives)
    {
        super(context);
        this.packages = streams;

        this.directives = directives;
    }

    public ProgramCreator creator() {
        return new ProgramCreator(this, packages, directives);
    }

    /** Returns the list of streams declared in this. */
    public List<Package> getPackages()
    {
        return packages;
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
