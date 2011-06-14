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

package sketch.compiler.passes.lowering;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.StreamType;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.util.exceptions.UnrecognizedVariableException;

/**
 * Replace "reference" types with their actual types.  Currently, this
 * replaces <code>sketch.compiler.nodes.TypeStructRef</code> with
 * <code>sketch.compiler.nodes.TypeStruct</code>, where the former
 * is just "the user used a name as a type".  This looks through all
 * of the structure declarations and replaces types in variable and
 * field declarations and parameters with the correct actual structure
 * type.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class NoRefTypes extends FEReplacer
{
    // maps name of structure to TypeStruct
    private HashMap structs;

    private Type remapType(Type type, FENode srcErrInfo)
    {
	if (type instanceof TypeArray) {
	    TypeArray ta = (TypeArray)type;
            Type newBase = remapType(ta.getBase(), srcErrInfo);
	    type = new TypeArray(newBase, ta.getLength());
	}
        if (type instanceof TypeStructRef)
        {
            TypeStructRef tsr = (TypeStructRef)type;
            String name = tsr.getName();
            if (!structs.containsKey(name))
                throw new UnrecognizedVariableException(name, srcErrInfo);
            type = (Type)structs.get(name);
        }
        return type;
    }

    public NoRefTypes()
    {
        structs = new HashMap();
    }
    
    public Object visitProgram(Program prog)
    {
        // Go through the list of structures, and notice them all.
        // We also need to rewrite the structures, in case there are
        // structs that contain structs.
        List newStructs = new java.util.ArrayList();
        for (Iterator iter = prog.getStructs().iterator(); iter.hasNext(); )
        {
            TypeStruct struct = (TypeStruct)iter.next();           
            newStructs.add(struct);
            structs.put(struct.getName(), struct);
        }
        
        for (Iterator iter = newStructs.iterator(); iter.hasNext(); )
        {
            TypeStruct struct = (TypeStruct)iter.next();            
            for (int i = 0; i < struct.getNumFields(); i++)
            {           
            	String name = struct.getField(i);
                Type type = remapType(struct.getType(name), FENode.anonTypeNode(struct));
                struct.setType(name, type);
            }           
        }
        return super.visitProgram(prog.creator().structs(newStructs).create());
    }

    
    public Object visitType(Type t, FENode errSource) {
        return remapType(t, errSource);
    }

    public Object visitStreamSpec(StreamSpec ss)
    {
        // Visit the parameter list, then let FEReplacer do the
        // rest of the work.
        List newParams = new java.util.ArrayList();
        for (Iterator iter = ss.getParams().iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            Type type = remapType(param.getType(), param);
            param = new Parameter(type, param.getName(), param.getPtype());
            newParams.add(param);
        }
        return super.visitStreamSpec(new StreamSpec(ss,
                                                    ss.getType(),
                                                    ss.getStreamType(),
                                                    ss.getName(),
                                                    newParams,
                                                    ss.getVars(),
                                                    ss.getFuncs()));
    }

    public Object visitStreamType(StreamType st)
    {
        return new StreamType(st,
 remapType(st.getIn(), st), remapType(st.getOut(), st),
                remapType(st.getLoop(), st));
    }
}
