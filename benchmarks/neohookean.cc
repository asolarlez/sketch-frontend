#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <iostream>

#include "../../mathlib/mathlib.h"
#include "neohookean.h"

// 1 internal variable
summit::Neohookean::
Neohookean() : Material("neohookean",1) {}

summit::Neohookean::
Neohookean(const std::string& name) : Material(name,1) {}

summit::Neohookean::
Neohookean(const real rho, const real E, const real nu) : Material(rho, E, nu, 1)
{
    _lambda = _nu * _E / (1+_nu) / (1-2*_nu);
    _mu = _E / 2 / (1+_nu);
}

summit::Neohookean::
~Neohookean() {}

void
summit::Neohookean::
Load(FILE* fp)
{
    /* Read input */
    if (!fp) {
        std::cout<< "Error in Neohookean.cc : NeohookeanProperties, can't read material database"
                 <<std::endl;
        exit(1);
    }

    fscanf(fp,"%lf %lf %lf",&_rho,&_E,&_nu);

    // calculation of Lame coefficients
    _lambda = _nu * _E / (1+_nu) / (1-2*_nu);
    _mu = _E / 2 / (1+_nu);

    // Check material constants
    std::cout<< "Neo-Hookean material extended to the compressible range:" <<std::endl;

    std::cout<<"\tmass density ............ = "<< _rho <<std::endl;
    if (_rho < 0.e0) {
        std::cout<< "Error in Neohookean.cc : density must be a positive number\n" <<std::endl;
        exit(-1);
    }

    std::cout<<"\tYoung's modulus ....... = "<<_E <<std::endl;
    if (_E < 0.e0) {
        std::cout<< "Error in Neohookean.cc : Young's modulus must be positive" <<std::endl;
        exit(-1);
    }

    std::cout<< "\tPoisson's ratio ....... = "<< _nu <<std::endl;
    if (_nu < 0.e0) {
        std::cout<< "Error in Neohookean.cc : Poisson's ratio must be positive" <<std::endl;
        exit(-1);
    } else if (_nu > 0.5) {
        std::cout<< "Error in Neohookean.cc : Poisson's ratio must be less than 0.5\n" <<std::endl;
        exit(-1);
    }

    std::cout<< "\t1st Lame constant ........... = "<< _lambda <<std::endl;
    std::cout<< "\t2nd Lame constant ... = "<< _mu <<std::endl;

    // end of method
    return;
}

real
summit::Neohookean::
Celerity(const real* Fn, const real* q, const int ndm) const
{
    real C[9],detC,Cinv[9],p,coef,rhot;
    real F[]  = {1., 0., 0.,
          	 0., 1., 0.,
          	 0., 0., 1.};
    int i,ij,ij1,j;

    for (j=0,ij=0; j < ndm; j++)
      for(i=0,ij1=j*3; i < ndm; i++,ij++,ij1++)
        F[ij1] = Fn[ij];

    MathMat3Mults(F,F,C);
    detC = MathMat3Inv(C,Cinv);
    p = _lambda*0.5*log(detC);
    coef = _mu-p;
    rhot=_rho/sqrt(detC);

    // end of method
    return sqrt((_lambda+2*coef)/rhot);
}

