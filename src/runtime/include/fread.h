#include "stdio.h"
#include "string.h"
#include "stdlib.h"
#include "vops.h"

namespace fileIO{

inline str::Str* _fread(FILE* cfile){  //takes a file user declares, such as above
	
	str::Str *s = str::Str::create(10, (char*) NULL, 0);
	fgets(s->buf,s->n,cfile);
	
	if (!(s->buf[0])){
		return NULL;}

	while(s->buf[strlen(s->buf)-1]!='\n' && feof(cfile)==0 ){
	
		str::Str *temp = str::Str::create(s->n*2, s->buf, s->n);
		delete(s);
	
		s = temp;
	
		fgets(s->buf+s->n/2-1,s->n/2+1, cfile);
	
	}
	return s;
}
}
