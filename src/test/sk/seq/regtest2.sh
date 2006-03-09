for x in `ls *.sk`
do 

bname=`echo  miniTest34.sk | sed 's/\.sk//'`

rm -f ${bname}*.c
rm -f ${bname}
rm -f ${bname}*.h




echo "RUNNING $x  $d";
bash preproc.sh --dovectorization --outputcfiles --outputscript --outputtest --incremental 6 --seed 10 ${x} &> ${x}.output ;

bash script > ${x}.sout ;


done;
(grep -slr 'passed' *.sout | sort > tmptmp1);
(ls *.sout | sort >> tmptmp2); 
echo "THE FOLLOWING TESTS FAILED:"; 
(if (diff tmptmp1 tmptmp2) then (echo "ALL TESTS PASSED") else (echo "END") fi ); 
rm tmptmp1 tmptmp2;
rm *.tmp;



