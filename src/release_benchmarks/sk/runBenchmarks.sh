SYNTH=$1;
VERIF=$2;

if [ $SYNTH=="" ]; then

SYNTH="MINI"

fi

if [ $VERIF=="" ]; then

VERIF="MINI"

fi
echo " SYNTH = " $SYNTH " VERIF=" $VERIF;

for x in `ls *.sk harder/*.sk`
do

echo "RUNNING " $x
sketch --seed 5 --verbosity 10 --synth ${SYNTH} --verif ${VERIF} ${x} | "$FRONTEND"/sbin/dataCollect.sh

done
