/**
 *
 */
package streamit.frontend.passes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstant;
import streamit.frontend.nodes.ExprField;
import streamit.frontend.nodes.ExprNew;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.SymbolTable;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;

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
 *   assert (Foo_nextInstance + 1) < NUM_FOO;
 *   int f1 = ++Foo_nextInstance;
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

	public EliminateStructs (TempVarGen varGen_) {
		super(null);
		structs = new HashMap<String, StructTracker> ();
		varGen = varGen_;
	}

	/**
	 * Add variable declarations to the body of 'func', and rewrite its body.
	 */
	public Object visitFunction (Function func) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);

        for (Iterator iter = func.getParams().iterator(); iter.hasNext(); ) {
            Parameter param = (Parameter)iter.next();
            symtab.registerVar(param.getName(),
                               actualType(param.getType()),
                               param,
                               SymbolTable.KIND_FUNC_PARAM);
        }
        List<Statement> newBodyStmts = new LinkedList<Statement> ();
        for (String name : structsByName.keySet ()) {
			StructTracker tracker =
				new StructTracker (structsByName.get (name), func.getCx (), varGen);
			tracker.registerVariables (symtab);
			newBodyStmts.add (tracker.getVarDecls ());
			structs.put (name, tracker);
		}

        Function func2 = (Function) super.visitFunction(func);
        symtab = oldSymTab;

        Statement oldBody = func2.getBody ();
        newBodyStmts.add (oldBody);
        StmtBlock newBody = new StmtBlock (oldBody.getCx (), newBodyStmts);

        return new Function (func2.getCx (), func2.getCls (), func2.getName (),
        			func2.getReturnType (), func2.getParams (),
        			func2.getSpecification (), newBody);
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

		return new ExprArrayRange (struct.getFieldArray (field),
				(Expression) ef.getLeft ().accept (this));
	}

	/**
	 * Rewrite the 'new' expression into a guarded pointer increment.
	 */
    public Object visitExprNew (ExprNew expNew){
    	if (false == expNew.getTypeToConstruct().isStruct()) {
    		expNew.report ("Sorry, only structs are supported in 'new' statements.");
    		throw new RuntimeException ("unsupported type in 'new' statement.");
    	}

    	StructTracker struct =
    		structs.get (((TypeStruct) expNew.getTypeToConstruct ()).getName ());

    	this.addStatement (struct.makeAllocationGuard (expNew.getCx ()));
    	return struct.makeAllocation (expNew.getCx ());
    }

    /** Rewrite variables of type 'struct' into ones of type 'int'. */
    public Object visitTypeStruct (TypeStruct ts) {
    	return TypePrimitive.inttype;
    }

    /**
	 * Tracks variables used by structs when we eliminate 'new' expressions
	 * and field accesses.  Also provides convenience expression generators.
	 *
	 * @author Chris Jones
	 */
	private class StructTracker {
		private TypeStruct struct;
		private FEContext cx;
	    private ExprVar nextInstancePointer;
	    private Map<String, ExprVar> fieldArrays;

	    private final static int NUM_INSTANCES = 100;

	    /**
	     * Create a tracker of the variables used to eliminate allocs and
	     * field accesses of [struct_] variables.
	     *
	     * @param struct_	The struct to track
	     * @param cx_		The context at which new variables will be declared
	     * @param varGen	Generator for new variable names
	     */
	    public StructTracker (TypeStruct struct_,
	    					  FEContext cx_,
	    					  TempVarGen varGen) {
	    	struct = struct_;
	    	cx = cx_;
	    	nextInstancePointer =
	    		new ExprVar (cx, varGen.nextVar (struct.getName () +"_"+ "nextInstance"));

	    	fieldArrays = new HashMap<String, ExprVar> ();
	    	for (int i = 0; i < struct.getNumFields (); ++i) {
	    		String field = struct.getField (i);
	    		fieldArrays.put (field,
	    				new ExprVar (cx,
	    						varGen.nextVar (struct.getName () +"_"+ field)));
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
	    		inits.add (ExprConstant.createConstant (cx, "0"));
	    	}

	    	return new StmtVarDecl (cx, types, names, inits);
	    }

	    /**
	     * Allocate a new object of this struct type.
	     *
	     * @param cx  the context of the allocation site
	     * @return    an allocation expression
	     */
	    public Expression makeAllocation (FEContext cx) {
	    	return new ExprUnary (cx, ExprUnary.UNOP_PREINC, nextInstancePointer);
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
	    public StmtAssert makeAllocationGuard (FEContext cx) {
	    	return new StmtAssert (cx, this.getAllocationSafetyCheck (cx));
	    }

	    /**
	     * @return an expression to check for allocation safety.  Essentially,
	     * <code>(Foo_nextPointer + 1) &lt; len (Foo_instances)</code>.
	     */
	    public Expression getAllocationSafetyCheck (FEContext cx) {
	    	ExprBinary next =
	    		new ExprBinary (cx, ExprBinary.BINOP_ADD, nextInstancePointer,
	    				ExprConstant.createConstant (cx, "1"));
	    	return new ExprBinary (cx, ExprBinary.BINOP_LT, next, getNumInstsExpr(cx));
	    }

	    /**
	     * Return an Expression containing the number of instances of this
	     * struct.
	     *
	     * @param cx  Context where the Expression will be used.
	     * @return
	     */
	    public Expression getNumInstsExpr (FEContext cx) {
	    	return ExprConstant.createConstant (cx, ""+ NUM_INSTANCES);
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
	}
}
