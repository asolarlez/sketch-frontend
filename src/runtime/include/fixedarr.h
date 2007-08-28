#ifndef __fixedarr_H
#define __fixedarr_H

template<typename T, size_t N> class fixedarr;


template<typename T> 
class fixedarrRef{
	T* ref;
	int sz;
	public:
	fixedarrRef(T* t, int psz){ ref = t; sz = psz;}
	template <size_t Ntag> 
	const fixedarrRef<T> &operator=(const fixedarr<T, Ntag> &v){
		assert (Ntag <=sz);
		for(int i=0; i<Ntag; ++i){
			ref[i] = v[i].val();
		}
		return *this;
	}
	
	const fixedarrRef<T> &operator=(const T &v){
		ref[0] = v;
		return *this;
	}
	
	T val(){ return ref[0]; }
};

template<typename T, size_t N> 
class fixedarr{
public:
   T arr[N];
   fixedarr(){
	  for(int i=0; i<N; ++i){
        arr[i] = 0;
      }
   }
   fixedarr(unsigned i){
      arr[0] = i;
   }

   fixedarr(const fixedarr<T, N>& ar){
      for(int i=0; i<N; ++i){
        arr[i] = ar.arr[i];
      }
   }

   template <size_t Ntag> const fixedarr<T,Ntag> sub (size_t offset) const{
      fixedarr<T, Ntag> tmp;
      assert (offset + Ntag <= N);
      for(int i=0; i<Ntag; ++i){
              tmp.arr[i] = arr[i+offset];
      }      
      return tmp;
   }
   
   inline fixedarr<T,N>& v(size_t i, T val){
   		arr[i] = val;
   		return *this;
   }
   
   inline fixedarrRef<T> operator[] (const size_t i) const{
   	    assert ( i < N);
   		return fixedarrRef<T>((unsigned int*)&arr[i], N-i);
   }

    inline const T first() const{
    	return arr[0];	
    }
    inline operator unsigned int (void) const {
		return arr[0];
    }
    
    const bool operator!=(const bitvec<N> bv) const{
    	for(int i=0; i<N; ++i){
    		if( (*this)[i] != bv[i] ){ return true; }	
    	}	
    	return false;
    }
   
};


template <typename T, size_t Ntag> ostream &
operator<< (ostream &out, fixedarr<T, Ntag> &v)
{
    for (int i = 0; i < Ntag; i++)
	out << v[i].val()<<",";
    return out;
}

#endif 

