#ifndef _SPMD_H
#define _SPMD_H

#ifndef SPMD_MAX_BUF_LEN
#define SPMD_MAX_BUF_LEN 4
#endif  //SPMD_MAX_BUF_LEN

struct spmdinfo {
  float [SPMD_MAX_BUF_LEN] buffer;
}

spmdinfo spmdinit(int nproc, int maxlen)
{
    assert(nproc*maxlen <= SPMD_MAX_BUF_LEN);
    spmdinfo info = new spmdinfo();
    return info;
}

void spmdtransfer(global spmdinfo info, global int size, float [size] src_buff, bit scond, int recipient, ref float [size] dest_buff, bit rcond)
{
  spmdbarrier();
  if (scond) {
    info.buffer[(recipient*size)::size] = src_buff;
  }
  spmdbarrier();
  if (rcond) {
    dest_buff = info.buffer[(spmdpid)*size::size];
  }
}

#endif  //_SPMD_H
