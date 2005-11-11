cd ../../sbin
source install.sh
cd ../test/benchmarks

for d in  4  8 16 32
do

for x in `ls *.sk`
do 
echo "RUNNING $x  $d";
bash preproc.sh -D W ${d}  ${x} &> ${x}.${d}.output ;

done;

done;


