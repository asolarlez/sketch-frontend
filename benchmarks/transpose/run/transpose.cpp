//#define __dbg__ 1
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

timer * timers = NULL;

inline void output_timers(ofstream & fout, string const & name, int n, timer * timers) {
	fout << name << ':' << endl;
	for (int i=0; i<n; i++) {
		fout << ' ' << timers[i].read() << endl;
	}
}

#include "tr17.cpp"
using namespace npb;

inline void init_matrix(int size, double * matrix) {
	double base = double(size) * spmdpid;
	for (int i=0; i<size; i++) {
		matrix[i] = base + i;
	}
}

inline void check_result(ofstream & fout, int nx, int ny, int nz, double * matrix) {
	int nydivnp = ny / spmdnproc;
	int x = 0;
	int y = spmdpid*nydivnp;
	int z = 0;
	for (int i=0; i<nz*nx*nydivnp; i++, matrix++) {
		double num = *matrix;
		double correct = double(z)*ny*nx + y*nx + x;
		double diff = num - correct;
		if (diff < 0.0) {
			diff = -diff;
		}
		if (diff > 1e-5) {
			stringstream out;
			out << "Wrong result! x=" << x << " y=" << y << " z=" << z << " num=" << num << " correct=" << correct;
			cout << spmdpid << ": " << out.str() << endl;
			fout << out << endl;
			fout.close();
			MPI_Finalize();
			exit(555);
		}
		z++;
		if (z >= nz) {
			z = 0;
			x++;
		}
		if (x >= nx) {
			x = 0;
			y++;
		}
	}
}

int init(int nx, int ny, int nz, double ** matrix) {
	int ntdivnp = nx*ny*(nz/spmdnproc);
#ifdef __dbg__
	cout << spmdpid << ": before new" << endl;
#endif
	double * a = new double[ntdivnp + 3];
#ifdef __dbg__
	cout << spmdpid << ": after new ntdivnp=" << ntdivnp << endl;
#endif
	init_matrix(ntdivnp, a);
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
	double * result = new double[ntdivnp + 3];

	if (outputMatrix) {
		fout << "initial matrix:" << endl;
		output(fout, nx, ny, nz/spmdnproc, matrix);
	}

	//cout << spmdpid << ": " << "after init" << endl;

	timer t_warm[4], t_trans[4];

	// warmup
	timers = t_warm;
	mpiBarrier();
#ifdef __dbg__
	cout << spmdpid << ": before warm" << endl;
#endif
	transpose_xy_z(nx, ny, nz, matrix, result);
	check_result(fout, nx, ny, nz, result);
#ifdef __dbg__
	cout << spmdpid << ": after warm" << endl;
#endif
	
	if (outputMatrix) {
		fout << "after transpose_xy_z:" << endl;
		output(fout, nz, nx, ny/spmdnproc, result);
	}

	timers = t_trans;
	for (int i=0; i<iters; i++) {
#ifdef __dbg__
		cout << spmdpid << ": iter=" << i << endl;
#endif
		init_matrix(ntdivnp, matrix);
		mpiBarrier();
		transpose_xy_z(nx, ny, nz, matrix, result);
		mpiBarrier();
		check_result(fout, nx, ny, nz, result);
#ifdef __dbg__
		cout << spmdpid << ": after iter=" << i << endl;
#endif
	}

	//cout << spmdpid << ": " << "after transpose" << endl;
	output_timers(fout, "T_warm", 4, t_warm);	
	output_timers(fout, "T_trans", 4, t_trans);	

	fout.close();
	int rc = mpiFinalize();
	return rc;
}

