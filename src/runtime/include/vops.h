#ifndef VOPS_H
#define VOPS_H 1
#include <cmath>
#include <iostream>
#include <cstdlib>
#include <cstring>
#include <sstream>
#include <string>

using namespace std;

class Parameters{
public:
	int niters;
	int verbosity;
	Parameters(int argc, char** argv){	
		niters = 100;	
		verbosity = 0;
		for(int i=0; i<argc; ++i){
			if(string(argv[i]) == "-n"){
				if(i==argc-1){
					cerr<<" -n requires a parameter"<<endl;
					exit(0);
				}
				niters = atoi(argv[i+1]);
				i+=1; 
				continue;
			}
			if(string(argv[i]) == "-v"){
				if(i==argc-1){
					cerr<<" -v requires a parameter"<<endl;
					exit(0);
				}
				verbosity = atoi(argv[i+1]);
				i+=1; 
				continue;
			}	
			if(string(argv[i])== "-h"){
				cout<<" -n iters \t number of iterations for test"<<endl;
				cout<<" -v level \t verbosity level"<<endl;
				continue;
			}	
		}
	}
};


class AssumptionFailedException{
public:
	AssumptionFailedException(){
	
	}
};

/*
template<typename T>
inline void CopyArr(T* lhs, T* rhs, int lsize, int rsize){
	if(lsize <= rsize){
		memcpy(lhs, rhs, sizeof(T)*lsize);
	}else{
		memcpy(lhs, rhs, sizeof(T)*rsize);
		memset(lhs+rsize, 0, sizeof(T)*(lsize-rsize));
	}
}
*/
template<typename T, typename W>
inline void CopyArr(T* lhs, W* rhs, int lsize, int rsize){
	if(lsize <= rsize){
		for(int i=0; i<lsize; ++i){
			lhs[i] = rhs[i];
		}
	}else{
		int i;
		for(i=0; i<rsize; ++i){
			lhs[i] = rhs[i];
		}
		for(;i<lsize; ++i){
			lhs[i] = 0;
		}		
	}
}


template<typename T, typename W>
inline bool arrCompare(T* lhs, int lsize, W* rhs, int rsize){
	int m = max(lsize, rsize);
	for(int i=0; i<m; ++i){
		W r = i<rsize? rhs[i] : (W)0;
		T l = i<lsize? lhs[i] : (T)0;
		if(r!=l){
			return false;
		}
	}
	return true;
}

template<typename T>
inline void CopyArr(T* lhs, T rhs, int lsize){
	lhs[0] = rhs; 
	if(lsize > 1){					
		memset(lhs+1, 0, sizeof(T)*(lsize-1));
	}
}


inline bool* SumArr(bool* _out/* len = N */, int N, bool* x/* len = L */, int L, bool* y/* len = M */, int M) {
  CopyArr<bool >(_out,0, N);
  int  T=((L) < (M) ? M : L);
  int TT = N<T?N:T;
  bool  t=0;
  for (int  i=0;(i) < (TT);i = i + 1){
    bool  tx=((i) < (L) ? (x[i]) : 0);
    bool  ty=((i) < (M) ? (y[i]) : 0);
    (_out[i]) = (t ^ ty) ^ tx;
    bool  s_2=((tx ^ t) & (t ^ ty)) ^ t;
    t = s_2;
  }
  return _out;
}


inline bool* shR(bool* ar, int len, int shamt){
	for(int i=0; i<len; ++i){
		if(i+shamt < len){
			ar[i] = ar[i+shamt];
		}else{
			ar[i] = false;
		}
	}
	return ar;
}

inline bool* shL(bool* ar, int len, int shamt){
	for(int i=len-1; i>=0; --i){
		if(i-shamt >=0){
			ar[i] = ar[i-shamt];
		}else{
			ar[i] = 0;
		}
	}
	return ar;
}

template<typename T>
inline string printArr(T* ar, int len){
	stringstream s;
	s<<"[";
	for(int i=0; i<len; ++i){
		if(i>0){ s<<", ";}
		s<<ar[i];
	}
	s<<"]";
	return s.str();
}

inline int bvToInt(bool* ar, int len){
	int p = 1;
	int sol = 0;
	for(int i=0; i<len; ++i){
		if(ar[i]){ sol += p; }
		p = p*2;
	}
	return sol;
}


template<typename T1, typename T2, typename T3>
inline bool* bitwise(T3 op, bool* out, int olen, T1* lhs, int lsize, T2* rhs, int rsize){
	int m = max(lsize, rsize);
	m = max(m,olen); 
	for(int i=0; i<m; ++i){
		bool r = i<rsize? rhs[i] : 0;
		bool l = i<lsize? lhs[i] : 0;
		if(i<olen){
			out[i] = op(r, l);
		}
	}
	return out;
}

template<typename T1>
inline bool* bitneg(bool* out, int olen, T1* lhs, int lsize){
	int m = max(lsize, olen);
	for(int i=0; i<m; ++i){
		bool l = i<lsize? !lhs[i] : 0;
		if(i<olen){
			out[i] = l;
		}
	}
	return out;
}

template<typename T>
inline void CopyArr(T* lhs, void** rhs, int lsize, int rsize){
	CopyArr(lhs, (T*)rhs, lsize, rsize);
}

#endif 
