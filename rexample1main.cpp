#include <mpi.h>
#include <fstream>
#include <ctime>
#include <cstdlib>
using namespace std;

time_t start;
time_t end;
time_t totalEnd;

int spmdnproc;
int spmdpid;

#include "rexample1.cpp"
using namespace ANONYMOUS;

int W, H;
LState * ls;

void init() {
  int w = W/spmdnproc;
  int base = w*spmdpid;
  ls = new LState(H, NULL, 0, w);
  for (int i=0; i<w; ++i) {
    ls->arr[1+i] = base+i;
  }
}

int main(int argc, char ** argv) {
  if (argc<3) {
    cerr << "Usage: mpirun -np <nproc> " << argv[0] << " <H> <W>" << endl;
    exit(1);
  }
  H = atoi(argv[1]);
  W = atoi(argv[2]);
  
  MPI_Init(&argc, &argv);
  MPI_Comm_size(MPI_COMM_WORLD, &spmdnproc);
  MPI_Comm_rank(MPI_COMM_WORLD, &spmdpid);
  if (W%spmdnproc != 0) {
    cerr << "W must be multiple of nproc" << endl;
    exit(2);
  }

  init();
  
  time(&start);
  sk(spmdnproc, H, W, ls);
  time(&end);
  
  MPI_Barrier(MPI_COMM_WORLD);
  time(&totalEnd);
  MPI_Finalize();
}
