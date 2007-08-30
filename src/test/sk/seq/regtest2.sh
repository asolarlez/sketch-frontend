for x in `ls *.sk`
do 

echo "RUNNING $x  $d";

bname=`echo  $x | sed 's/\.sk//'`

rm -f ${bname}.cpp
rm -f ${bname}.h


bash sketch --outputtest  --outputcode -synth ABC -verif ABC  --incremental 6 --seed 10 ${x} &> ${x}.output ;
bash script;
done;

rm *.tmp;

grep -n '[0-9]' *.cpp | cpp | sed 's/:[0-9]*:/::/' > current.output;

# diff -w current.output reference;
grep 'CORRECT' *.output | tr ':' ' ' | awk '{ print $1; }' > cur
echo "LISTED BELOW ARE THE FAILED TESTS (IF ANY)"
diff cur ref 
echo "END OF LIST"
rm cur
