public class DenseMV {

    final int N = 5;

    public static void main (String arg[]) {
     final int N = 5;
     int[][] A = new int[N][N];
     int[]   x = new int[N];
     A[0][0] = 2;
     x[0] = 3;
     int[] y = multiply(A, x);
//     System.out.println("Output: "+y[0]+", "+y[1]);
    }
    
    //@ requires a.length == b.length
    //@ ensures \result 
    static int dot(/*@ non_null */ int[] a, /*@ non_null */ int[] b) {
        return 1;
    }

    //@ ensures (\forall int i; 0<=i<y.length ==> y[i] == dot(A[i],x))
    static int[] multiply(/*@ non_null */ int[][] A, /*@ non_null */ int[] x) {
        int[] y = new int[A.length];
        //@ assume y.length == A.length;
        for (int i = 0; i < A.length; i++) {
            y[i] = 0;
            //@ assume x.length == A[i].length;
            //@ assume A[i] != null;
            for (int j = 0; j < A[i].length; j++) {
                y[i] += A[i][j]*x[j];
            }
        }
      return y; 
    }
  }