#include "spmd.h"

#include <fstream>
#include <sstream>
#include <ctime>
#include <cstdlib>

using namespace std;

time_t start;
time_t end;
time_t totalEnd;

#include "rex1d.cpp"
using namespace ANONYMOUS;

int W, H;
LState * ls;

void init() {
  int base, width;
  partition(spmdnproc, spmdpid, W, base, width);
  ls = new LState(H, NULL, 0, width, base);
  for (int i=0; i<width; ++i) {
    ls->arr[1+i] = base+i;
  }
}

void output(int height, int width, float * arr) {
  stringstream fname;
  fname << "rex1d.output" << spmdpid;
  ofstream fout(fname.str().c_str());
  for (int x=0; x<width; ++x) {
    for (int t=0; t<height; ++t) {
      fout << arr[width*t+x] << " ";
    }
    fout << endl;
  }
  fout.close();
}

int main(int argc, char ** argv) {
  if (argc<3) {
    cerr << "Usage: mpirun -np <nproc> " << argv[0] << " <H> <W>" << endl;
    exit(1);
  }
  H = atoi(argv[1]);
  W = atoi(argv[2]);
  
  mpiInit(&argc, &argv);
  if (W < spmdnproc) {
    cerr << "W must not < nproc" << endl;
    exit(2);
  }

  init();
  
  time(&start);
  sk(spmdnproc, H, W, ls);
  time(&end);
  
  mpiBarrier();
  time(&totalEnd);
  mpiFinalize();
  output(ls->height, ls->width+2, ls->arr);
  return 0;
}
