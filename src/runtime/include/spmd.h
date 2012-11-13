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

/*
int SPMD_MAX = 1;
int SPMD_MIN = 2;
int SPMD_SUM = 3;
int SPMD_PROD = 4;
*/
static MPI_Op const OP[] = {
  0,
  MPI_MAX,
  MPI_MIN,
  MPI_SUM,
  MPI_PROD
};

void mpiReduce(int op, int size, float * sendbuf, float * recvbuf) {
	MPI_Allreduce(sendbuf, recvbuf, size, MPI_FLOAT, OP[op], MPI_COMM_WORLD);
}

void mpiTransfer(int size, bool scond, float * sendbuf, int recipient, bool rcond, float * recvbuf) {
  static int epoch = 0;

  int tag = ++epoch;
  MPI_Status status;
  if (scond) {
    if (rcond) {
      MPI_Sendrecv(sendbuf, size, MPI_FLOAT, recipient, tag, recvbuf, size, MPI_FLOAT, MPI_ANY_SOURCE, tag, MPI_COMM_WORLD, &status);
    } else {
      MPI_Send(sendbuf, size, MPI_FLOAT, recipient, tag, MPI_COMM_WORLD);
    }
  } else {
    MPI_Recv(recvbuf, size, MPI_FLOAT, MPI_ANY_SOURCE, tag, MPI_COMM_WORLD, &status);
  }
}

#endif //_SPMD_H
