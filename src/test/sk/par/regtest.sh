for x in `ls *.sk`
do 

echo "RUNNING $x  $d";

bname=`echo  $x | sed 's/\.sk//'`

rm -f ${bname}.cpp
rm -f ${bname}.h


bash psketch   -synth MINI -verif MINI  --inlineamnt 14   ${x} &> ${x}.output ;

done;

rm *.tmp;


# diff -w current.output reference;
grep 'CORRECT' *.output | tr ':' ' ' | awk '{ print $1; }' > cur
echo "LISTED BELOW ARE THE FAILED TESTS (IF ANY)"
diff -w cur ref 
echo "END OF LIST"
rm cur
