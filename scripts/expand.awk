BEGIN{
Nfun = 0;
}
function doopsel(ops, name, plist, hasmul){
rv =    "  if(??){ \n";
if(hasmul){
rv = rv "    bit tms = ??; \n"
rv = rv "    int xa = "name"(" plist ", times && !tms);\n"  ;
rv = rv "    int xb = "name"(" plist ", times && !tms);\n"  ;
}else{
rv = rv "    int xa = "name"(" plist ");\n" ; 
rv = rv "    int xb = "name"(" plist ");\n" ; 
}
for(top in ops){
if(ops[top] == "*"){
rv = rv "    if(tms && times){\n";
}else{
rv = rv "    if(??){\n ";
}
rv = rv "      int tt= xa "ops[top]" xb;\n";
rv = rv "      return tt;\n";
rv = rv "    }\n";
}
rv = rv "  }\n";
return rv;
}

function dofun(v, ops, hasmul){
name = "BigGen" Nfun ;
Nfun = Nfun + 1;
plen = length(v);
plist = ""; fplist = ""; olist = "";
for(i=0; i<plen-1; ++i){
plist = plist "A_" i ", ";
fplist = fplist "int A_" i ", ";
olist = olist v[i+1] ", ";
}
plist = plist "A_" (plen-1);
fplist = fplist "int A_" (plen-1);
olist = olist v[plen];
if(hasmul){
olist = olist ", true";
}
choice = plist;
gsub(/,/, "|", choice);
choice = "{|" choice "| ti |}";

opsel = doopsel(ops, name, plist, hasmul);

if(hasmul){
gen = "generator \nint " name "(" fplist ", bit times){ \n" ;
}else{
gen = "generator \nint " name "(" fplist "){ \n" ;

}
gen = gen  opsel "\n";
gen = gen "int ti = ??(2); \n" ;
gen = gen " return " choice "; \n";
gen = gen "}\n";

Generators = Generators gen;

return name "(" olist ")";

}
/\{\$.*\$\}/ { match($0, "\\{\\$.*\\$\\}");
               s = substr($0, RSTART, RLENGTH);
               s = substr(s, 3, length(s)-4);
               gsub(/ /, "", s);
               split(s, l1split,":"); 
               vrs = l1split[1];
               op = l1split[2];
               split(vrs, v,",");
               split(op, o, ",");

               rep = dofun(v, o, match(op, "\\*"));
               rv = $0;
               sub(/\{\$.*\$\}/, rep, rv);
		print rv; 
}	

$0 !~ /\{\$.*\$\}/ { print $0; }

END{
print "\n\n";
print Generators;
}
