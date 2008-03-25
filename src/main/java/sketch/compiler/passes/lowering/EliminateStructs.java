/**
 *
 */
package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import streamit.frontend.nodes.ExprArrayInit;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprConstant;
import streamit.frontend.nodes.ExprField;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprNew;
import streamit.frontend.nodes.ExprNullPtr;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.SymbolTable;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;
import streamit.frontend.nodes.TypeStructRef;

/**
 * Does three things:
 *   (1) Replaces 'new [struct]()' expressions with pointers into
 *   	 [struct] arrays
 *   (2) Converts variables of type [struct] into ints
 *   (3) Replaces field accesses with indexes into field arrays
 *
 * For example:
 * <code>
 *   struct Foo { int bar; }
 *   ...
 *   Foo f1 = new Foo ();
 *   Foo f2 = f1;
 *   return f2.bar;
 * </code>
 *
 * Is converted into:
 * <code>
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
 * @author Chris Jones
 *
 */
public class EliminateStructs extends SymbolTableVisitor {
    private Map<String, StructTracker> structs;
    private TempVarGen varGen;
    private final int heapsize;
	public EliminateStructs (TempVarGen varGen_, int heapsize) {
		super(null);
		this.heapsize = heapsize;
		structs = new HashMap<String, StructTracker> ();
		varGen = varGen_;
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
	 * Add variable declarations to the body of 'func', and rewrite its body.
	 */
	public Object visitFunction (Function func) {
		boolean isMain = false;
		if(mainFunctions.contains(func.getName())){
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
	        for (String name : structsByName.keySet ()) {
				StructTracker tracker =
					new StructTracker (structsByName.get (name), func, varGen, heapsize);
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

	        return new Function (func2, func2.getCls (), func2.getName (),
	        			func2.getReturnType (), newParams,
	        			func2.getSpecification (), newBody);
		}


		if(calledFunctions.contains(func.getName())){
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
	        for (String name : structsByName.keySet ()) {
				StructTracker tracker =
					new StructTracker (structsByName.get (name), func, varGen, heapsize);
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

	        return new Function (func2, func2.getCls (), newName,
	        			func2.getReturnType (), newParams,
	        			func2.getSpecification (), newBody);
        }
		return null;
	}

	@Override
	public Object visitExprFunCall(ExprFunCall fc){

		String newName = fc.getName();

		Function fun = this.sspec.getFuncNamed(newName) ;
		if( fun.isUninterp() ){
			return super.visitExprFunCall(fc);
		}

		List<Expression> newplist = new ArrayList<Expression>(fc.getParams());


		for (String name : structsByName.keySet ()) {
			structs.get(name).addActualParams(newplist);
		}

		if(mainFunctions.contains(newName)){
			newName = newName + "_2";
		}

		return new ExprFunCall(fc,newName, newplist);
	}






	/**
	 * Rewrite field accesses of the form '((Foo)foo).bar' into 'Foo_bar[foo]'.
	 */
	public Object visitExprField (ExprField ef) {
		Type t = getType (ef.getLeft ());
		if (false == t.isStruct ()) {
			ef.report ("Trying to read field of non-struct variable.");
			throw new RuntimeException ("reading field of non-struct");
		}

		StructTracker struct = structs.get (((TypeStruct)t).getName ());
		String field = ef.getName ();

		return new ExprArrayRange (ef, struct.getFieldArray (field),
				(Expression) ef.getLeft ().accept (this));
	}

	/**
	 * Rewrite the 'new' expression into a guarded pointer increment.
	 */
    public Object visitExprNew (ExprNew expNew){
    	expNew.assertTrue (expNew.getTypeToConstruct().isStruct(),
    			"Sorry, only structs are supported in 'new' statements.");

    	StructTracker struct =
    		structs.get (((TypeStructRef) expNew.getTypeToConstruct ()).getName ());

    	this.addStatement (struct.makeAllocationGuard (expNew));
    	return struct.makeAllocation (expNew);
    }

    /** Rewrite variables of type 'struct' into ones of type 'int'. */
    public Object visitTypeStruct (TypeStruct ts) {
    	return TypePrimitive.inttype;
    }

    public Object visitType (Type t) {
    	return (t instanceof TypeStructRef) ? TypePrimitive.inttype : t;
    }



    final Set<String> calledFunctions = new HashSet<String>();
    final Set<String> mainFunctions = new HashSet<String>();


    public Object visitStreamSpec(StreamSpec spec)
    {
    	calledFunctions.clear();
    	for (Iterator<Function> iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
    		Function func = iter.next();
    		if(func.getSpecification() != null){
    			mainFunctions.add(func.getName());
    			mainFunctions.add(func.getSpecification());
    		}

    		func.accept(new FEReplacer(){
    			@Override
    			public Object visitExprFunCall(ExprFunCall exp){
    				calledFunctions.add(exp.getName());
    				return exp;
    			}
    		});
        }
    	return super.visitStreamSpec(spec);
    }






    /**
	 * Tracks variables used by structs when we eliminate 'new' expressions
	 * and field accesses.  Also provides convenience expression generators.
	 *
	 * @author Chris Jones
	 */
	private class StructTracker {
		private TypeStruct struct;
		private FENode cx;
	    private ExprVar nextInstancePointer;
	    private Map<String, ExprVar> fieldArrays;

	    private final int heapsize;

	    /**
	     * Create a tracker of the variables used to eliminate allocs and
	     * field accesses of [struct_] variables.
	     *
	     * @param struct_	The struct to track
	     * @param cx_		The context at which new variables will be declared
	     * @param varGen	Generator for new variable names
	     */
	    public StructTracker (TypeStruct struct_,
	    					  FENode cx_,
	    					  TempVarGen varGen, int heapsize) {

	    	this.heapsize = heapsize;
	    	struct = struct_;
	    	cx = cx_;
	    	nextInstancePointer =
	    		new ExprVar (cx, varGen.nextVar ("_"+ struct.getName () +"_"+ "nextInstance_"));

	    	fieldArrays = new HashMap<String, ExprVar> ();
	    	for (int i = 0; i < struct.getNumFields (); ++i) {
	    		String field = struct.getField (i);
	    		fieldArrays.put (field,
	    				new ExprVar (cx,
	    						varGen.nextVar ("_"+struct.getName () +"_"+ field +"_")));
	    	}
	    }


	    public void addParams(List<Parameter> newParams){

	    	newParams.add(new Parameter(TypePrimitive.inttype, nextInstancePointer.getName (), Parameter.REF));

	    	for (String field : fieldArrays.keySet ()) {
	    		newParams.add(new Parameter(typeofFieldArr (field) , fieldArrays.get (field).getName (), Parameter.REF ));
	    	}
	    }

	    public void addActualParams(List<Expression> params){
	    	params.add(nextInstancePointer);
	    	for (String field : fieldArrays.keySet ()) {
	    		params.add(fieldArrays.get(field));
	    	}
	    }

	    public void registerAsParameters(SymbolTable symtab){
	    	String nip = nextInstancePointer.getName ();
	    	symtab.registerVar(nip,
                    TypePrimitive.inttype,
                    nextInstancePointer,
                    SymbolTable.KIND_FUNC_PARAM);

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
	    	types.add (TypePrimitive.inttype);
	    	inits.add (ExprConstant.createConstant (cx, "0"));

	    	for (String field : fieldArrays.keySet ()) {
	    		names.add (fieldArrays.get (field).getName ());
	    		types.add (typeofFieldArr (field));
	    		if (struct.getType (field).isStruct ())
	    			inits.add (initNull (cx));
	    		else
	    			inits.add (initZero (cx));
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
	    	return new ExprUnary (cx, ExprUnary.UNOP_POSTINC, nextInstancePointer);
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
	    public StmtAssert makeAllocationGuard (FENode cx) {
	    	return new StmtAssert (cx, this.getAllocationSafetyCheck (cx), "Heap is too small. Make it bigger with the --heapsize flag");
	    }

	    /**
	     * @return an expression to check for allocation safety.  Essentially,
	     * <code>(Foo_nextPointer + 1) &lt; len (Foo_instances)</code>.
	     */
	    public Expression getAllocationSafetyCheck (FENode cx) {
	    	return new ExprBinary (cx, nextInstancePointer, "<", getNumInstsExpr(cx));
	    }

	    /**
	     * Return an Expression containing the number of instances of this
	     * struct.
	     *
	     * @param cx  Context where the Expression will be used.
	     * @return
	     */
	    public Expression getNumInstsExpr (FENode cx) {
	    	return ExprConstant.createConstant (cx, ""+ heapsize);
	    }

	    /**
	     * Get the type of the field array for 'field'.
	     *
	     * @param field
	     * @return
	     */
	    private Type typeofFieldArr (String field) {
	    	Type fieldType = struct.getType (field);
	    	return new TypeArray (
	    		fieldType.isStruct () ? TypePrimitive.inttype : fieldType,
	    		getNumInstsExpr (cx));
	    }

	    /** Return a null-initializer. */
	    private Expression initNull (FENode cx) {
	    	return new ExprArrayInit (cx,
	    			Collections.nCopies (heapsize, (Expression) ExprNullPtr.nullPtr));
	    }

	    /** Return a null-initializer. */
	    private Expression initZero (FENode cx) {
	    	return new ExprArrayInit (cx,
	    			Collections.nCopies (heapsize, (Expression) ExprConstInt.zero));
	    }
	}
}
