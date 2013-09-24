for x in `ls *.sk`
do 

echo "RUNNING $x  $d";

bname=`echo  $x | sed 's/\.sk//'`

rm -f ${bname}.cpp
rm -f ${bname}.h


bash stencompile.sh --outputcfiles -overrideCtrls 3  -overrideInputs 3   --inlineamnt 2  --unrollamnt 4 -synth ABC  -verif ABC  --seed 10 ${x} &> ${x}.output ;
grep oracle.*\[[0-9]+\] ${x}.output ;
done;

rm *.tmp;

grep 'CORRECT' *.output

# grep -n '[0-9]' *.cpp | cpp | sed 's/:[0-9]*:/::/' > current.output;

# diff -w current.output reference;

