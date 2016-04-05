
RESDIR=$1
echo "Creating Results Directory " $RESDIR
mkdir $RESDIR



for ii in {1..15}
do

for x in gallery/*.sk performance_benchmarks/*/*.sk
do
echo "Running" $x
BNAME=${x/*\//}
bash sketch -V 5  --slv-seed $ii   --fe-inc 'gallery' --fe-inc 'performance_benchmarks/imp_synt_benchmarks' --fe-inc '../sketchlib' --fe-tempdir . --slv-timeout 16  $x > $RESDIR/$BNAME.$ii 2>&1

done

done 