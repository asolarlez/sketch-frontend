
echo "" > runoutput;
for x in `ls miniTest*.sk`
do 


echo "RUNNING $x  $d";

bname=`echo  $x | sed 's/\.sk//'`

rm -f ${bname}.cpp
rm -f ${bname}.h


bash sketch --outputtest  --outputcode -synth ABC -verif ABC  --incremental 6 --seed 10 ${x} &> ${x}.output ;
bash script >> runoutput;
done;

rm *.tmp;


cat runoutput;
grep 'Automated testing passed' runoutput | awk '{ print $5".sk.output"; }' > cur
cat cur;
echo "LISTED BELOW ARE THE FAILED TESTS (IF ANY)"
diff  -w cur ref 
echo "END OF LIST"
rm cur
rm runoutput
