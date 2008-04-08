package streamit.frontend.nodes;

public class ExprChoiceSelect extends Expression {
	private Expression obj;
	private Selector field;

	public ExprChoiceSelect (Expression obj, Selector field) {
		super (obj);
		this.obj = obj;
		this.field = field;
	}

	public String toString () {
		return obj.toString () + field.toString ();
	}

	@Override
	public Object accept (FEVisitor v) {
		return null; //v.visitExprChoiceSelect (this);
	}

	public static abstract class Selector {
	}

	public static abstract class Select extends Selector {
		boolean optional;

		public Select () {  this.optional = false;  }

		public Select (Select s) {
			this.optional = s.optional;
		}

		public static Select clone (Select s) {
			return s instanceof SelectOr ? new SelectOr (s)
						: s instanceof SelectChain ? new SelectChain (s)
								: new SelectField (s);
		}

		public void setOptional (boolean optional) {
			this.optional = optional;
		}
	}

	public static class SelectOr extends Select {
		private Select ths, that;

		public SelectOr (Select ths, Select that) {
			this.ths = ths;
			this.that = that;
		}

		public SelectOr (Select s) {
			super (s);
			if (s instanceof SelectOr) {
				this.ths = ((SelectOr) s).ths;
				this.that = ((SelectOr) s).that;
			}
		}

		public String toString () {
			return "("+ ths +" | "+ that +")"+ (optional ? "?" : "");
		}
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

		public String toString () {
			return (optional ? "(" : "")
				+ first + next
				+ (optional ? ")?" : "");
		}
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

		public String toString () {
			return (optional ? "(" : "")
				+ "."+ field
				+ (optional ? ")?" : "");
		}
	}
}
