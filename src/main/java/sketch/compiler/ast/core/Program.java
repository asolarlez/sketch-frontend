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
import static sketch.util.DebugOut.printWarning;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.passes.printers.SimpleCodePrinter;

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
    private List<TypeStruct> structs;

    /** Creates a new StreamIt program, given lists of streams and
     * structures. */
    public Program(FENode context, List<StreamSpec> streams, List<TypeStruct> structs)
    {
        super(context);
        this.streams = streams;
        this.structs = structs;
    }

    /** Returns the list of streams declared in this. */
    public List<StreamSpec> getStreams()
    {
        return streams;
    }

    /** Returns the list of structures declared in this. */
    public List<TypeStruct> getStructs()
    {
        return structs;
    }

    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitProgram(this);
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
}
