for x in `ls *.sk`
do 

echo "RUNNING $x  $d";
rm ${x}.output;
bash preproc.sh  --ccode ${x}.cc --incremental 6 --seed 10 ${x} &> ${x}.output ;
grep oracle.*\[[0-9]+\] ${x}.output ;
done;

rm *.tmp;

grep -n '[0-9]' *.cc | cpp > current.output;

diff current.output reference;

