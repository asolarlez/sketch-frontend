/* This program will solve the following poisson's equation in 2D
       grad( div u) = 1.0
 Domain   : Square with side 1 unit
 Boundary condition : u = 0 on all boundary nodes
 Numerical Method : Red Black Gauss Siedel 
 Initial guess u = 1 on all internal nodes
*/


#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "walltime.h"
#define N_INT  100       			// Number of internal points in each direction

int main(int argc, char** argv)
{ int N = N_INT+2; 
  double u[N_INT+2][N_INT+2];				//Matrix representing the grid points
  double h = 1.0/(N+1);			//Spacing between points
  int i,j;
  double res=0.0;
  double time, start_time=0.0;

  time = walltime(&start_time);
  //Apply boundary conditions
  for (i=0; i < N ; i++)
	u[0][i] = 0.0;

  for (i=0; i < N ; i++)
	u[N-1][i] = 0.0;

  for (i=1; i < N -1; i++)
  {	
	u[i][0] = 0.0;	
	u[i][N-1] = 0.0;
  }
	
  //Apply initial guess
  for(i=1; i<N-1; i++)
	for(j=1; j<N-1; j++)
		u[i][j] = 1.0;
  

/*  	
  for(i=0; i<N; i++)
  { for(j=0; j<N; j++)
  		printf("%0.2f  ",u[i][j]);
    printf("\n");
  }
*/
  int count = 0;
  do
  {
  	//Pass for Red Nodes
  	for(i=1; i<N-1; i++)
		for(j=1+(i-1)%2; j<N-1; j=j+2)
			u[i][j] = (u[i+1][j]+u[i-1][j]+u[i][j+1]+u[i][j-1]-h*h)/4.0;      // 5 point stensil
		
	
  	//Pass for Black Nodes
  	for(i=1; i<N-1; i++)
		for(j=1+i%2; j<N-1; j=j+2)
			u[i][j] = (u[i+1][j]+u[i-1][j]+u[i][j+1]+u[i][j-1]-h*h)/4.0;      // 5 point stensil

  	res =0.0;
  	//Calculate residual
  	for(i=1; i<N-1; i++)
		for(j=1; j<N-1; j++)
			res +=  pow(((u[i+1][j]+u[i-1][j]+u[i][j+1]+u[i][j-1]-4*u[i][j])/(h*h) -1.0),2.0);
	res=sqrt(res);
        count++;
  }
  while (res > 0.01);
  time = walltime(&time);

  printf("Exe time = %f\n",time);
  printf("Residual = %f\n",res);
  printf("Number of iterations = %d\n",count);
/*    
  for(i=0; i<N; i++)
  { for(j=0; j<N; j++)
  		printf("%0.2f  ",u[i][j]);
    printf("\n");
  }
*/
  return 0;
}
