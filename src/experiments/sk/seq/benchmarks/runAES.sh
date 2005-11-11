cd ../../sbin
source install.sh
cd ../test/benchmarks

for x in `ls aesFullStage*.sk`
do 
echo "RUNNING $x  $d";
bash preproc.sh --incremental 5 ${x} &> ${x}.incr.output ;
done;

for x in `ls aesFullStage*.sk`
do 
echo "RUNNING $x  $d";
bash preproc.sh  ${x} &> ${x}.output ;
done;
