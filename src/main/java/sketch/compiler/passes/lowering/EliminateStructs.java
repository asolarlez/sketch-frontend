/**
 *
 */
package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.parallelEncoder.VarSetReplacer;

/**
 * Does four things: (1) Replaces 'new [struct]()' expressions with pointers into [struct]
 * arrays (2) Converts variables of type [struct] into ints (3) Replaces field accesses
 * with indexes into field arrays (4) Replaces null with the constant -1. For example:
 * <code>
 *   struct Foo { int bar; }
 *   ...
 *   Foo f1 = new Foo ();
 *   Foo f2 = f1;
 *   return f2.bar;
 * </code> Is converted into: <code>
 *   int[NUM_FOO] Foo_bar = 0;
 *   int   Foo_nextInstance = 0;
 *   ...
 *   assert Foo_nextInstance < NUM_FOO;
 *   int f1 = Foo_nextInstance++;
 *   int f2 = f1;
 *   return Foo_bar[f2];
 * 
 * Preconditions to doing this rewrite:
 *   (1) all recursion has been eliminated
 *   (2) type checking has already been done
 * 
 * This class assumes that all nested constructor calls have been factored, so 
 * new A( x= new B()) 
 * has been turned into
 * tmp = new B();
 * new A(x=tmp);
 * 
 * @author Chris Jones, Armando
 */
public class EliminateStructs extends SymbolTableVisitor {
    private Map<String, StructTracker> structs;
    private TempVarGen varGen;
    Expression maxArrSize;

    // private final String heapSzVar = "_hsz";
    public EliminateStructs(TempVarGen varGen_, Expression maxArrSize) {
		super(null);
        structs = new HashMap<String, StructTracker>();
		varGen = varGen_;
        this.maxArrSize = maxArrSize;
	}

	@Override
	public Object visitParameter(Parameter par){
		Type t = (Type) par.getType().accept(this);
    	if( t == par.getType()){
    		return par;
    	}else{
    		return new Parameter(t, par.getName(), par.getPtype() );
    	}
	}

	/**
	 * 
	 * Null is replaced with the integer -1. Null pointer accesses will therefore lead to out of bounds array access.
	 */
	@Override
	public Object visitExprNullPtr(ExprNullPtr nptr){ return nptr; }
	
	
	/**
	 * Add variable declarations to the body of 'func', and rewrite its body.
	 */
	public Object visitFunction (Function func) {
		boolean isMain = false;
        if (mainFunctions.contains(nres.getFunName(func.getName()))) {
			isMain = true;
	        SymbolTable oldSymTab = symtab;
	        symtab = new SymbolTable(symtab);

	        List<Parameter> newParams = new ArrayList<Parameter>();
	        for (Iterator iter = func.getParams().iterator(); iter.hasNext(); ) {
	            Parameter param = (Parameter)iter.next();
	            symtab.registerVar(param.getName(),
	                               actualType(param.getType()),
	                               param,
	                               SymbolTable.KIND_FUNC_PARAM);
	            newParams.add ((Parameter) param.accept (this));
	        }

	        List<Statement> newBodyStmts = new LinkedList<Statement> ();
            // ExprStar es = new ExprStar(func, 2);
            // es.setType(TypePrimitive.inttype);
            // newBodyStmts.add(new StmtVarDecl(func, TypePrimitive.inttype, heapSzVar,
            // new ExprBinary(new ExprConstInt(HSIZE), "+", es)));
            for (String name : nres.structNamesList()) {
				StructTracker tracker =
                        new StructTracker(nres.getStruct(name), func, varGen, maxArrSize);
				tracker.registerVariables (symtab);
				newBodyStmts.add (tracker.getVarDecls ());
				structs.put (name, tracker);
			}

	        Function func2 = (Function) super.visitFunction(func);
	        symtab = oldSymTab;

	        if(func.isUninterp()){
	        	return func2;
	        }

	        Statement oldBody = func2.getBody ();
	        newBodyStmts.add (oldBody);
	        StmtBlock newBody = new StmtBlock (oldBody, newBodyStmts);

	        newFuncs.add(func2.creator().params(newParams).body(newBody).create());
		}


        if (calledFunctions.contains(nres.getFunName(func.getName()))) {
			SymbolTable oldSymTab = symtab;
	        symtab = new SymbolTable(symtab);
	        List<Parameter> newParams = new ArrayList<Parameter>();
	        for (Iterator iter = func.getParams().iterator(); iter.hasNext(); ) {
	            Parameter param = (Parameter)iter.next();
	            symtab.registerVar(param.getName(),
	                               actualType(param.getType()),
	                               param,
	                               SymbolTable.KIND_FUNC_PARAM);
	            newParams.add ((Parameter) param.accept (this));
	        }

	        List<Statement> newBodyStmts = new LinkedList<Statement> ();
            for (String name : nres.structNamesList()) {
				StructTracker tracker =
                        new StructTracker(nres.getStruct(name), func, varGen, maxArrSize);
				tracker.registerAsParameters(symtab);
				tracker.addParams(newParams);
				structs.put (name, tracker);
			}

	        Function func2 = (Function) super.visitFunction(func);
	        symtab = oldSymTab;

	        if(func.isUninterp()){
	        	return func2;
	        }

	        Statement oldBody = func2.getBody ();
	        newBodyStmts.add (oldBody);
	        StmtBlock newBody = new StmtBlock (oldBody, newBodyStmts);

	        String newName = func.getName();

	        if(isMain){
	        	newName = newName + "_2";
	        }

            newFuncs.add(func2.creator().name(newName).params(newParams).body(newBody).create());
        }
		return null;
	}

