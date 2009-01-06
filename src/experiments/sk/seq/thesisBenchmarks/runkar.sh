





for x in `ls harder/k*.sk`
do

echo "RUNNING " $x

sketch --timeout 18 --seed $((RANDOM % 20)) --verbosity 10  ${x} | "$FRONTEND"/sbin/dataCollect.sh

done





