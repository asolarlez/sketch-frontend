#ifndef TR17_H
#define TR17_H

#include <cstring>

#include "vops.h"

namespace npb{
  class LState;
}
namespace spmd{
}
namespace npb{
class LState; 
class LState{
  public:
  int  ntdivnp;
  double*  a;
  LState(){}
  LState(  int  ntdivnp_,   double*  a_, int a_len){
    ntdivnp =  ntdivnp_;
    a = new double [ntdivnp_];
    CopyArr(a, a_, ntdivnp_, a_len ); 
  }
  ~LState(){
    delete[] a;
  }
};
extern void sk(int P, int nx, int ny, int nz, LState* a, LState*& result);
extern void spec_xy_z(int P, int nx, int ny, int nz, double* a/* len = (nx * ny) * nz */, double* result/* len = (nx * ny) * nz */);
extern void movein(int nproc, int pid, int nt, double* ga/* len = nt */, LState*& _out);
extern void transpose_xy_z(int nx, int ny, int nz, double* a/* len = ((nx * ny) * nz) / spmdnproc */, double* b/* len = ((nx * ny) * nz) / spmdnproc */);
extern void moveout(int nproc, int pid, int nt, double* ga/* len = nt */, LState* ls);
extern void transpose_local(int nx, int ny, int nz, double* a/* len = ((nx * ny) * nz) / spmdnproc */, double* b/* len = ((nx * ny) * nz) / spmdnproc */);
extern void transpose_finish(int nx, int ny, int nz, double* a/* len = ((nx * ny) * nz) / spmdnproc */, double* b/* len = ((nx * ny) * nz) / spmdnproc */);
}
namespace spmd{
extern void spmdalltoall(int sz, double* src/* len = sz * spmdnproc */, double* dst/* len = sz * spmdnproc */);
}

#endif
