for x in `ls *.sk`
do 

echo "RUNNING $x  $d";
rm ${x}.output;
echo "TIMES" > ${x}.times;

for z in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
do
echo "BEGIN TIME"

bash ./help.sh "$x" &> ${x}.output ;
grep "real.*s"  ${x}.output >> ${x}.times;

echo "END TIME"
done
grep oracle.*\[[0-9]+\] ${x}.output ;
done;

rm *.tmp;


