for x in `ls *.sk`
do 

echo "RUNNING $x  $d";
rm ${x}.output;
bash preproc.sh  --seed 10 ${x} &> ${x}.output ;
grep oracle.*\[[0-9]+\] ${x}.output ;
done;

rm *.tmp;

grep 'int.*oracle.*\[[0-9]*\]' *.output > current.output;

diff current.output reference;

