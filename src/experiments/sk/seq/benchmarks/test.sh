cd ../../sbin
source install.sh
cd ../test/benchmarks

rm -f reverse.report.txt
echo generating reverse_W4.txt
bash preproc.sh -D W 4     reverse.sk &> reverse_W4.txt
echo generating reverse_W8.txt
bash preproc.sh -D W 8     reverse.sk &> reverse_W8.txt
echo generating reverse_W12.txt
bash preproc.sh -D W 12     reverse.sk &> reverse_W12.txt
echo generating reverse_W16.txt
bash preproc.sh -D W 16     reverse.sk &> reverse_W16.txt

rm -f KaratsubaPoly.report.txt
echo generating KaratsubaPoly_WW2.txt
bash preproc.sh -D WW 2     KaratsubaPoly.sk &> KaratsubaPoly_WW2.txt
echo generating KaratsubaPoly_WW4.txt
bash preproc.sh -D WW 4     KaratsubaPoly.sk &> KaratsubaPoly_WW4.txt
echo generating KaratsubaPoly_WW6.txt
bash preproc.sh -D WW 6     KaratsubaPoly.sk &> KaratsubaPoly_WW6.txt

rm -f tblcrc.report.txt
echo generating tblcrc_X1N1.txt
bash preproc.sh -D X 1 -D N 1     tblcrc.sk &> tblcrc_X1N1.txt
echo generating tblcrc_X1N2.txt
bash preproc.sh -D X 1 -D N 2     tblcrc.sk &> tblcrc_X1N2.txt
echo generating tblcrc_X2N1.txt
bash preproc.sh -D X 2 -D N 1     tblcrc.sk &> tblcrc_X2N1.txt
echo generating tblcrc_X2N2.txt
bash preproc.sh -D X 2 -D N 2     tblcrc.sk &> tblcrc_X2N2.txt
echo generating tblcrc_X3N1.txt
bash preproc.sh -D X 3 -D N 1     tblcrc.sk &> tblcrc_X3N1.txt
echo generating tblcrc_X3N2.txt
bash preproc.sh -D X 3 -D N 2     tblcrc.sk &> tblcrc_X3N2.txt

rm -f tblcrc2.report.txt
echo generating tblcrc2_X1N1.txt
bash preproc.sh -D X 1 -D N 1     tblcrc.sk &> tblcrc2_X1N1.txt
echo generating tblcrc2_X1N2.txt
bash preproc.sh -D X 1 -D N 2     tblcrc.sk &> tblcrc2_X1N2.txt
echo generating tblcrc2_X1N4.txt
bash preproc.sh -D X 1 -D N 4     tblcrc.sk &> tblcrc2_X1N4.txt
echo generating tblcrc2_X1N8.txt
bash preproc.sh -D X 1 -D N 8     tblcrc.sk &> tblcrc2_X1N8.txt

rm -f parity.report.txt
echo generating parity_W2.txt
bash preproc.sh -D W 2     parity.sk &> parity_W2.txt
echo generating parity_W4.txt
bash preproc.sh -D W 4     parity.sk &> parity_W4.txt
echo generating parity_W8.txt
bash preproc.sh -D W 8     parity.sk &> parity_W8.txt
echo generating parity_W12.txt
bash preproc.sh -D W 12     parity.sk &> parity_W12.txt
echo generating parity_W16.txt
bash preproc.sh -D W 16     parity.sk &> parity_W16.txt
echo generating parity_W20.txt
bash preproc.sh -D W 20     parity.sk &> parity_W20.txt
echo generating parity_W24.txt
bash preproc.sh -D W 24     parity.sk &> parity_W24.txt
echo generating parity_W32.txt
bash preproc.sh -D W 32     parity.sk &> parity_W32.txt

rm -f log2.report.txt
echo generating log2_W4.txt
bash preproc.sh -D W 4     log2.sk &> log2_W4.txt
echo generating log2_W8.txt
bash preproc.sh -D W 8     log2.sk &> log2_W8.txt
echo generating log2_W12.txt
bash preproc.sh -D W 12     log2.sk &> log2_W12.txt
echo generating log2_W16.txt
bash preproc.sh -D W 16     log2.sk &> log2_W16.txt
echo generating log2_W20.txt
bash preproc.sh -D W 20     log2.sk &> log2_W20.txt
echo generating log2_W24.txt
bash preproc.sh -D W 24     log2.sk &> log2_W24.txt

rm -f logcount.report.txt
echo generating logcount_W4.txt
bash preproc.sh -D W 4     logcount.sk &> logcount_W4.txt
echo generating logcount_W8.txt
bash preproc.sh -D W 8     logcount.sk &> logcount_W8.txt
echo generating logcount_W16.txt
bash preproc.sh -D W 16     logcount.sk &> logcount_W16.txt

rm -f *.tmp
