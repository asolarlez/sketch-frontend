#include "spmd.h"

#include <fstream>
#include <sstream>
#include <sys/time.h>
#include <cstdlib>

using namespace std;

struct timeval times[2][3];

#include "rex1d.cpp"
using namespace ANONYMOUS;

int W, H;
LState * ls = NULL;
bool outputMatrix = false;

void init() {
  int w;
  int b;
  partition(spmdnproc, spmdpid, W, b, w);
  int g = (H+1)/2;
 
  if (ls != NULL) delete ls; 
  ls = new LState(g, H, NULL, 0, w, b);
//  cout << "g=" << g << " glen=" << ls->glen << endl;
  for (int i=0; i<w; ++i) {
    ls->arr[g+i] = b+i;
  }
}

string transT(struct timeval const & t) {
  stringstream s;
  s << t.tv_sec << "," << t.tv_usec;
  return s.str();
}

void output(int height, int width, float * arr) {
  stringstream fname;
  fname << "rex1d.output" << spmdpid;
  ofstream fout(fname.str().c_str());
  fout << "times0: " << transT(times[0][0]) << " " << transT(times[0][1]) << " " << transT(times[0][2]) << endl;
  fout << "times1: " << transT(times[1][0]) << " " << transT(times[1][1]) << " " << transT(times[1][2]) << endl;
  if (outputMatrix) {
    for (int x=0; x<width; ++x) {
      for (int t=0; t<height; ++t) {
        fout << arr[width*t+x] << " ";
      }
      fout << endl;
    }
  }
  fout.close();
}

void moveRow(LState & s) {
  int h = s.height;
  int w = s.width;
  for (int i=0; i<w; ++i) {
    int x = s.glen + i;
    s.arr[x] = s.arr[(h-1)*(s.width+s.glen*2) + x];
  }
}

void doSk(int iters, struct timeval * t) {
  init();
  mpiBarrier();
  gettimeofday(&t[0], NULL);
  for (int i=0; i<iters; ++i) {
    if (i>0) { moveRow(*ls); }
    sk(spmdnproc, H, W, ls);
  }
  gettimeofday(&t[1], NULL);

  mpiBarrier();
  gettimeofday(&t[2], NULL);
}

int main(int argc, char ** argv) {
  if (argc<3) {
    cerr << "Usage: mpirun -np <nproc> " << argv[0] << " <H> <W> [iters] [outputMatrix?]" << endl;
    exit(1);
  }
  H = atoi(argv[1]);
  W = atoi(argv[2]);
  int iters = 1;
  if (argc>3) {
    iters = atoi(argv[3]);
  }
  if (argc>4) {
    outputMatrix = (bool)atoi(argv[4]);
  }
  
  mpiInit(&argc, &argv);
  if (W < spmdnproc) {
    cerr << "W must not < nproc" << endl;
    exit(2);
  }

  doSk(iters, times[0]);
  doSk(iters, times[1]);

  mpiFinalize();
  output(ls->height, ls->width+ls->glen*2, ls->arr);
  return 0;
}
