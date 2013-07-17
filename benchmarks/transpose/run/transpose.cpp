#include "spmd.h"
using namespace spmd;

#include <cstdlib>
#include <fstream>
#include <iostream>
#include <iomanip>
#include <sstream>
using std::string;
using std::cout;
using std::ofstream;
using std::stringstream;
using std::endl;
using std::setw;

#include "tr17.cpp"
using namespace npb;

int init(int nx, int ny, int nz, double ** matrix) {
	int nt = nx*ny*nz;
	int ntdivnp = nt / spmdnproc;
	int base = ntdivnp*spmdpid;
	double * a = new double[ntdivnp];
	for (int i=0; i<ntdivnp; i++) {
		a[i] = base + i;
	}
	*matrix = a;
	return ntdivnp;
}

void output(ofstream & fout, int nx, int ny, int nz, double * a) {
	double * p = a;
	for (int z=0; z<nz; z++) {
		for (int y=0; y<ny; y++) {
			for (int x=0; x<nx; x++) {
				fout << std::setw(3) << *p << ' ';
				p++;
			}
			fout << endl;
		}
		fout << endl;
	}
}

int main(int argc, char * argv[]) {
	mpiInit(&argc, &argv);
	if (argc < 4) {
	    cout << "Usage: mpirun -np <nproc> " << argv[0] << " nx ny nz [iters [outputMatrix?]]" << endl;
	    exit(1);
	}
	
	int nx = atoi(argv[1]);
	int ny = atoi(argv[2]);
	int nz = atoi(argv[3]);
	int iters = 0;
	bool outputMatrix = false;
	if (argc > 4) {
		iters = atoi(argv[4]);
	}
	if (argc > 5) {
		outputMatrix = (bool)atoi(argv[5]);
	}
	if (!(nx>0 && ny>0 && nz>0 && nz%spmdnproc==0 && ny%spmdnproc==0)) {
		cout << "Wrong configuration nx, ny, nz!" << endl;
		exit(2);
	}

	stringstream pidss;
	pidss << spmdpid;
	string const pidstr(pidss.str());
	stringstream fname;
	fname << "transpose.out" << pidstr << ".txt";

	ofstream fout(fname.str().c_str());

	double * matrix;
	int ntdivnp = init(nx, ny, nz, &matrix);
	double * result = new double[ntdivnp];

	if (outputMatrix) {
		fout << "initial matrix:" << endl;
		output(fout, nx, ny, nz/spmdnproc, matrix);
	}

	//cout << spmdpid << ": " << "after init" << endl;

	timer t_warm;
	timer * t_trans = new timer[iters];

	// warmup
	mpiBarrier();
	t_warm.start();
	transpose_xy_z(nx, ny, nz, matrix, result);
	t_warm.stop();
	
	if (outputMatrix) {
		fout << "after transpose_xy_z:" << endl;
		output(fout, nz, nx, ny/spmdnproc, result);
	}

	mpiBarrier();
	for (int i=0; i<iters; i++) {
		timer & t = t_trans[i];
		t.start();
		transpose_xy_z(nx, ny, nz, matrix, result);
		t.stop();
		mpiBarrier();
	}

	//cout << spmdpid << ": " << "after transpose" << endl;
	
	fout << "T_warm: " << t_warm.read() << endl;
	fout << "T_trans:";
       	for (int i=0; i<iters; i++) {
		fout << ' ' << t_trans[i].read();
	}
	fout << endl;

	fout.close();
	int rc = mpiFinalize();
	return rc;
}
