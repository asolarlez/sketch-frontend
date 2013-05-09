#include "tr17.h"
namespace npb{
void transpose_xy_z(int nx, int ny, int nz, double* a/* len = ((nx * ny) * nz) / spmdnproc */, double* b/* len = ((nx * ny) * nz) / spmdnproc */) {
  timers[0].start();
#ifdef __dbg__
  cout << spmdpid << ": before local" << endl;
#endif
  timers[1].start();
  transpose_local(nx, ny, nz, a, b);
  timers[1].stop();
#ifdef __dbg__
  cout << spmdpid << ": after local" << endl;
#endif
  int  sz = nx * (ny/spmdnproc) * (nz/spmdnproc) ;
#ifdef __dbg__
  cout << spmdpid << ": before alltoall" << endl;
#endif
  timers[2].start();
  spmd::spmdalltoall(sz, b, a);
  timers[2].stop();
#ifdef __dbg__
  cout << spmdpid << ": after alltoall" << endl;
#endif
  timers[3].start();
  transpose_finish(nx, ny, nz, a, b);
  timers[3].stop();
#ifdef __dbg__
  cout << spmdpid << ": after finish" << endl;
#endif
  timers[0].stop();
}
inline void transpose_local(int nx, int ny, int nz, double* a/* len = ((nx * ny) * nz) / spmdnproc */, double* b/* len = ((nx * ny) * nz) / spmdnproc */) {
  int  iend_s30 = nz / spmdnproc;
  int  jend_s32 = (nx * ny) ;
  const int transblock = 32;
  const int transblockpad = 34;
  double z[transblock][transblockpad];
#ifdef __dbg__
  cout << spmdpid << ": nx,ny,nz,np,iend,jend=" << nx << ' ' << ny << ' ' << nz << ' ' << spmdnproc << ' ' << iend_s30 << ' ' << jend_s32 << endl;
#endif
  if (iend_s30%transblock != 0 || jend_s32%transblock != 0) {
    for (int  i=0;(i) < (iend_s30); i++) {
      for (int  j=0;(j) < (jend_s32); j++) {
        b[(j * iend_s30) + i] = a[(i * jend_s32) + j];
      }
    }
  } else {
    for (int i=0;(i) < (iend_s30); i+=transblock) {
      for (int j=0;(j) < (jend_s32); j+=transblock) {
        for (int ii=0; ii<transblock; ii++) {
          for (int jj=0; jj<transblock; jj++) {
            z[jj][ii] = a[(i+ii)*jend_s32 + j+jj];
	  }
	}
        for (int jj=0; jj<transblock; jj++) {
          for (int ii=0; ii<transblock; ii++) {
            b[(j+jj)*iend_s30 + i+ii] = z[jj][ii];
	  }
	}
      }
    }
  }
}
inline void transpose_finish(int nx, int ny, int nz, double* a/* len = ((nx * ny) * nz) / spmdnproc */, double* b/* len = ((nx * ny) * nz) / spmdnproc */) {
  int  iend_s14 = nz / spmdnproc;
  int  jend_s16 = nx * (ny / spmdnproc);
  int  _out_s24 = iend_s14 * jend_s16;
  for (int  p=0;(p) < (spmdnproc);p = p++){
    int ioff = p * iend_s14;
    for (int  j=0;(j) < (jend_s16);j = j++){
      for (int  i=0;(i) < (iend_s14);i = i++){
        b[(j * nz) + ioff + i] = a[(p * _out_s24) + (j * iend_s14) + i];
      }
    }
  }
}

}
namespace spmd{

inline void spmdalltoall(int sz, double* src/* len = sz * spmdnproc */, double* dst/* len = sz * spmdnproc */) { mpiAlltoall(sz, src, dst); }

}
