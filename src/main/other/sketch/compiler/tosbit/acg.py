def splitreplace(line,lookfor,replacement):
	if line==lookfor:
		return [replacement]
	ss=line.split(lookfor)
	if len(ss)>1:
		ret=[]
		ret=ret+dosplit(ss.pop(0))
		for f in ss:
			ret.append(replacement)
			ret=ret+dosplit(f)
		return ret
	return None

def dosplit(line):
	ret=splitreplace(line,"Array2DPtr","@arrayPtrType");
	if ret: return ret
	ret=splitreplace(line,"Array2D","@arrayType");
	if ret: return ret
	ret=splitreplace(line,"double**","@dataType(dim)");
	if ret: return ret
	ret=splitreplace(line,"double*","@dataType(dim-1)");
	if ret: return ret
	ret=splitreplace(line,"for(int i0=0;i0<dim0;i0++) for(int i1=0;i1<dim1;i1++) ","@loopNest(dim)");
	if ret: return ret
	ret=splitreplace(line,"for(int i0=0;i0<dim0;i0++) ","@loopNest(dim-1)");
	if ret: return ret
	ret=splitreplace(line,"int d0, int d1","@ctorParams(dim)");
	if ret: return ret
	ret=splitreplace(line,"dim0(d0),dim1(d1)","@ctorInits(dim)");
	if ret: return ret
	ret=splitreplace(line,"dim0(p.dim0),dim1(p.dim1)","@ctorInits2(dim)");
	if ret: return ret
	ret=splitreplace(line,"dim0,dim1","@dimFields(dim)");
	if ret: return ret
	ret=splitreplace(line,"<delete>\\n","@makeDeleteCode(dim)");
	if ret: return ret
	ret=splitreplace(line,"<alloc>\\n","@makeAllocCode(dim)");
	if ret: return ret
	ret=splitreplace(line,"<assert>\\n","@makeAssertCode(dim)");
	if ret: return ret
	ret=splitreplace(line,"<dimfields>\\n","@makeDimFields(dim)");
	if ret: return ret
	ret=splitreplace(line,"[i0][i1]","@indexNest(dim)");
	if ret: return ret
	return [line]


f=open("array.tem")
for line in f:
	line=line.rstrip()+'\n'
	line=line.encode('string_escape')
	ss=dosplit(line)
	for f in ss:
		if len(f)>0 and f[0]=='@':
			str=f[1:]
		else:
			str='"'+f+'"'
		print "\t\t\tbuf.append(" + str + ");"
	print
