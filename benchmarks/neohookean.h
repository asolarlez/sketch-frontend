#ifndef SUMMIT_NEOHOOKEAN_LAW_H
#define SUMMIT_NEOHOOKEAN_LAW_H

#include "../material.h"

namespace summit {

class Neohookean : public Material
{
  public:
    /**
     * Constructor (default)
     */
    Neohookean();

    /**
     * Constructor 
     * @param[in] name a string that defines the name of the material (is it really used?) 
     */
    Neohookean(const std::string& name);

    /**
     * Constructor
     * @param[in] rho a real which is the density
     * @param[in] E a real which is the Young's modulus
     * @param[in] nu a real which is the Poisson ratio
     */
    Neohookean(const real rho, const real E, const real nu);

    /**
     * Destructor
     */
    virtual ~Neohookean();
    
  private:
    /**
     * Copy Constructor.
     * Declared private not implemented
     */
    Neohookean(const Neohookean&);
    
    /**
     * Overloaded operator =.
     * Declared private and not implemented
     */
    Neohookean&
    operator=(const Neohookean&);

  public:
    /**
     * Method to load a material file
     * @param[in] material_file a file 
     */
    virtual void 
    Load(FILE* material_file);
    
    /**
     * Method to compute the critical wavespeed
     * @param[in] Fn a pointer of real
     * @param[in] q a pointer of real which is the internal variables
     * @param[in] ndm an integer which is the dimension (number of components in the strain tensor?)
     */
    virtual real 
    Celerity( const real* Fn, const real* q, const int ndm) const; 

    /**
     * Neohookean constitutive update, all tensors are passed in ROW MAJOR
     * @param u0 the interpolation of field u0 at quadrature point
     * @param u1 the interpolation of field u1 at quadrature point
     * @param Du0 the previous deformation gradient
     * @param Du1 the current deformation gradient
     * @param P the first Piola Kirchhoff stress
     * @param q the internal variables
     * @param tangent the tangent dP/dF
     * @param dt the timestep
     * @param ndf the degree of freedom per node
     * @param ndm the dimension of the computational domain
     * @param compute_tangents the flag for computing the tangent
     */
    virtual void 
    Constitutive(const real* u0, const real* u1, const real* Du0, const real* Du1, real* P,
                 real* q, real* tangent, real dt, const int ndf, const int ndm,
                 bool compute_tangents = false) const;

  private:
    /**
     * 1rst Lame coefficient 
     */
    real _lambda;

    /**
     * 2nd Lame coefficient 
     */
    real _mu;    

};

}

#endif
