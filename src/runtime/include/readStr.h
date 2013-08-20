#include "stdio.h"
#include "string.h"
#include "stdlib.h"
#include "vops.h"

namespace str{

inline Str* _readStr(){
	Str * s = Str::create(10, NULL, 0);
	fgets(s->buf,s->n,stdin);
	while(s->buf[strlen(s->buf)-1]!='\n')
	{
		Str * temp = Str::create(s->n*2, s->buf, s->n);
		delete(s);
		s = temp;
		fgets(s->buf+s->n/2-1,s->n/2+1,stdin);
	}
	return s;
}
}
