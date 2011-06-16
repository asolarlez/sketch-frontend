
SATSOLVER=$1;

if [ $SATSOLVER=="" ]; then

SATSOLVER="MINI"

fi

echo "USING SATSOLVER " $SATSOLVER;
ls mini*.sk | sed 's\.sk\.sk.output\g'> ref

time for x in `ls mini*.sk`
do 

echo "RUNNING $x  $d";

bname=`echo  $x | sed 's/\.sk//'`

rm -f ${bname}.cpp
rm -f ${bname}.h


bash sketch -V 5 --slv-synth $SATSOLVER --slv-verif $SATSOLVER   ${x} &> ${x}.output ;

done;

rm *.tmp;


# diff -w current.output reference;
grep 'DONE' *.output | tr ':' ' ' | awk '{ print $1; }' > cur
echo "LISTED BELOW ARE THE FAILED TESTS (IF ANY)"
diff -w cur ref 
echo "END OF LIST"
rm cur
