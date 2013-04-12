#include <cstdio>
#include <assert.h>
#include "vops.h"
#include "tr17.h"
namespace npb{

void sk(int P, int nx, int ny, int nz, LState* a, LState*& result) {
  int  nt=0;
  nt = (nx * ny) * nz;
  {
    LState*  la_s10=NULL;
    la_s10 = a;
    LState*  lr_s12=NULL;
    lr_s12 = result;
    double*  a_s179= new double [((nx * ny) * nz) / spmdnproc]; CopyArr<double >(a_s179,la_s10->a, ((nx * ny) * nz) / spmdnproc, la_s10->ntdivnp);
    double*  b_s180= new double [((nx * ny) * nz) / spmdnproc]; CopyArr<double >(b_s180,lr_s12->a, ((nx * ny) * nz) / spmdnproc, lr_s12->ntdivnp);
    transpose_xy_z(nx, ny, nz, a_s179, b_s180);
    CopyArr<double >(la_s10->a,a_s179, la_s10->ntdivnp, ((nx * ny) * nz) / spmdnproc);
    CopyArr<double >(lr_s12->a,b_s180, lr_s12->ntdivnp, ((nx * ny) * nz) / spmdnproc);
    result = lr_s12;
    delete[] a_s179;
    delete[] b_s180;
  };
}
void spec_xy_z(int P, int nx, int ny, int nz, double* a/* len = (nx * ny) * nz */, double* result/* len = (nx * ny) * nz */) {
  // assume ((P) > (0));
  // assume ((P) <= (2));
  // assume (((ny % P)) == (0));
  // assume (((nz % P)) == (0));
  // assume (((nx) >= (1)) && ((nx) <= (16)));
  // assume (((ny) >= (1)) && ((ny) <= (16)));
  // assume (((nz) >= (1)) && ((nz) <= (16)));
  // assume ((((nx * ny) * nz)) <= (32));
  // assume (((((nx * ny) * nz) / P)) <= (16));
  for (int  x=0;(x) < (nx);x = x + 1){
    for (int  y=0;(y) < (ny);y = y + 1){
      for (int  z=0;(z) < (nz);z = z + 1){
        (result[((y * (nx * nz)) + (x * nz)) + z]) = (a[((z * (nx * ny)) + (y * nx)) + x]);
      }
    }
  }
}
void movein(int nproc, int pid, int nt, double* ga/* len = nt */, LState*& _out) {
  _out = NULL;
  int  ntdivnp=0;
  ntdivnp = nt / nproc;
  double*  a= new double [ntdivnp]; CopyArr<double >(a,0.0f, ntdivnp);
  int  base=0;
  base = ntdivnp * pid;
  for (int  i=0;(i) < (ntdivnp);i = i + 1){
    (a[i]) = (ga[base + i]);
  }
  _out = new LState(ntdivnp, a, ntdivnp);
  delete[] a;
  return;
}
void transpose_xy_z(int nx, int ny, int nz, double* a/* len = ((nx * ny) * nz) / spmdnproc */, double* b/* len = ((nx * ny) * nz) / spmdnproc */) {
  int  n=0;
  n = ((nx * ny) * nz) / spmdnproc;
  transpose_local(nx, ny, nz, a, b);
  int  sz=0;
  sz = n / spmdnproc;
  spmd::spmdalltoall(sz, b, a);
  transpose_finish(nx, ny, nz, a, b);
}
void moveout(int nproc, int pid, int nt, double* ga/* len = nt */, LState* ls) {
  int  base=0;
  base = ls->ntdivnp * pid;
  int  i=0;
  bool  __sa0=(0) < (ls->ntdivnp);
  while (__sa0) {
    (ga[base + i]) = (ls->a[i]);
    i = i + 1;
    __sa0 = (i) < (ls->ntdivnp);
  }
}
void transpose_local(int nx, int ny, int nz, double* a/* len = ((nx * ny) * nz) / spmdnproc */, double* b/* len = ((nx * ny) * nz) / spmdnproc */) {
  int  iend_s30=0;
  iend_s30 = nz / spmdnproc;
  int  jend_s32=0;
  jend_s32 = (nx * ny) / 1;
  for (int  i=0;(i) < (iend_s30);i = i + 1){
    for (int  j=0;(j) < (jend_s32);j = j + 1){
      int  _out_s36=0;
      _out_s36 = nz / spmdnproc;
      int  _out_s38=0;
      _out_s38 = (nx * ny) / 1;
      (b[i + (j * _out_s36)]) = (a[(i * _out_s38) + j]);
    }
  }
}
void transpose_finish(int nx, int ny, int nz, double* a/* len = ((nx * ny) * nz) / spmdnproc */, double* b/* len = ((nx * ny) * nz) / spmdnproc */) {
  int  iend_s14=0;
  iend_s14 = nz / spmdnproc;
  int  jend_s16=0;
  jend_s16 = (nx * ny) / spmdnproc;
  for (int  p=0;(p) < (spmdnproc);p = p + 1){
    for (int  i=0;(i) < (iend_s14);i = i + 1){
      for (int  j=0;(j) < (jend_s16);j = j + 1){
        int  _out_s18=0;
        _out_s18 = nz / spmdnproc;
        int  _out_s22=0;
        _out_s22 = nz / 1;
        int  _out_s24=0;
        _out_s24 = ((nx * ny) * nz) / (spmdnproc * spmdnproc);
        int  _out_s28=0;
        _out_s28 = nz / spmdnproc;
        (b[((p * _out_s18) + i) + (j * _out_s22)]) = (a[((p * _out_s24) + i) + (j * _out_s28)]);
      }
    }
  }
}

}
namespace spmd{

void spmdalltoall(int sz, double* src/* len = sz * spmdnproc */, double* dst/* len = sz * spmdnproc */) { mpiAlltoall(sz, src, dst); }

}
