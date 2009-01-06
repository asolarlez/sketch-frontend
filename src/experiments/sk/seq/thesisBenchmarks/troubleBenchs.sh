

for SLV in MINI
do

VERIF=$SLV
SYNTH=$SLV 

for OLEVEL in 0 1 2 3 5 
do

for x in listReverseHarder.sk 
do

echo "BMRK = " $x " SYNTH = " $SYNTH " VERIF=" $VERIF " OLEVEL=" $OLEVEL;

sketch --timeout 25 --seed $((RANDOM % 20)) -olevel $OLEVEL --verbosity 10 --synth ${SYNTH} --verif ${VERIF} ${x} | "$FRONTEND"/sbin/dataCollect.sh

done

done

done

for SLV in ABC 
do

VERIF=$SLV
SYNTH=$SLV 

for OLEVEL in  0 1 2 3 5 6 7
do

for x in hardSort.sk 
do

echo "BMRK = " $x " SYNTH = " $SYNTH " VERIF=" $VERIF " OLEVEL=" $OLEVEL;

sketch --timeout 25 --seed $((RANDOM % 20)) -olevel $OLEVEL --verbosity 10 --synth ${SYNTH} --verif ${VERIF} ${x} | "$FRONTEND"/sbin/dataCollect.sh

done

done

done

SLV=MINI; 

VERIF=$SLV
SYNTH=$SLV 

sketch --timeout 25 --seed $((RANDOM % 20)) -olevel 5 --verbosity 10 --synth ${SYNTH} --verif ${VERIF} log2VarLoop.sk | "$FRONTEND"/sbin/dataCollect.sh
sketch --timeout 25 --seed $((RANDOM % 20)) -olevel 5 --verbosity 10 --synth ${SYNTH} --verif ${VERIF} harder/xpose.sk | "$FRONTEND"/sbin/dataCollect.sh
sketch --timeout 25 --seed $((RANDOM % 20)) -olevel 5 --verbosity 10 --synth ${SYNTH} --verif ${VERIF} harder/xposeBit.sk | "$FRONTEND"/sbin/dataCollect.sh


sketch --timeout 25 --seed $((RANDOM % 20)) -olevel 5 --verbosity 10 --synth ${SYNTH} --verif ${VERIF} log2VarLoop.sk | "$FRONTEND"/sbin/dataCollect.sh
sketch --timeout 25 --seed $((RANDOM % 20)) -olevel 5 --verbosity 10 --synth ${SYNTH} --verif ${VERIF} harder/xpose.sk | "$FRONTEND"/sbin/dataCollect.sh
sketch --timeout 25 --seed $((RANDOM % 20)) -olevel 5 --verbosity 10 --synth ${SYNTH} --verif ${VERIF} harder/xposeBit.sk | "$FRONTEND"/sbin/dataCollect.sh
