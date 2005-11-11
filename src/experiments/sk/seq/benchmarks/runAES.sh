cd ../../sbin
source install.sh
cd ../test/benchmarks

for x in `ls aesFullStage*.sk`
do 
echo "RUNNING $x  $d";
bash preproc.sh --incremental 5 ${x} &> ${x}.incr.out ;
bash collectSideInfo.sh ${x}.incr.out
done;

for x in `ls aesFullStage*.sk`
do 
echo "RUNNING $x  $d";
bash preproc.sh  ${x} &> ${x}.out ;
bash collectSideInfo.sh ${x}.out
done;
