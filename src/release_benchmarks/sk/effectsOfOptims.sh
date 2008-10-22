

for SLV in ABC MINI
do

VERIF=$SLV
SYNTH=$SLV 

for OLEVEL in 7 0 1 2 3 5 6 7
do

for x in listReverseHarder.sk 
do

echo "BMRK = " $x " SYNTH = " $SYNTH " VERIF=" $VERIF " OLEVEL=" $OLEVEL;

sketch --timeout 15 --seed $((RANDOM % 20)) -olevel $OLEVEL --verbosity 10 --synth ${SYNTH} --verif ${VERIF} ${x} | "$FRONTEND"/sbin/dataCollect.sh

done

done

done

