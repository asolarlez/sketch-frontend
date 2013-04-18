      program ft

      implicit none

      include 'mpif.h'
      include 'global.h'
      integer i, ierr
      
c---------------------------------------------------------------------
c u0, u1, u2 are the main arrays in the problem. 
c Depending on the decomposition, these arrays will have different 
c dimensions. To accomodate all possibilities, we allocate them as 
c one-dimensional arrays and pass them to subroutines for different 
c views
c  - u0 contains the initial (transformed) initial condition
c  - u1 and u2 are working arrays
c---------------------------------------------------------------------

      double precision, allocatable :: u0(:), u1(:)
c---------------------------------------------------------------------
c Large arrays are in common so that they are allocated on the
c heap rather than the stack. This common block is not
c referenced directly anywhere else. Padding is to avoid accidental 
c cache problems, since all array sizes are powers of two.
c---------------------------------------------------------------------

      common /bigarrays/ u0, u1

      integer iter

      call MPI_Init(ierr)

c---------------------------------------------------------------------
c Run the entire problem once to make sure all data is touched. 
c This reduces variable startup costs, which is important for such a 
c short benchmark. The other NPB 2 implementations are similar. 
c---------------------------------------------------------------------
      T_warm = 1
      T_max = 1
      do i = 1, 1000
         T_max = T_max+1
         T_trans(i) = T_max
      end do

      call setup()
      call init(u0, u1)
      
      call MPI_Barrier(MPI_COMM_WORLD, ierr)

      call timer_start(T_warm)
      call transpose_xy_z(u0, u1)
      call timer_stop(T_init)

      if (me .eq. 0) then
      endif

      do iter = 1, niter
         call MPI_Barrier(MPI_COMM_WORLD, ierr)
         call timer_start(T_trans(iter))
         call transpose_xy_z(u0, u1)
         call timer_stop(T_trans(iter))
      end do

      call MPI_Finalize(ierr)

      call output_timers()

      close(fout)
      end


c---------------------------------------------------------------------
c---------------------------------------------------------------------

      subroutine init(u0, u1)
      implicit none
      include 'global.h'
      double precision, allocatable :: u0(:), u1(:)
      integer base, i

      ntdivnp = nx*ny*(nz/np)
      if (.not.allocated(u0)) allocate( u0(ntdivnp) )
      if (.not.allocated(u1)) allocate( u1(ntdivnp) )
      base = ntdivnp*me - 1
      do i = 1, ntdivnp
          u0(i) = base + i
      end do
      return
      end

      subroutine output_timers
      implicit none
      include 'global.h'
      double precision t

      write(fout, 1010) read_timer(T_warm)
1010  format('T_warm: ', F)

      write(fout, 'T_trans:')
      do i = 1, niter
          write(fout, '(F)', read_timer(T_trans(i))
      end do

      return
      end



      subroutine setup
      implicit none
      include 'mpinpb.h'
      include 'global.h'

      integer ierr, i, j, fstatus
      integer argc
      character(1024) arg
      character(1024) fname
      call MPI_Comm_size(MPI_COMM_WORLD, np, ierr)
      call MPI_Comm_rank(MPI_COMM_WORLD, me, ierr)

      dc_type = MPI_DOUBLE

      argc = iargc()
      if (argc .lt. 4) then
         call getarg(0, arg)
         write(*, 238) arg
 238     format('Usage: mpirun -np <nproc> ', A, ' nx ny nz [iters
[outputMatrix?]]')
         call MPI_Abort(MPI_COMM_WORLD, 1, ierr)
      endif

      call getarg(1, arg)
      read(arg, '(i10)', nx)
      call getarg(2, arg)
      read(arg, '(i10)', ny)
      call getarg(3, arg)
      read(arg, '(i10)', nz)
      if (argc > 4) then
         call getarg(4, arg)
         read(arg, '(i10)', niter)
      endif
      if (argc > 5) then
         call getarg(5, arg)
         read(arg, '(i10)', outputMatrix)
      endif

      if (.not. ((nx .gt. 0) .and. (ny .gt. 0) .and. (nz .gt. 0) 
        > .and. (mod(ny, np) .eq. 0) .and. (mod(nz, np) .eq. 0) )) then
         write(*, 239)
 239     format('wrong configuration nx, ny, nz, np!')
         call MPI_Abort(MPI_COMM_WORLD, 1, ierr)
      endif

      write(fname, 240) me
 240  format('out.ft', I10, '.txt')
      fout = 22
      open(unit=fout, file=fname, action='write', status='replace')

      return
      end

      

      subroutine transpose_xy_z(xin, xout)

      implicit none
      include 'global.h'
      integer n1, n2
      double precision xin(ntdivnp), xout(ntdivnp)

      n1 = nx * ny
      n2 = nz / np

      call transpose2_local(n1, n2, xin, xout)
      call transpose2_global(xout, xin)
      call transpose2_finish(n1, n2, xin, xout)

      return
      end



      subroutine transpose2_local(n1, n2, xin, xout)

      implicit none
      include 'mpinpb.h'
      include 'global.h'
      integer n1, n2
      double precision xin(n1, n2), xout(n2, n1)
      
      double precision z(transblockpad, transblock)

      integer i, j, ii, jj

c---------------------------------------------------------------------
c If possible, block the transpose for cache memory systems. 
c How much does this help? Example: R8000 Power Challenge (90 MHz)
c Blocked version decreases time spend in this routine 
c from 14 seconds to 5.2 seconds on 8 nodes class A.
c---------------------------------------------------------------------

      if (n1 .lt. transblock .or. n2 .lt. transblock) then
         if (n1 .ge. n2) then 
            do j = 1, n2
               do i = 1, n1
                  xout(j, i) = xin(i, j)
               end do
            end do
         else
            do i = 1, n1
               do j = 1, n2
                  xout(j, i) = xin(i, j)
               end do
            end do
         endif
      else
         do j = 0, n2-1, transblock
            do i = 0, n1-1, transblock
               
c---------------------------------------------------------------------
c Note: compiler should be able to take j+jj out of inner loop
c---------------------------------------------------------------------
               do jj = 1, transblock
                  do ii = 1, transblock
                     z(jj,ii) = xin(i+ii, j+jj)
                  end do
               end do
               
               do ii = 1, transblock
                  do jj = 1, transblock
                     xout(j+jj, i+ii) = z(jj,ii)
                  end do
               end do
               
            end do
         end do
      endif

      return
      end


c---------------------------------------------------------------------
c---------------------------------------------------------------------

      subroutine transpose2_global(xin, xout)

c---------------------------------------------------------------------
c---------------------------------------------------------------------

      implicit none
      include 'global.h'
      include 'mpinpb.h'
      double precision xin(ntdivnp)
      double precision xout(ntdivnp) 
      integer ierr

      call mpi_alltoall(xin, ntdivnp/np, dc_type,
     >                  xout, ntdivnp/np, dc_type,
     >                  MPI_COMM_WORLD, ierr)

      return
      end



c---------------------------------------------------------------------
c---------------------------------------------------------------------

      subroutine transpose2_finish(n1, n2, xin, xout)

c---------------------------------------------------------------------
c---------------------------------------------------------------------

      implicit none
      include 'global.h'
      integer n1, n2, ioff
      double precision xin(n2, n1/np, 0:np-1), xout(n2*np, n1/np)
      
      integer i, j, p

      do p = 0, np-1
         ioff = p*n2
         do j = 1, n1/np
            do i = 1, n2
               xout(i+ioff, j) = xin(i, j, p)
            end do
         end do
      end do

      return
      end