	@Override
	public Object visitExprFunCall(ExprFunCall fc){

        String newName = nres.getFunName(fc.getName());

        Function fun = nres.getFun(newName);
		if( fun.isUninterp() ){
			return super.visitExprFunCall(fc);
		}

		List<Expression> newplist = new ArrayList<Expression>(fc.getParams());

        for (String name : nres.structNamesList()) {
            structs.get(name).addActualParams(newplist);
		}

        if (mainFunctions.contains(newName)) {
            String tmp = fun.getName() + "_2";
            newName = newName.replace(fun.getName(), tmp);
		}

		return new ExprFunCall(fc,newName, newplist);
	}






	/**
	 * Rewrite field accesses of the form '((Foo)foo).bar' into 'Foo_bar[foo]'.
	 */
	public Object visitExprField (ExprField ef) {

        Type t = this.actualType(getType(ef.getLeft()));
        if (!t.isStruct()) {
			ef.report ("Trying to read field of non-struct variable.");
			throw new RuntimeException ("reading field of non-struct");
		}

        StructTracker struct =
                structs.get(nres.getStructName(((TypeStruct) t).getName()));
		String field = ef.getName ();

        return struct.getFieldAccess(ef, field, (Expression) ef.getLeft().accept(this));

	}

	/**
	 * Rewrite the 'new' expression into a guarded pointer increment.
	 */
    public Object visitExprNew (ExprNew expNew){
    	expNew.assertTrue (expNew.getTypeToConstruct().isStruct(),
    			"Sorry, only structs are supported in 'new' statements.");

        String name = ((TypeStructRef) expNew.getTypeToConstruct()).getName();
        StructTracker struct = structs.get(nres.getStructName(name));        
        List<Expression> rhs = new ArrayList<Expression>();
        Map<String, Expression> fieldExprs = new HashMap<String, Expression>();
        for (ExprNamedParam en : expNew.getParams()) {
            Expression tt = (Expression) en.getExpr().accept(this);
            fieldExprs.put(en.getName(), tt);
            Expression lhs = new ExprVar(expNew, varGen.nextVar());
            addStatement(new StmtVarDecl(expNew, getType(tt), lhs.toString(), tt));
            rhs.add(lhs);
        }

        VarSetReplacer vsr = new VarSetReplacer(fieldExprs);
        int i = 0;
        for (ExprNamedParam en : expNew.getParams()) {
            Expression lhs =
                    struct.getLHSFieldAccess(expNew, en.getName(),
                            struct.nextInstancePointer, vsr);
            addStatement(new StmtAssign(lhs, rhs.get(i)));
            ++i;
        }
        // this.addStatement (struct.makeAllocationGuard (expNew));
    	return struct.makeAllocation (expNew);
    }

    public Object visitTypeStruct (TypeStruct ts) {
    	// return TypePrimitive.inttype;
    	return ts;
    }

    @Override
    public Object visitTypeStructRef (TypeStructRef t) {
    	// return  TypePrimitive.inttype ;
    	return t;
    }