void
summit::Neohookean::
Constitutive(const real* u0, const real* un, const real* F0, const real* Fn, real* P, real* q,
             real* Tangent, real dtime, const int ndf, const int ndm, bool compute_tangents) const
{
    int i,j,k,l,m,n,ij,jj,kl,ij1,ijkl;
    real coef,defVol,detC,p,trace;
    real F[]  = {1., 0., 0.,
          	 0., 1., 0.,
          	 0., 0., 1.};
//    real C[9],Cinv[9],S[9],M[81];
    real Cinv[9],S[9],M[81];

    // potentially move up in dimensionality
    for (j=0,ij=0; j < ndm; j++){
        for(i=0,ij1=j*3; i < ndm; i++,ij++,ij1++){
            F[ij1] = Fn[ij];
        }
    }


    // compute right Cauchy-Green tensor C
//    MathMat3Mults(F, F, C);

    // compute PK2 stresses and derivatives wrt C
//    detC = MathMat3Inv(C, Cinv);

    // Using Sketch-generated code
    sk_neohookean::inv_det_trace3(F, Cinv, detC, trace);

    if (detC < 1.e-10) {
        std::cout<< "Error in Neohookean.cc : neohookean constitutive, "
                 << "close to negative jacobian" <<std::endl;
        detC=1.e-10;
    }

    defVol = 0.5*log(detC);
    p = _lambda*defVol;

//    trace = C[0]+C[4]+C[8];

    q[0] = (0.5*p-_mu)*defVol+0.5*_mu*(trace-3.0);

    coef = p-_mu;

    for (j=0,ij=0,jj=0; j < 3; j++,jj+=4) {
        for (i=0; i < 3; i++,ij++){
            S[ij] = coef*Cinv[ij];
        }
        S[jj] += _mu;
    }

    if (compute_tangents) {
        coef = _mu-p;
        // not sure about row vs. column majorness so reimplementing in a simpler way
        /*    for (l=0,kl=0,ijkl=0; l < 3; l++)
          for (k=0,jk=0; k < 3; k++,kl++)
            for (j=0,ij=0,jl=l*3; j < 3; j++,jk++,jl++)
              for (i=0,ik=k*3,il=l*3; i < 3; i++,ij++,ik++,il++,ijkl++)
                M[ijkl] = _lambda*Cinv[ij]*Cinv[kl]
                +coef*(Cinv[ik]*Cinv[jl]+Cinv[il]*Cinv[jk]);*/
        for (i = 0, ijkl=0, ij=0; i < 3; ++i){
            for (j = 0; j < 3; ++j, ++ij) {
                for (k = 0, kl=0; k < 3; ++k){
                    for (l = 0; l < 3; ++l, ++ijkl, ++kl) {
                        M[ijkl] = _lambda*Cinv[ij]*Cinv[kl] + coef*(Cinv[3*i+k]*Cinv[3*j+l]
                                                            +  Cinv[3*i+l]*Cinv[3*j+k]);
                    }
                }
            }
        }
    } // end if compute_tangent


    // PK2 -> PK1, not sure if this is row or column major
    /*  for (j=0,ij=0; j < ndm; j++)
      for(i=0; i < ndm; i++,ij++) {
        P[ij] = 0.e0;
        for (k=0,ik=i,kj=j*3; k < 3; k++,ik+=3,kj++)
          P[ij] += F[ik]*S[kj];
          }*/
    for (i = 0, ij=0; i < ndm; ++i)
      for (j = 0; j < ndm; ++j, ++ij) {
        P[ij] = 0.0;
        for (k = 0; k < 3; ++k) {
          P[ij] += F[3*i+k]*S[3*k+j];
        }
      }

    if (compute_tangents) {
      // apply partial push-forward and add geometrical term
      /*
      for (l=0,ijkl=0; l < ndm; l++)
        for (k=0; k < ndf; k++)
          for (j=0,jl=l*3; j < ndm; j++,jl++)
            for (i=0; i < ndf; i++,ijkl++) {

              Tangent[ijkl] = 0.e0;
              // push-forward
              for (n=0,kn=k,nl=l*3; n < 3; n++,kn+=3,nl++) {
                indx = nl*9;
                for (m=0,im=i,mj=j*3; m < 3; m++,im+=3,mj++)
          	Tangent[ijkl] += F[im]*M[mj+indx]*F[kn];
              }

              // geometrical term
              if (i == k)
                Tangent[ijkl] += S[jl];
            }
      */
      for (i = 0, ijkl = 0; i < ndf; ++i)
        for (j = 0; j < ndm; ++j)
          for (k = 0; k < ndf; ++k)
            for (l = 0; l < ndm; ++l, ++ijkl) {
              Tangent[ijkl] = 0.0;

              // push-forward
              for (n = 0; n < 3; ++n)
                for (m = 0; m < 3; ++m)
                  Tangent[ijkl] += F[3*i+m]*M[9*(3*j+m)+(3*l+n)]*F[3*k+n];

              // geometrical term
              if (i == k)
                Tangent[ijkl] += S[3*j+l];
            }
    }// end of if compute_tangent
    // end of method
    return;
}

