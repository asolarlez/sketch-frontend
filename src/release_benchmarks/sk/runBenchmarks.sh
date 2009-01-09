SYNTH=$1;
VERIF=$2;
OLEVEL=$3;

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

for x in `ls *.sk harder/*.sk`
do

echo "RUNNING " $x

sketch --timeout 18 --seed $((RANDOM % 20)) -olevel $OLEVEL --verbosity 10 --synth ${SYNTH} --verif ${VERIF} ${x} | "$FRONTEND"/sbin/dataCollect.sh

done

# sketch --timeout 18 --D W 16  --seed $((RANDOM % 20)) --verbosity 10 --synth ${SYNTH} --verif ${VERIF} logcount2.sk  | "$FRONTEND"/sbin/dataCollect.sh


# sketch --timeout 18 --D W 16  --seed $((RANDOM % 20)) --verbosity 10 --synth ${SYNTH} --verif ${VERIF} logcount.sk  | "$FRONTEND"/sbin/dataCollect.sh


