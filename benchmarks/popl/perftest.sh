
RESDIR=$1
echo "Creating Results Directory " $RESDIR
mkdir $RESDIR



for ii in {1..10}
do

for x in *.sk
do
echo "Running" $x
BNAME=${x/*\//}
bash sketch -V 5  --slv-seed $ii  --fe-inc '../sketchlib' --slv-timeout 45  $x > $RESDIR/$BNAME.$ii 2>&1
python outputToDB.py $RESDIR/$BNAME.$ii $ii
done

done 