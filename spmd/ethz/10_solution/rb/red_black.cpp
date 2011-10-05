/* This program will solve the following poisson's equation in 2D
   grad( div u) = 1.0
   Domain   : Square with side 1 unit
   Boundary condition : u = 0 on all boundary nodes
   Numerical Method : Red Black Gauss Siedel
   Initial guess u = 1 on all internal nodes
*/

#include <iostream>
#include <stdlib.h>
#include <math.h>
#include "walltime.h"
#include <mpi.h>
#define N_INT  100   // Number of internal points in each direction
#define P 2          // Processors in each direction

using namespace std;

/*
 * Prototypes
 */
void communicate(int left, int right, int top, int bottom,
                 int n, double* buff_send,double* buff_recv, int N, double** u);
void red_black_GS(int N, double h, double **u);
void residual(int N, double h, double **u, double* local_res);
void boundary_conditions(int left, int right, int top, int bottom,
                         double left_bound, double right_bound, double top_bound,
                         double  bottom_bound, int N, double **u );
void initialize(int N, double**u, double init_guess);

/**
 * In the main function:
 */
int main(int argc, char** argv)
{
  int i,j;
  int rank, size;

  MPI_Init(&argc,&argv);
  MPI_Comm_rank(MPI_COMM_WORLD, &rank);
  MPI_Comm_size(MPI_COMM_WORLD, &size);
  MPI_Status status;

  double time,start_time=0.0;

  /**
   * Check nb of processors
   */
  if (size < P*P)
    {
      cout << "Minimum processors required = " << P*P;
      MPI_Finalize();
      return 1;
    }

  time = walltime(&start_time);

  /**
   * Assign neighbors for each processor
   */
  int left,right,top,bottom;
  left   = (rank % P == 0) ? MPI_PROC_NULL : rank - 1;
  right  = ((rank+1)%P == 0) ? MPI_PROC_NULL:rank + 1;
  top    = (rank < P) ? MPI_PROC_NULL : rank - P;
  bottom = (rank >= P * (P - 1)) ? MPI_PROC_NULL : rank + P;

  int n = N_INT / P; // Number of internal nodes per processor in each direction
  int N = n + 2;     // Number of nodes including ghost cells per processor in each direction

  // Allocate blocks of matrix (along with ghost cells) to each processor
  double **u = new double*[N];
  for (i=0; i < N; i++) {
    u[i] = new double[N];
  }

  double h = 1.0 / (N_INT + 1); //Spacing between nodes
  double res = 0.0;
  double local_res = 0.0;

  //Apply boundary conditions
  boundary_conditions(left, right, top, bottom,
                      0.0, 0.0, 0.0, 0.0, N, u);

  //Apply initial guess to internal nodes
  initialize(N, u, 1.0);

  //Allocate buffers for communication
  double *buff_send = new double[n];
  double *buff_recv = new double[n];

  int count = 0; //Iteration count;

  do {
    // Communication between ghost cells
    communicate(left, right, top, bottom, n, buff_send, buff_recv,N, u);

    // Red Black Gauss Siedel Iteration
    red_black_GS(N,h,u);

    // Calculate the residual on each processor
    residual(N, h, u, &local_res);

    // Calculate residual for the whole system
    MPI_Allreduce(&local_res, &res, 1, MPI_DOUBLE, MPI_SUM, MPI_COMM_WORLD);
    res=sqrt(res);

    if(rank == 0 && count %100 == 0)
	std::cout << "Iteration: " << count << std::endl;

    count++;
  }
  while (res > 0.01);

  MPI_Barrier(MPI_COMM_WORLD);
  time = walltime(&time);


  if (rank == 0)
    {
      cout << "Parallel ecexution time = " << time << endl;
      cout << "Residual = " << res << endl;
      cout << "Number of iterations = " << count << endl;
    }


  // De-allocate memory
  delete [] buff_send;
  delete [] buff_recv;

  for(int i=0; i < N; i++) {
    delete []  u[i];
  }
  delete [] u;

  MPI_Finalize();

  return 0;
}

