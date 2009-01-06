SYNTH=ABC;
VERIF=ABC;
OLEVEL=5;

if [ "$SYNTH" == "" ]; then

SYNTH="MINI"

fi

if [ "$VERIF" == "" ]; then

VERIF="MINI"

fi

if [ "$OLEVEL" == "" ]; then

OLEVEL="5"

fi


echo " SYNTH = " $SYNTH " VERIF=" $VERIF " OLEVEL=" $OLEVEL;

for x in `ls *.sk`
do

echo "RUNNING " $x

sketch --timeout 18 --seed $((RANDOM % 20)) -olevel $OLEVEL --verbosity 10 --synth ${SYNTH} --verif ${VERIF} ${x} | "$FRONTEND"/sbin/dataCollect.sh

done


