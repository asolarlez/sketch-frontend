#!/bin/bash
ms="/afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/sketch-frontend/msketch --fe-cegis-path  /afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/sketch-backend/src/SketchSolver/cegis"
cd /afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/models


for tr in 1 2 3 4 5; do
	for i in `ls ./model_usage`; do
		cd ./model_usage/$i
		$ms model.sk -V 10 &> "../../outputs/$i.model_trial$tr.txt";
		itr=`grep 'GOT THE CORRECT' "../../outputs/$i.model_trial$tr.txt" | awk '{print $6}'`;
		time=`grep 'Total time' "../../outputs/$i.model_trial$tr.txt" | awk '{print $4}'`;
		echo "$i iter=$itr time=$time";
		cd ../../
	done
done

