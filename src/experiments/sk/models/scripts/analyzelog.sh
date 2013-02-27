#!/bin/bash -e

sketch -V 10  --bnd-inbits $1 --bnd-unroll-amnt $2 --beopt:-bndwrand 125 --slv-timeout 2 model.sk &> /tmp/t1$3.txt
succ=`grep DONE /tmp/t1$3.txt | wc -l`
if [ $succ -eq 1 ]
then
	echo "SUCCESSFUL!"
	grep -n "\*\*\*" /tmp/t1$3.txt  
	echo "--"
	grep -n "growing" /tmp/t1$3.txt
	export n1=`grep -n "\*\*\*" /tmp/t1$3.txt  | sed '$d' | tail -1 | sed 's/:/ /g' | awk '{print $1}'`
	echo $n1;
	res=`grep -n "growing" /tmp/t1$3.txt |  sed 's/:/ /g'| awk '{if($1 < ENVIRON["n1"]) print $1,$8}'| tail -1 | awk '{print $2}'`
	echo "best inbits="$res
else
	echo "FAILED"
	less /tmp/t1$3.txt
fi

