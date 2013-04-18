      common /procgrid/ np


      integer transblock, transblockpad
      parameter(transblock=32, transblockpad=34)
      
c we need a bunch of logic to keep track of how
c arrays are laid out. 
c coords of this processor
      integer me
      common /coords/ me


c There are basically three stages
c 1: x-y-z layout
c 2: after x-transform (before y)
c 3: after y-transform (before z)
c The computation proceeds logically as

c set up initial conditions
c fftx(1)
c transpose (1->2)
c ffty(2)
c transpose (2->3)
c fftz(3)
c time evolution
c fftz(3)
c transpose (3->2)
c ffty(2)
c transpose (2->1)
c fftx(1)
c compute residual(1)

c for the 0D, 1D, 2D strategies, the layouts look like xxx
c        
c            0D        1D        2D
c 1:        xyz       xyz       xyz
c 2:        xyz       xyz       yxz
c 3:        xyz       zyx       zxy

c the array dimensions are stored in dims(coord, phase)
      integer nx, ny, nz
      common /layout/ nx, ny, nz

      integer T_warm
      integer T_trans(1000)
      integer T_max

      common /timers/ T_warm, T_trans, T_max

      external timer_read
      double precision timer_read

      integer niter, outputMatrix, fout
      common /params/ niter, outputMatrix, fout