    final Set<String> calledFunctions = new HashSet<String>();
    final Set<String> mainFunctions = new HashSet<String>();


    public Object visitProgram(Program p) {
        calledFunctions.clear();
        nres = new NameResolver(p);
        final NameResolver lnres = nres;
        for (StreamSpec pkg : p.getStreams()) {
            nres.setPackage(pkg);
            for (Function func : pkg.getFuncs()) {
                if (func.getSpecification() != null) {
                    mainFunctions.add(nres.getFunName(func.getName()));
                    mainFunctions.add(nres.getFunName(func.getSpecification()));
                }

                func.accept(new FEReplacer() {
                    @Override
                    public Object visitExprFunCall(ExprFunCall exp) {
                        calledFunctions.add(lnres.getFunName(exp.getName()));
                        return exp;
                    }
                });
            }
        }
        return super.visitProgram(p);
    }


    private class MakeArrFieldsConst extends FEReplacer {

        Expression maxArrSz;

        public MakeArrFieldsConst(Expression maxArrSz) {
            this.maxArrSz = maxArrSz;
        }

        @Override
        public Object visitTypeArray(TypeArray ta) {
            Integer ilen = ta.getLength().getIValue();
            if (ilen != null) {
                return super.visitTypeArray(ta);
            } else {
                return new TypeArray((Type) ta.getBase().accept(this), maxArrSz);
            }
        }
    }

    /**
	 * Tracks variables used by structs when we eliminate 'new' expressions
	 * and field accesses.  Also provides convenience expression generators.
	 *
	 * @author Chris Jones
	 */
	private class StructTracker {

		private TypeStruct struct;
		private TypeStructRef sref;
		private FENode cx;
        // private final ExprVar heapSzVar;
        private final ExprVar nextInstancePointer;
        private final Map<String, ExprVar> fieldArrays;

        private class FieldInfo {
            final boolean isArray;
            final Expression maxLen;
            final Expression realLen;

            FieldInfo(boolean isArray, Expression maxLen, Expression realLen) {
                this.isArray = isArray;
                this.maxLen = maxLen;
                this.realLen = realLen;
            }
        }

        Expression getFieldMaxLen(String field) {
            return fieldInfo.get(field).maxLen;
        }

        Expression getFieldRealLen(String field) {
            return fieldInfo.get(field).realLen;
        }

        boolean isFieldArr(String field) {
            return fieldInfo.get(field).isArray;
        }

        private final Map<String, FieldInfo> fieldInfo;
        MakeArrFieldsConst mafc;

        // private final String heapsize;

        // private final int heapsize;

	    /**
	     * Create a tracker of the variables used to eliminate allocs and
	     * field accesses of [struct_] variables.
	     *
	     * @param struct_	The struct to track
	     * @param cx_		The context at which new variables will be declared
	     * @param varGen	Generator for new variable names
	     */
	    public StructTracker (TypeStruct struct_,
 FENode cx_, TempVarGen varGen,
                final Expression maxArrSz)
        {

            // this.heapsize = heapsize;
	    	struct = struct_;
	    	sref = new TypeStructRef(struct.getName());
	    	cx = cx_;

            // heapSzVar = new ExprVar(cx, heapsize);

	    	nextInstancePointer =
                    new ExprVar(cx, varGen.nextVar("#" + struct.getName() + "_" +
                            "nextInstance_"));

	    	fieldArrays = new HashMap<String, ExprVar> ();
            fieldInfo = new HashMap<String, FieldInfo>();
            mafc = new MakeArrFieldsConst(maxArrSz);
            FEReplacer compMaxes = new FEReplacer() {
                public Object visitExprField(ExprField ev) {
                    return maxArrSz;
                }
                public Object visitExprVar(ExprVar ev) {
                    return maxArrSz;
                }
            };
            for (Entry<String, Type> entry : struct) {
                fieldArrays.put(
                        entry.getKey(),
                        new ExprVar(cx, varGen.nextVar("_" + struct.getName() + "_" +
                                entry.getKey() + "_")));
                if (entry.getValue() instanceof TypeArray) {
                    TypeArray ta = (TypeArray) entry.getValue();
                    Integer ii = ta.getLength().getIValue();
                    if (ii != null) {
                        fieldInfo.put(entry.getKey(), new FieldInfo(true, ta.getLength(),
                                ta.getLength()));
                    } else {
                        fieldInfo.put(
                                entry.getKey(),
                                new FieldInfo(true, (Expression) ta.getLength().accept(
                                        compMaxes), ta.getLength()));
                    }
                } else {
                    fieldInfo.put(entry.getKey(), new FieldInfo(false, null, null));
                }

            }
	    }


