package streamit.misc;

import java.io.IOException;
import java.io.OutputStream;

public class TruncatedOutputStream extends OutputStream {
	StringBuffer sbuf = new StringBuffer();	
	final int sz = 2000;
	char [] cb = new char[sz];
	int idx  =0;
	int beg = 0;
	@Override
	public void write(int arg0) throws IOException {
		// TODO Auto-generated method stub
		if(sbuf.length() < sz){
			sbuf.append( Character.toChars(arg0)  );
		}else{
			char [] tmp = Character.toChars(arg0) ; 
			for(int i=0; i<tmp.length; ++i){
				cb[idx] = tmp[i]; 
				idx = (idx + 1) % sz;
				if(idx == beg){ beg = (beg + 1) % sz;}
			}
		}
	}
	
	public String toString(){
		StringBuffer sb2 = new StringBuffer();
		int t = idx;
		int i= beg;
		while(i != t){
			sb2.append(cb[i]);
			i = (i + 1) % sz;
		}
		return sbuf.toString() + sb2.toString();
	}

}