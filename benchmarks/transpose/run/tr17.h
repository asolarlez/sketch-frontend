#ifndef TR17_H
#define TR17_H

namespace npb{
extern void transpose_xy_z(int nx, int ny, int nz, double* a/* len = ((nx * ny) * nz) / spmdnproc */, double* b/* len = ((nx * ny) * nz) / spmdnproc */);
extern void transpose_local(int nx, int ny, int nz, double* a/* len = ((nx * ny) * nz) / spmdnproc */, double* b/* len = ((nx * ny) * nz) / spmdnproc */);
extern void transpose_finish(int nx, int ny, int nz, double* a/* len = ((nx * ny) * nz) / spmdnproc */, double* b/* len = ((nx * ny) * nz) / spmdnproc */);
}
namespace spmd{
extern void spmdalltoall(int sz, double* src/* len = sz * spmdnproc */, double* dst/* len = sz * spmdnproc */);
}

#endif