	    public void addParams(List<Parameter> newParams){


	    	newParams.add(new Parameter(sref, nextInstancePointer.getName (), Parameter.REF));
            // newParams.add(new Parameter(TypePrimitive.inttype, heapsize));
	    	for (String field : fieldArrays.keySet ()) {
	    		newParams.add(new Parameter(typeofFieldArr (field) , fieldArrays.get (field).getName (), Parameter.REF ));
	    	}
	    }

	    public void addActualParams(List<Expression> params){
	    	params.add(nextInstancePointer);
            // params.add(heapSzVar);
	    	for (String field : fieldArrays.keySet ()) {
	    		params.add(fieldArrays.get(field));
	    	}
	    }

	    public void registerAsParameters(SymbolTable symtab){
	    	String nip = nextInstancePointer.getName ();
	    	symtab.registerVar(nip,
 this.struct,
                    nextInstancePointer,
                    SymbolTable.KIND_FUNC_PARAM);

            // symtab.registerVar(heapsize, TypePrimitive.inttype, heapSzVar,
            // SymbolTable.KIND_FUNC_PARAM);

	    	/*symtab.registerVar(nip + "_out",
                    TypePrimitive.inttype,
                    nextInstancePointer,
                    SymbolTable.KIND_FUNC_PARAM);*/

	    	for (String field : fieldArrays.keySet ()) {
	    		symtab.registerVar (fieldArrays.get (field).getName (),
	    				typeofFieldArr (field), fieldArrays.get(field), SymbolTable.KIND_FUNC_PARAM);

	    		/* symtab.registerVar (fieldArrays.get (field).getName () + "_out",
	    				typeofFieldArr (field), fieldArrays.get(field), SymbolTable.KIND_FUNC_PARAM); */
	    	}




	    }



	    /**
	     * Register the variables used by this struct with the symbol table.
	     *
	     * @param symtab  symbol table with which to register
	     */
	    public void registerVariables (SymbolTable symtab) {
	    	symtab.registerVar (nextInstancePointer.getName (), TypePrimitive.inttype);
            // symtab.registerVar(heapsize, TypePrimitive.inttype);
	    	for (String field : fieldArrays.keySet ()) {
	    		symtab.registerVar (fieldArrays.get (field).getName (),
	    				typeofFieldArr (field));
	    	}
	    }

	    /**
	     * Return the field array variable associated with 'field.'
	     *
	     * @param field
	     * @return
	     */
	    public Expression getFieldArray (String field) {
	    	return fieldArrays.get (field);
	    }

        public Expression getFieldAccess(FENode cx, String field, final Expression basePtr)
        {
            if (isFieldArr(field)) {
                FEReplacer switchFields = new FEReplacer() {
                    Type lastType;
                    public Object visitExprField(ExprField ev) {
                        Expression base = (Expression) ev.getLeft().accept(this);
                        TypeStruct ts = (TypeStruct) lastType;
                        NameResolver lnr = EliminateStructs.this.nres;
                        StructTracker struct =
                                structs.get(lnr.getStructName((ts).getName()));
                        lastType = actualType(ts.getType(ev.getName()));
                        return new ExprArrayRange(ev, struct.getFieldArray(ev.getName()),
                                base);
                    }

                    public Object visitExprVar(ExprVar ev) {
                        String nm = ev.getName();
                        lastType = actualType(struct.getType(nm));
                        return new ExprArrayRange(ev, getFieldArray(nm), basePtr);
                    }
                };
                RangeLen rl =
                        new RangeLen(new ExprBinary(basePtr, "*", getFieldMaxLen(field)),
                                (Expression) getFieldRealLen(field).accept(switchFields));
                return new ExprArrayRange(cx, getFieldArray(field), rl);
            } else {
                return new ExprArrayRange(cx, getFieldArray(field), basePtr);
            }
	    }

