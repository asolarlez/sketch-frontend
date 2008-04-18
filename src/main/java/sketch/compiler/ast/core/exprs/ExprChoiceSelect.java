package streamit.frontend.nodes;

public class ExprChoiceSelect extends Expression {
	private Expression obj;
	private Selector field;

	public ExprChoiceSelect (Expression obj, Selector field) {
		this (obj, obj, field);
	}

	public ExprChoiceSelect (FENode cx, Expression obj, Selector field) {
		super (cx);
		this.obj = obj;
		this.field = field;
	}

	public Expression getObj () { return obj; }
	public Selector getField () { return field; }

	public boolean isLValue () {
		return true;
	}

	public String toString () {
		return obj.toString () + field.toString ();
	}

	@Override
	public Object accept (FEVisitor v) {
		return v.visitExprChoiceSelect (this);
	}

	public Object accept (SelectorVisitor sv) {
		return field.accept (sv);
	}

	public static abstract class Selector {
		public abstract Object accept (SelectorVisitor sv);
		public abstract boolean isOptional ();
	}

	public static abstract class SelectorVisitor {
		public abstract Object visit (SelectOrr so);
		public abstract Object visit (SelectChain sc);
		public abstract Object visit (SelectField sf);
	}

	public static abstract class Select extends Selector {
		boolean optional;

		public Select () {  this.optional = false;  }

		public Select (Select s) {
			this.optional = s.optional;
		}

		@Override public boolean isOptional () {
			return optional;
		}
		public void setOptional (boolean optional) {
			this.optional = optional;
		}

		public static Select clone (Select s) {
			return s instanceof SelectOrr ? new SelectOrr (s)
						: s instanceof SelectChain ? new SelectChain (s)
								: new SelectField (s);
		}
	}

	public static class SelectOrr extends Select {
		private Select ths, that;

		public SelectOrr (Select ths, Select that) {
			this.ths = ths;
			this.that = that;
		}

		public SelectOrr (Select s) {
			super (s);
			if (s instanceof SelectOrr) {
				this.ths = ((SelectOrr) s).ths;
				this.that = ((SelectOrr) s).that;
			}
		}

		public Selector getThis () { return ths; }
		public Selector getThat () { return that; }

		public String toString () {
			return "("+ ths +" | "+ that +")"+ (optional ? "?" : "");
		}
		public Object accept (SelectorVisitor sv) { return sv.visit (this); }
	}

	public static class SelectChain extends Select {
		private Select first, next;

		public SelectChain (Select first, Select next) {
			this.first = first;
			this.next = next;
		}

		public SelectChain (Select s) {
			super (s);
			if (s instanceof SelectChain) {
				this.first = ((SelectChain) s).first;
				this.next = ((SelectChain) s).next;
			}
		}

		public Selector getFirst () { return first; }
		public Selector getNext ()  { return next; }

		public String toString () {
			return (optional ? "(" : "")
				+ first + next
				+ (optional ? ")?" : "");
		}
		public Object accept (SelectorVisitor sv) { return sv.visit (this); }
	}

	public static class SelectField extends Select {
		private String field;

		public SelectField (String field) {
			this.field = field;
		}

		public SelectField (Select s) {
			super (s);
			if (s instanceof SelectField) {
				this.field = ((SelectField) s).field;
			}
		}

		public String getField () {  return field;  }

		public String toString () {
			return (optional ? "(" : "")
				+ "."+ field
				+ (optional ? ")?" : "");
		}
		public Object accept (SelectorVisitor sv) { return sv.visit (this); }
	}
}
