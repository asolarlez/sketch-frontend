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
#define N_INT  100    			// Number of internal points in each direction
#define P 2 					// Processors in each direction

using namespace std;

void communicate(int left, int right, int top, int bottom, int n, double* buff_send,double* buff_recv, int N, double** u);
void red_black_GS(int N, double h, double **u);
void residual(int N, double h, double **u, double* local_res);
void boundary_conditions(int left, int right, int top, int bottom, double left_bound, double right_bound, double top_bound, double bottom_bound, int N, double **u );
void initialize(int N, double**u, double init_guess);

int main(int argc, char** argv)
{ 
  int i,j;
  int rank, size;

  MPI_Init(&argc,&argv);
  MPI_Comm_rank(MPI_COMM_WORLD, &rank);
  MPI_Comm_size(MPI_COMM_WORLD, &size);
  MPI_Status status;  

  double time,start_time=0.0;
  if (size < P*P) 
  {  cout << "Minimum processors required = " << P*P;
     return 0;
  }
   
  time = walltime(&start_time);
  int left,right,top,bottom; 		//Assign neighbors for each processor
  left   = (rank%P == 0)?MPI_PROC_NULL:rank-1;
  right  = ((rank+1)%P == 0)?MPI_PROC_NULL:rank+1;
  top    = (rank < P)?MPI_PROC_NULL:rank-P;
  bottom = (rank >= P*(P-1))?MPI_PROC_NULL:rank+P;
 
  int n=N_INT/P;                  	//Number of internal nodes per processor in each direction
  int N=n+2; 			  			//Number of nodes includeing ghost cells per processor in each direction
  
  double **u = new double*[N];      //Allocate blocks of matrix (along with ghost cells) to each processor 
  for (i=0; i < N; i++)
	u[i] = new double[N];

  double h = 1.0/(N_INT+1); 		//Spacing between nodes
  double res=0.0;
  double local_res=0.0;

  boundary_conditions(left,right,top,bottom,0.0,0.0,0.0,0.0,N,u);       //Apply boundary conditions
	
  initialize(N,u,1.0);  			//Apply initial guess to internal nodes

  double *buff_send = new double[n];//Allocate buffers for communication
  double *buff_recv = new double[n];
  int count = 0;					//Iteration count;
  do
  {     
        communicate(left, right, top, bottom, n, buff_send, buff_recv,N, u);    // Communication between ghost layer

        red_black_GS(N,h,u);           // Red Black Gauss Siedel Iteration 

        residual(N,h,u,&local_res);    // Calculate the residual on each processor

        MPI_Allreduce(&local_res,&res,1,MPI_DOUBLE,MPI_SUM,MPI_COMM_WORLD);    // Calculate residual for the whole system
        res=sqrt(res);

        count ++;	
  }
  while (res > 0.01 && count < 13000);

  MPI_Barrier(MPI_COMM_WORLD);
  time = walltime(&time);

  
  if (rank == 0 )
  {  
     cout << "Parallel ecexution time = " << time << endl; 
     cout << "Residual = " << res << endl;
     cout << "Number of iterations = " << count << endl;
  }
  
  // De-allocate memory
  delete [] buff_send;
  delete [] buff_recv;

  for(int i=0; i < N; i++)
     delete []  u[i]; 
  delete [] u; 
  MPI_Finalize();
  return 0;
}

void residual(int N, double h, double **u, double* local_res)
{  
  	*local_res =0.0;
  	//Calculate residual
  	for(int i=1; i<N-1; i++)
		for(int j=1; j<N-1; j++)
			*local_res +=  pow(((u[i+1][j]+u[i-1][j]+u[i][j+1]+u[i][j-1]-4*u[i][j])/(h*h) -1.0),2.0);

}

// TODO : Implement the following functions
void communicate (int left, int right, int top, int bottom, int n, double *buff_send, double *buff_recv, int N,double** u)
{  
  
}

void red_black_GS(int N, double h, double **u)
{

}

void boundary_conditions(int left, int right, int top, int bottom, 
                         double left_bound, double right_bound, double top_bound, double bottom_bound,
                         int N, double **u )
{

}

void initialize(int N, double**u, double init_guess)
{

}
