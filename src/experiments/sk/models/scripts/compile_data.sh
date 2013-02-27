#!/bin/bash 
ms="/afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/sketch-frontend/msketch --fe-cegis-path  /afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/sketch-backend/src/SketchSolver/cegis"
#cd /afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/Dropbox/models

rm -f "outputs_organized/$1_new/all_angelic_$1.txt"
rm -f "outputs_organized/$1_new/all_old_cegis_$1.txt"
rm -f "outputs_organized/$1_new/all_cegis_$1.txt"

for tr in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
#for tr in 1 2; do
	cd ./model_usage/$1

	#angelic
	itr=`grep 'GOT THE CORRECT' "../../outputs_organized/$1_new/$1.model_angelic_trial$tr.txt" | awk '{print $6}'`;
	time=`grep 'Total elapsed time' "../../outputs_organized/$1_new/$1.model_angelic_trial$tr.txt" | awk '{print $5}'`;
	echo "iter=$itr time=$time" >> "../../outputs_organized/$1_new/all_angelic_$1.txt"

	#old cegis
	itr=`grep 'GOT THE CORRECT' "../../outputs_organized/$1_new/$1.model_old_cegis_trial$tr.txt" | awk '{print $6}'`;
	time=`grep 'Total elapsed time' "../../outputs_organized/$1_new/$1.model_old_cegis_trial$tr.txt" | awk '{print $5}'`;
	echo "iter=$itr time=$time" >> "../../outputs_organized/$1_new/all_old_cegis_$1.txt"

	#cegis
	itr=`grep 'GOT THE CORRECT' "../../outputs_organized/$1_new/$1.model_cegis_trial$tr.txt" | awk '{print $6}'`;
	time=`grep 'Total time' "../../outputs_organized/$1_new/$1.model_cegis_trial$tr.txt" | awk '{print $5}'`;
	echo "iter=$itr time=$time" >> "../../outputs_organized/$1_new/all_cegis_$1.txt"

	cd ../../
done
