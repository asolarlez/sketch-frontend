#!/bin/bash -e
ms='/afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/sketch-frontend/msketch --fe-cegis-path  /afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/sketch-backend/src/SketchSolver/cegis'
cd /afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/Dropbox/models

for i in `ls ./moresketchified_model_usage/`; do
		cd ./moresketchified_model_usage/$i
		$ms model.sk -V 10 $1 $2 $3 $4 $5 $6 &> "../../outputs/mn/$i.model_temp.txt";
		n=`grep -n "\*\*\*" "../../outputs/mn/$i.model_temp.txt"  | sed '$d' | tail -1 | sed 's/:/ /g' | awk '{print $1}'`
		res=`grep -n "growing" "../../outputs/mn/$i.model_temp.txt" |  sed 's/:/ /g'| awk -v num="$n" '{if($1 < $num) print $1,$8}'| tail -1 | awk '{print $2}'`
		echo $i".res="$res
		cd ../../
done

