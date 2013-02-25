#!/bin/bash 
ms="/afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/sketch-frontend/msketch --fe-cegis-path  /afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/sketch-backend/src/SketchSolver/cegis"
cd /afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/Dropbox/models
for tr in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
	for i in `ls ./model_usage`; do
		cd ./model_usage/$i
		$ms --slv-timeout 10 model.sk --beopt:simplifycex NOSIM --be:angelic-model -V 10 &> "../../outputs/$i.model_angelic_trial$tr.txt";
		itr=`grep 'GOT THE CORRECT' "../../outputs/$i.model_angelic_trial$tr.txt" | awk '{print $6}'`;
		time=`grep 'Total time' "../../outputs/$i.model_angelic_trial$tr.txt" | awk '{print $4}'`;
		echo "$i iter=$itr time=$time";
		cd ../../
	done
done
