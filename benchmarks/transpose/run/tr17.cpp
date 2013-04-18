#include "tr17.h"
namespace npb{
void transpose_xy_z(int nx, int ny, int nz, double* a/* len = ((nx * ny) * nz) / spmdnproc */, double* b/* len = ((nx * ny) * nz) / spmdnproc */) {
#ifdef __dbg__
  cout << spmdpid << ": before local" << endl;
#endif
  transpose_local(nx, ny, nz, a, b);
#ifdef __dbg__
  cout << spmdpid << ": after local" << endl;
#endif
  int  sz = nx * (ny/spmdnproc) * (nz/spmdnproc) ;
#ifdef __dbg__
  cout << spmdpid << ": before alltoall" << endl;
#endif
  spmd::spmdalltoall(sz, b, a);
#ifdef __dbg__
  cout << spmdpid << ": after alltoall" << endl;
#endif
  transpose_finish(nx, ny, nz, a, b);
#ifdef __dbg__
  cout << spmdpid << ": after finish" << endl;
#endif
}
void transpose_local(int nx, int ny, int nz, double* a/* len = ((nx * ny) * nz) / spmdnproc */, double* b/* len = ((nx * ny) * nz) / spmdnproc */) {
  int  iend_s30 = nz / spmdnproc;
  int  jend_s32 = (nx * ny) ;
#ifdef __dbg__
  cout << spmdpid << ": nx,ny,nz,np,iend,jend=" << nx << ' ' << ny << ' ' << nz << ' ' << spmdnproc << ' ' << iend_s30 << ' ' << jend_s32 << endl;
#endif
  for (int  i=0;(i) < (iend_s30);i = i + 1){
    for (int  j=0;(j) < (jend_s32);j = j + 1){
      int  _out_s36 = iend_s30;
      int  _out_s38 = jend_s32;
      b[i + (j * _out_s36)] = a[(i * _out_s38) + j];
    }
  }
}
void transpose_finish(int nx, int ny, int nz, double* a/* len = ((nx * ny) * nz) / spmdnproc */, double* b/* len = ((nx * ny) * nz) / spmdnproc */) {
  int  iend_s14 = nz / spmdnproc;
  int  jend_s16 = nx * (ny / spmdnproc);
  for (int  p=0;(p) < (spmdnproc);p = p + 1){
    for (int  j=0;(j) < (jend_s16);j = j + 1){
      for (int  i=0;(i) < (iend_s14);i = i + 1){
        int  _out_s18 = iend_s14;
        int  _out_s22 = nz;
        int  _out_s24 = nx * (ny/spmdnproc) * (nz/spmdnproc);
        int  _out_s28 = iend_s14;
// compiler should be able to take p*_out_s18 out
        b[((p * _out_s18) + i) + (j * _out_s22)] = a[((p * _out_s24) + i) + (j * _out_s28)];
      }
    }
  }
}

}
namespace spmd{

void spmdalltoall(int sz, double* src/* len = sz * spmdnproc */, double* dst/* len = sz * spmdnproc */) { mpiAlltoall(sz, src, dst); }

}