        public Expression getLHSFieldAccess(FENode cx, String field,
                final Expression basePtr, VarSetReplacer vsr)
        {
            if (isFieldArr(field)) {
                Expression realLen = (Expression) getFieldRealLen(field).accept(vsr);
                realLen = (Expression) realLen.accept(EliminateStructs.this);
                Expression maxLen = getFieldMaxLen(field);
                EliminateStructs.this.addStatement(new StmtAssert(
                        cx,
                        new ExprBinary(realLen, "<=", maxLen),
                        cx.getCx() +
                                ": You are exceeding the maximum size of an array in a struct. You can grow it with the --bnd-arr-size flag.",
                        false));
                RangeLen rl =
 new RangeLen(new ExprBinary(basePtr, "*", maxLen), realLen);
                return new ExprArrayRange(cx, getFieldArray(field), rl);
            } else {
                return new ExprArrayRange(cx, getFieldArray(field), basePtr);
            }
        }


	    /**
	     * @return 	Declarations for new variables created by eliminating
	     * this struct.
	     */
	    public Statement getVarDecls () {
	    	List<String> names = new LinkedList<String> ();
	    	List<Type> types = new LinkedList<Type> ();
	    	List<Expression> inits = new LinkedList<Expression> ();

	    	// Add the next instance poiner
	    	names.add (nextInstancePointer.getName ());
	    	types.add ( this.sref );
            inits.add(ExprConstant.createConstant(cx, "0"));

	    	for (String field : fieldArrays.keySet ()) {
	    		names.add (fieldArrays.get (field).getName ());
	    		types.add (typeofFieldArr (field));
	    		Type t = struct.getType (field); 
	    		inits.add(initField(cx, t.defaultValue()));	    		
	    	}

	    	return new StmtVarDecl (cx, types, names, inits);
	    }

	    /**
	     * Allocate a new object of this struct type.
	     *
	     * @param cx  the context of the allocation site
	     * @return    an allocation expression
	     */
	    public Expression makeAllocation (FENode cx) {
            String tvar = varGen.nextVar();
            EliminateStructs.this.addStatement((Statement) new StmtVarDecl(cx, struct,
                    tvar, nextInstancePointer).accept(EliminateStructs.this));
            EliminateStructs.this.addStatement(new StmtAssign(nextInstancePointer,
                    new ExprBinary(nextInstancePointer, "+", ExprConstInt.one)));
            return new ExprVar(cx, tvar);
	    }

	    /**
	     * Return an allocation guard for a 'new' statement.  This is intended to
	     * be used in the following way:
	     * <code>
	     *   Foo f = new Foo ();
	     *   // translates to
	     *   assert (Foo_nextPointer + 1) &lt; len (Foo_instances);\
	     *   int f = ++Foo_nextPointer;
	     * </code>
	     *
	     * @param cx  The context of the 'new' statement to guard
	     * @return    An allocation guard
	     */
        // public StmtAssert makeAllocationGuard (FENode cx) {
        // return new StmtAssert(cx, this.getAllocationSafetyCheck(cx), cx.getCx() +
        // ": Heap is too small. Make it bigger with the --bnd-heap-size flag",
        // false);
        // }

	    /**
	     * @return an expression to check for allocation safety.  Essentially,
	     * <code>(Foo_nextPointer + 1) &lt; len (Foo_instances)</code>.
	     */
        // public Expression getAllocationSafetyCheck (FENode cx) {
        // return new ExprBinary (cx, nextInstancePointer, "<", getNumInstsExpr(cx));
        // }

	    /**
	     * Return an Expression containing the number of instances of this
	     * struct.
	     *
	     * @param cx  Context where the Expression will be used.
	     * @return
	     */
        // public Expression getNumInstsExpr (FENode cx) {
        // return new ExprVar(cx, heapsize);
        // }

	    /**
	     * Get the type of the field array for 'field'.
	     *
	     * @param field
	     * @return
	     */
	    private Type typeofFieldArr (String field) {
            Type fieldType = (Type) struct.getType(field); // .accept(mafc);
            if (fieldType instanceof TypeArray) {
                fieldType = ((TypeArray) fieldType).getAbsoluteBase();
            }
	    	return new TypeArray (
	    		//fieldType.isStruct () ? TypePrimitive.inttype : 
	    			fieldType,
                    // getNumInstsExpr (cx)
                    null);
	    }

	    /** Return a  field initializer. */
        private Expression initField (FENode cx, Expression e) {
            return null;
        }
	    	    
	}
}
