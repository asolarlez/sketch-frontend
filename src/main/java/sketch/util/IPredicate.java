package sketch.util;


public interface IPredicate<T> {

	IPredicate<String> PassThrough = new PassThrough();
	IPredicate<String> HoleVarPredicate = new HoleVarPredicate();

	public boolean accept(T obj);
	
	public static class HoleVarPredicate implements IPredicate<String> {
		public static final String HOLE_PREFIX = "H__";

        
        public boolean accept(String obj) {
            return obj.startsWith(HOLE_PREFIX);
        }
		
	}
	
	public static class PassThrough implements IPredicate<String> {
		
		public boolean accept(String obj) {
			return true;
		}
	}
}
