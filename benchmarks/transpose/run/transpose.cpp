#include "spmd.h"
using namespace spmd;

#include <cstdlib>
#include <fstream>
#include <iomanip>
#include <sstream>
using std::string;
using std::cerr;
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
	    cerr << "Usage: mpirun -np <nproc> " << argv[0] << " nx ny nz [iters [outputMatrix?]]" << endl;
	    exit(1);
	}
	
	int nx = atoi(argv[1]);
	int ny = atoi(argv[2]);
	int nz = atoi(argv[3]);
	int iters = 1;
	bool outputMatrix = false;
	if (argc > 4) {
		iters = atoi(argv[4]);
	}
	if (argc > 5) {
		outputMatrix = (bool)atoi(argv[5]);
	}
	if (!(nx>0 && ny>0 && nz>0 && nz%spmdnproc==0 && ny%spmdnproc==0)) {
		cerr << "Wrong configuration nx, ny, nz!" << endl;
		exit(2);
	}

	stringstream pidss;
	pidss << spmdpid;
	string const pidstr(pidss.str());
	stringstream fname;
	fname << "output" << pidstr << ".txt";

	ofstream fout(fname.str().c_str());

	double * matrix;
	int ntdivnp = init(nx, ny, nz, &matrix);
	double * result = new double[ntdivnp];

	fout << "initial matrix:" << endl;
	output(fout, nx, ny, nz/spmdnproc, matrix);
	fout.flush();

	//cout << spmdpid << ": " << "after init" << endl;

	mpiBarrier();
	transpose_xy_z(nx, ny, nz, matrix, result);
	mpiBarrier();

	//cout << spmdpid << ": " << "after transpose" << endl;

	fout << "after transpose_xy_z:" << endl;
	output(fout, nz, nx, ny/spmdnproc, result);

	fout.close();
	int rc = mpiFinalize();
	return rc;
}