void communicate (int left, int right, int top, int bottom,
                  int n, double *buff_send, double *buff_recv,
                  int N,double** u) {

  MPI_Status status;

  // Send to top, receive from bottom
  MPI_Sendrecv(&u[1][1],n,MPI_DOUBLE,top,0,
               &u[N-1][1],n,MPI_DOUBLE,bottom,0,
               MPI_COMM_WORLD,&status);

  // Send to left, receive from right
  for(int i=0;i<n;i++)
    buff_send[i] = u[1+i][1];

  MPI_Sendrecv(buff_send,n,MPI_DOUBLE,left,0,
               buff_recv,n,MPI_DOUBLE,right,0,
               MPI_COMM_WORLD,&status);

  if(right != MPI_PROC_NULL)
    for(int i=0;i<n;i++)
      u[1+i][N-1] = buff_recv[i];

  // Send to bottom, receive from top
  MPI_Sendrecv(&u[N-2][1],n,MPI_DOUBLE,bottom,0,
               &u[0][1],n,MPI_DOUBLE,top,0,
               MPI_COMM_WORLD,&status);

  // Send to right, receive from left
  for(int i=0; i<n;i++)
    buff_send[i] = u[1+i][N-2];

  MPI_Sendrecv(buff_send,n,MPI_DOUBLE,right,0,
               buff_recv,n,MPI_DOUBLE,left,0,
               MPI_COMM_WORLD,&status);

  if(left != MPI_PROC_NULL)
    for(int i=0;i<n;i++)
      u[1+i][0] = buff_recv[i];

}

/**
 * One Red-Black-Gauss-Seidel iteration through the cartesian grid.
 * The functions assumes a layer of 1 ghost-layer nodes.
 */
void red_black_GS(int N, double h, double **u)
{
  //Pass for Red Nodes (5 point stencil)
  for(int i = 1; i < N - 1; i++) {
    for(int j = 1 + (i - 1) % 2; j < N - 1; j = j + 2) {
      u[i][j] = (u[i+1][j] + u[i-1][j] + u[i][j+1] + u[i][j-1] - h * h) / 4.0;
    }
  }

  //Pass for Black Nodes (5 point stencil)
  for(int i = 1; i <  N - 1; i++) {
    for(int j = 1 + i % 2 ; j < N - 1; j = j + 2) {
      u[i][j] = (u[i+1][j] + u[i-1][j] + u[i][j+1] + u[i][j-1] - h * h) / 4.0;
    }
  }
}


/**
 * Calculates the residual: Adds up the difference of the
 * finite differences and the function value in the poisson eq.
 */
void residual(int N, double h, double **u, double* local_res)
{
  *local_res = 0.0;

  for(int i=1; i < N-1; i++) {
    for(int j=1; j < N-1; j++) {
      *local_res +=  pow(((u[i+1][j] + u[i-1][j] + u[i][j+1] + u[i][j-1] - 4*u[i][j])
                          / (h * h) - 1.0), 2.0);
    }
  }
}


void boundary_conditions(int left, int right, int top, int bottom,
                         double left_bound, double right_bound,
                         double top_bound, double bottom_bound,
                         int N, double **u )
{
  if(top == MPI_PROC_NULL)
    for (int i=0; i < N ; i++)
      u[0][i] = top_bound;

  if(bottom == MPI_PROC_NULL)
    for (int i=0; i < N ; i++)
      u[N-1][i] = bottom_bound;

  if(left == MPI_PROC_NULL)
    for (int i=0; i < N ; i++)
      u[i][0] = left_bound;

  if(right == MPI_PROC_NULL)
    for (int i=0; i < N ; i++)
      u[i][N-1] = right_bound;
}

void initialize(int N, double**u, double init_guess)
{
  for(int i=1; i<N-1; i++)
    for(int j=1; j<N-1; j++)
      u[i][j] = init_guess;
}
