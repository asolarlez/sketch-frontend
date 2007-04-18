




(time bash stencompile.sh  -overrideCtrls 3 -withrestrictions  -overrideInputs 4 --branchamnt 10  --inlineamnt 4  --unrollamnt 4  -synth ABC  -verif ABC  --seed 5 mgInterp2.sk | grep '\*' | grep -v ')' ) &> times.out



( time bash stencompile.sh  -overrideCtrls 3 -withrestrictions  -overrideInputs 4 --branchamnt 10  --inlineamnt 5  --unrollamnt 4 -synth ABC  -verif ABC  --seed 5 rb3dSimpleOddpp.sk| grep '\*' | grep -v ')' ) >> times.out 2>&1


( time bash stencompile.sh  -overrideCtrls 3 -withrestrictions  -overrideInputs 4 --branchamnt 10  --inlineamnt 5  --unrollamnt 4 -synth ABC  -verif ABC  --seed 5 mgInterpEasy.sk| grep '\*' | grep -v ')' ) >> times.out 2>&1


( time bash stencompile.sh  -overrideCtrls 3 -withrestrictions  -overrideInputs 4 --branchamnt 18  --inlineamnt 5  --unrollamnt 4 -synth ABC  -verif ABC  --seed 5 cacheObv1dGood2.sk| grep '\*' | grep -v ')' ) >> times.out 2>&1


( time bash stencompile.sh  -overrideCtrls 3 -withrestrictions  -overrideInputs 4 --branchamnt 10  --inlineamnt 5  --unrollamnt 4 -synth ABC  -verif ABC  --seed 5 rb3dpp.sk| grep '\*' | grep -v ')' ) >> times.out 2>&1


( time bash stencompile.sh  -overrideCtrls 3 -withrestrictions  -overrideInputs 4 --branchamnt 18  --inlineamnt 5  --unrollamnt 4 -synth ABC  -verif ABC  --seed 5 cacheObv1d4.sk| grep '\*' | grep -v ')' ) >> times.out 2>&1





