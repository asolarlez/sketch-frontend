#!/bin/bash
ms='/afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/sketch-frontend/msketch --fe-cegis-path  /afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/sketch-backend/src/SketchSolver/cegis'
cd /afs/csail.mit.edu/u/r/rohitsingh/public_html/sketch4jan/Dropbox/models 

for i in `ls ./moresketchified_model_usage/`; do
	cd ./moresketchified_model_usage/$i
	for j in 1 2 3 4 5; do
		$ms model.sk -V 10 --beopt:simplifycex NOSIM --beopt:-bndwrand 125 --slv-timeout 2 &> "../../outputs/mn/$i.model_temp$j.txt";
		ftime=`cat "../../outputs/mn/$i.model_temp$j.txt" | grep "FIND TIME" | awk '{print $3}'`;
		ctime=`cat "../../outputs/mn/$i.model_temp$j.txt" | grep "FIND TIME" | awk '{print $6}'`;
		echo "$i model $ftime $ctime";
	done
	nm=`ls nomodel*.sk`
	for j in 1 2 3 4 5; do
		$ms $nm -V 10 --beopt:simplifycex NOSIM --beopt:-bndwrand 125 --slv-timeout 2 &> "../../outputs/mn/$i.nomodel_temp$j.txt";
		ftime=`cat "../../outputs/mn/$i.nomodel_temp$j.txt" | grep "FIND TIME" | awk '{print $3}'`;
		ctime=`cat "../../outputs/mn/$i.nomodel_temp$j.txt" | grep "FIND TIME" | awk '{print $6}'`;
		echo "$i nomodel $ftime $ctime";
	done
	cd ../../
done

