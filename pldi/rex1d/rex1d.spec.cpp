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
float * A;

void init() {
  A = new float[W*H];
  for (int i=0; i<W; ++i) {
    A[i] = i;
  }
}

void output(int height, int width, float * arr) {
  stringstream fname;
  fname << "rex1d.sout";
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
    cerr << "Usage: " << argv[0] << " <H> <W>" << endl;
    exit(1);
  }
  H = atoi(argv[1]);
  W = atoi(argv[2]);
  
  init();
  
  time(&start);
  spec(1, H, W, A);
  time(&end);
  output(H, W, A);
  return 0;
}
