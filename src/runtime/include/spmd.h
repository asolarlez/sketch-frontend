#ifndef _SPMD_H
#define _SPMD_H 1

#include <mpi.h>

int spmdnproc;
int spmdpid;

void mpiInit(int * argc, char ***argv) {
  MPI_Init(argc, argv);
  MPI_Comm_size(MPI_COMM_WORLD, &spmdnproc);
  MPI_Comm_rank(MPI_COMM_WORLD, &spmdpid);
}

void mpiFinalize() {
  MPI_Finalize();
}

void mpiBarrier() {
  MPI_Barrier(MPI_COMM_WORLD);
}

void mpiTransfer(int size, float * src_buff, bool scond, int recipient, float * dest_buff, bool rcond) {
  static int epoch = 0;

  int tag = ++epoch;
  MPI_Status status;
  if (scond) {
    if (rcond) {
      MPI_Sendrecv(src_buff, size, MPI_FLOAT, recipient, tag, dest_buff, size, MPI_FLOAT, MPI_ANY_SOURCE, tag, MPI_COMM_WORLD, &status);
    } else {
      MPI_Send(src_buff, size, MPI_FLOAT, recipient, tag, MPI_COMM_WORLD);
    }
  } else {
    MPI_Recv(dest_buff, size, MPI_FLOAT, MPI_ANY_SOURCE, tag, MPI_COMM_WORLD, &status);
  }
}

#endif //_SPMD_H
