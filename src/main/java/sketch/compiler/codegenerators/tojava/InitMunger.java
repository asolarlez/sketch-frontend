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

package sketch.compiler.codegenerators.tojava;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;

/**
 * Base class for visitors that add statements to classes' init functions.
 * This provides functions to locate an init function in a set of
 * functions, and to add statements to the start of a stream object's
 * init function.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
abstract public class InitMunger extends FEReplacer
{
    public static Function findInit(FENode context, List fns)
    {
        for (Iterator iter = fns.iterator(); iter.hasNext(); )
        {
            Function fn = (Function)iter.next();
            if (fn.isInit())
                return fn;
        }

        // No init function; create an empty one.
        return Function.creator(context, null, FcnType.Init).body(new StmtBlock(context)).create();
    }

    /**
     * Finds an init function in fns, or creates one using context.
     * Removes it from fns, and replaces it with an equivalent function
     * with stmts at the start of its body.  Returns fns.
     * @deprecated
     */
    public static List replaceInitWithPrepended(FENode context,
                                                List fns, List stmts)
    {
        Function init = findInit(context, fns);
        fns.remove(init);
        StmtBlock oldBody = (StmtBlock)init.getBody();
        List newStmts = new ArrayList(stmts);
        newStmts.addAll(oldBody.getStmts());
        Statement newBody = new StmtBlock(oldBody, newStmts);
        init = init.creator().body(newBody).create();
        fns.add(init);
        return fns;
    }
}
