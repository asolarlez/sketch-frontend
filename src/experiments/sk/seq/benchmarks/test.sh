cd ../../sbin
source install.sh
cd ../test/benchmarks

rm -f reverse.report.txt
echo generating reverse_W4.txt
bash preproc.sh -D W 4     reverse.sk &> reverse_W4.txt
bash postproc.sh reverse_W4.txt reverse.report.txt
echo generating reverse_W8.txt
bash preproc.sh -D W 8     reverse.sk &> reverse_W8.txt
bash postproc.sh reverse_W8.txt reverse.report.txt
echo generating reverse_W12.txt
bash preproc.sh -D W 12     reverse.sk &> reverse_W12.txt
bash postproc.sh reverse_W12.txt reverse.report.txt
echo generating reverse_W16.txt
bash preproc.sh -D W 16     reverse.sk &> reverse_W16.txt
bash postproc.sh reverse_W16.txt reverse.report.txt

rm -f aesFullStage.report.txt
echo generating aesFullStage_W32.txt
bash preproc.sh -D W 32   --incremental 5  aesFullStage.sk &> aesFullStage_W32.txt
bash postproc.sh aesFullStage_W32.txt aesFullStage.report.txt

rm -f aesFullStageBest.report.txt
echo generating aesFullStageBest_W32.txt
bash preproc.sh -D W 32   --incremental 5  aesFullStageBest.sk &> aesFullStageBest_W32.txt
bash postproc.sh aesFullStageBest_W32.txt aesFullStageBest.report.txt

rm -f aesFullStageClean.report.txt
echo generating aesFullStageClean_W32.txt
bash preproc.sh -D W 32   --incremental 5  aesFullStageClean.sk &> aesFullStageClean_W32.txt
bash postproc.sh aesFullStageClean_W32.txt aesFullStageClean.report.txt

rm -f *.tmp
