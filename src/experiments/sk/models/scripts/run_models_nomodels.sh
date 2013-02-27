#!/bin/bash
ms="/afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/sketch-frontend/msketch --fe-cegis-path  /afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/sketch-backend/src/SketchSolver/cegis"
cd /afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/Dropbox/models

for i in `ls ./moresketchified_model_usage`; do
	for j in 1 2 3 4 5; do
		cd ./moresketchified_model_usage/$i
		$ms model.sk -V 10 &> "../../outputs/mn/$i.model_rt$tr.txt";
		itr=`grep 'GOT THE CORRECT' "../../outputs/mn/$i.model_rt$tr.txt" | awk '{print $6}'`;
		time=`grep 'Total time' "../../outputs/mn/$i.model_rt$tr.txt" | awk '{print $4}'`;
		echo "$i iter=$itr time=$time";
	done
done
