#include "stdio.h"
#include "string.h"
#include "stdlib.h"
#include "vops.h"

namespace fileIO{

inline int _isEmpty(FILE* cfile){
	if(feof(cfile)!=0)
		return 1;
	else
		return 0;	
}
}