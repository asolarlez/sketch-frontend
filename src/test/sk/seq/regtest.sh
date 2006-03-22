for x in `ls *.sk`
do 

echo "RUNNING $x  $d";

bname=`echo  $x | sed 's/\.sk//'`

rm -f ${bname}.c
rm -f ${bname}.h

bash preproc.sh --outputcfiles --incremental 6 --seed 10 ${x} &> ${x}.output ;
grep oracle.*\[[0-9]+\] ${x}.output ;
done;

rm *.tmp;

grep -n '[0-9]' *.c | cpp | sed 's/:[0-9]*:/::/' > current.output;

diff current.output reference;

