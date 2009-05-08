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

for dir in `ls -d barrier/`
do
for x in `ls ${dir}*2*.sk`
do

echo "RUNNING " $x "  " $((RANDOM % 37))


##%% psketch --timeout 15 --inc ${dir} --seed $((RANDOM % 20)) -olevel $OLEVEL --verbosity 4 --synth ${SYNTH} --verif ${VERIF} ${x} | "$FRONTEND"/sbin/pardataCollect.sh


rm /cygdrive/c/Documents\ and\ Settings/asolar.EECS/Local\ Settings/Temp/*.exe
rm /cygdrive/c/Documents\ and\ Settings/asolar.EECS/Local\ Settings/Temp/*pml*


done
done

