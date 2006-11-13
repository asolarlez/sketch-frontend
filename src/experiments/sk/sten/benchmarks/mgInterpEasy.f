      program mgInterpEasy
      parameter (N=2)
      parameter (prtarr=1)

      parameter (f2=0.5, f4=0.25, f8=0.125)
      real in(0:N-1, 0:N-1, 0:N-1)
      real out(0:(2 * N)-1, 0:(2 * N)-1, 0:(2 * N)-1)
      real out2(0:(2 * N)-1, 0:(2 * N)-1, 0:(2 * N)-1)
      real out3(0:(2 * N)-1, 0:(2 * N)-1, 0:(2 * N)-1)
      integer clockrate, t0, t1
      integer k, k2

      call system_clock(count_rate=clockrate)

      do i_0 = 0, N-1
        do i_1 = 0, N-1
          do i_2 = 0, N-1
            in(i_0, i_1, i_2) = 1.0 + sin(real(k))
            k=mod(k*k+73,997)
          enddo
        enddo
      enddo

      print *, "input"
      if(prtarr .GT. 0) print *, in
      
      call system_clock(count=t0)
      call MGinter(in,f2,f4,f8,out,N)
      call system_clock(count=t1)
      print *, "spec"
      if(prtarr .GT. 0) print *, out
      print *, real(t1-t0)/(real(clockrate)/1000.0)

      call system_clock(count=t0)
      call skMGinter(in,f2,f4,f8,out2,N)
      call system_clock(count=t1)
      print *, "sketch"
      if(prtarr .GT. 0) print *, out2
      print *, real(t1-t0)/(real(clockrate)/1000.0)

      call system_clock(count=t0)
      call skSimpleMGinter(in,f2,f4,f8,out3,N)
      call system_clock(count=t1)
      print *, "simplesketch"
      if(prtarr .GT. 0) print *, out3
      print *, real(t1-t0)/(real(clockrate)/1000.0)
      
      do i_0 = 0, ((2 * N) - 1)
        do i_1 = 0, ((2 * N) - 1)
          do i_2 = 0, ((2 * N) - 1)
            if(out(i_0,i_1,i_2) .NE. out2(i_0,i_1,i_2)) then
              print *,"Found a difference in output!"
              print *,i_0,i_1,i_2,out(i_0,i_1,i_2),out2(i_0,i_1,i_2)
              stop
            endif
            if(out(i_0,i_1,i_2) .NE. out3(i_0,i_1,i_2)) then
              print *,"Found a difference in output (2)!"
              print *,i_0,i_1,i_2,out(i_0,i_1,i_2),out3(i_0,i_1,i_2)
              stop
            endif
          enddo
        enddo
      enddo
      end

      subroutine MGinter(in,half,fourth,eight,output,N)
      real in(0:N-1, 0:N-1, 0:N-1)
      real half
      real fourth
      real eight
      real output(0:(2 * N)-1, 0:(2 * N)-1, 0:(2 * N)-1)
      integer N
      do i_0 = 0, ((2 * N) - 1)
        do i_1 = 0, ((2 * N) - 1)
          do i_2 = 0, ((2 * N) - 1)
            output(i_0, i_1, i_2) = 0
          enddo
        enddo
      enddo
      do i = 0, (((2 * N) - 2) - 1)
        do j = 0, (((2 * N) - 2) - 1)
          do k = 0, (((2 * N) - 2) - 1)
            if ((((mod(i,2) .EQ. 0) .AND. (mod(j,2) .EQ. 0)) .AND. (mod(
     +k,2) .EQ. 0))) then
              output(i, j, k) = in((i / 2), (j / 2), (k / 2))
            endif
            if ((((mod(i,2) .EQ. 1) .AND. (mod(j,2) .EQ. 0)) .AND. (mod(
     +k,2) .EQ. 0))) then
              output(i, j, k) = (half * (in((i / 2), (j / 2), (k / 2)) +
     + in(((i / 2) + 1), (j / 2), (k / 2))))
            endif
            if ((((mod(i,2) .EQ. 0) .AND. (mod(j,2) .EQ. 1)) .AND. (mod(
     +k,2) .EQ. 0))) then
              output(i, j, k) = (half * (in((i / 2), (j / 2), (k / 2)) +
     + in((i / 2), ((j / 2) + 1), (k / 2))))
            endif
            if ((((mod(i,2) .EQ. 0) .AND. (mod(j,2) .EQ. 0)) .AND. (mod(
     +k,2) .EQ. 1))) then
              output(i, j, k) = (half * (in((i / 2), (j / 2), (k / 2)) +
     + in((i / 2), (j / 2), ((k / 2) + 1))))
            endif
            if ((((mod(i,2) .EQ. 1) .AND. (mod(j,2) .EQ. 1)) .AND. (mod(
     +k,2) .EQ. 0))) then
              output(i, j, k) = (fourth * (((in((i / 2), (j / 2), (k / 2
     +)) + in(((i / 2) + 1), ((j / 2) + 1), (k / 2))) + in((i / 2), ((j 
     +/ 2) + 1), (k / 2))) + in(((i / 2) + 1), (j / 2), (k / 2))))
            endif
            if ((((mod(i,2) .EQ. 0) .AND. (mod(j,2) .EQ. 1)) .AND. (mod(
     +k,2) .EQ. 1))) then
              output(i, j, k) = (fourth * (((in((i / 2), (j / 2), (k / 2
     +)) + in((i / 2), ((j / 2) + 1), ((k / 2) + 1))) + in((i / 2), ((j 
     +/ 2) + 1), (k / 2))) + in((i / 2), (j / 2), ((k / 2) + 1))))
            endif
            if ((((mod(i,2) .EQ. 1) .AND. (mod(j,2) .EQ. 0)) .AND. (mod(
     +k,2) .EQ. 1))) then
              output(i, j, k) = (fourth * (((in((i / 2), (j / 2), (k / 2
     +)) + in(((i / 2) + 1), (j / 2), ((k / 2) + 1))) + in(((i / 2) + 1)
     +, (j / 2), (k / 2))) + in((i / 2), (j / 2), ((k / 2) + 1))))
            endif
            if ((((mod(i,2) .EQ. 1) .AND. (mod(j,2) .EQ. 1)) .AND. (mod(
     +k,2) .EQ. 1))) then
              output(i, j, k) = (eight * (((((((in((i / 2), (j / 2), (k 
     +/ 2)) + in(((i / 2) + 1), (j / 2), (k / 2))) + in((i / 2), ((j / 2
     +) + 1), (k / 2))) + in((i / 2), (j / 2), ((k / 2) + 1))) + in(((i 
     +/ 2) + 1), ((j / 2) + 1), (k / 2))) + in((i / 2), ((j / 2) + 1), (
     +(k / 2) + 1))) + in(((i / 2) + 1), (j / 2), ((k / 2) + 1))) + in((
     +(i / 2) + 1), ((j / 2) + 1), ((k / 2) + 1))))
            endif
          enddo
        enddo
      enddo
      return
      end

      subroutine skMGinter(in,half,fourth,eight,output,N)
      real in(0:N-1, 0:N-1, 0:N-1)
      real half
      real fourth
      real eight
      real output(0:(2 * N)-1, 0:(2 * N)-1, 0:(2 * N)-1)
      integer N
      real z1(0:(N + 1)-1)
      real z2(0:(N + 1)-1)
      real z3(0:(N + 1)-1)
      real t1
      real t2
      real t3
      do i_0 = 0, ((2 * N) - 1)
        do i_1 = 0, ((2 * N) - 1)
          do i_2 = 0, ((2 * N) - 1)
            output(i_0, i_1, i_2) = 0
          enddo
        enddo
      enddo
      do i = 0, (((N - 2) + 1) - 1)
        do j = 0, (((N - 2) + 1) - 1)
          do i_0 = 0, ((N + 1) - 1)
            z1(i_0) = 0
          enddo
          do i_0 = 0, ((N + 1) - 1)
            z2(i_0) = 0
          enddo
          do i_0 = 0, ((N + 1) - 1)
            z3(i_0) = 0
          enddo
          do k = 0, (((N - 2) + 3) - 1)
            t1 = (in((k + 0), (j + 0), (i + 1)) + in((k + 0), (j + 0), (
     +i + 0)))
            t2 = (in((k + 0), (j + 0), (i + 0)) + in((k + 0), (j + 1), (
     +i + 0)))
            t3 = (in((k + 0), (j + 1), (i + 1)) + in((k + 0), (j + 1), (
     +i + 0)))
            t3 = (t3 + t1)
            z1(k) = t1
            z2(k) = t2
            z3(k) = t3
          enddo
          do k = 0, (((N - 2) + 1) - 1)
            output(((k * 2) + 0), ((j * 2) + 0), ((i * 2) + 0)) = in((k 
     ++ 0), (j + 0), (i + 0))
            output(((k * 2) + 1), ((j * 2) + 0), ((i * 2) + 0)) = (half 
     +* (in((k + 0), (j + 0), (i + 0)) + in((k + 1), (j + 0), (i + 0))))
          enddo
          do k = 0, (((N - 2) + 1) - 1)
            output(((k * 2) + 0), ((j * 2) + 0), ((i * 2) + 1)) = (half 
     +* z1((k + 0)))
            output(((k * 2) + 1), ((j * 2) + 0), ((i * 2) + 1)) = (fourt
     +h * (z1((k + 0)) + z1((k + 1))))
          enddo
          do k = 0, (((N - 2) + 1) - 1)
            output(((k * 2) + 0), ((j * 2) + 1), ((i * 2) + 0)) = (half 
     +* z2((k + 0)))
            output(((k * 2) + 1), ((j * 2) + 1), ((i * 2) + 0)) = (fourt
     +h * (z2((k + 1)) + z2((k + 0))))
          enddo
          do k = 0, (((N - 2) + 1) - 1)
            output(((k * 2) + 0), ((j * 2) + 1), ((i * 2) + 1)) = (fourt
     +h * z3((k + 0)))
            output(((k * 2) + 1), ((j * 2) + 1), ((i * 2) + 1)) = (eight
     + * (z3((k + 0)) + z3((k + 1))))
          enddo
        enddo
      enddo
      return
      end

      subroutine skSimpleMGinter(in,half,fourth,eight,output,N)
      real in(0:N-1, 0:N-1, 0:N-1)
      real half
      real fourth
      real eight
      real output(0:(2 * N)-1, 0:(2 * N)-1, 0:(2 * N)-1)
      integer N
      do i_0 = 0, ((2 * N) - 1)
        do i_1 = 0, ((2 * N) - 1)
          do i_2 = 0, ((2 * N) - 1)
            output(i_0, i_1, i_2) = 0
          enddo
        enddo
      enddo
      do i = 0, (((N - 2) + 1) - 1)
        do j = 0, (((N - 2) + 1) - 1)
          do k = 0, (((N - 2) + 1) - 1)
            output(((k * 2) + 0), ((j * 2) + 0), ((i * 2) + 0)) = in((k 
     ++ 0), (j + 0), (i + 0))
            output(((k * 2) + 0), ((j * 2) + 1), ((i * 2) + 0)) = (half 
     +* (in((k + 0), (j + 1), (i + 0)) + in((k + 0), (j + 0), (i + 0))))
            output(((k * 2) + 0), ((j * 2) + 0), ((i * 2) + 1)) = (half 
     +* (in((k + 0), (j + 0), (i + 0)) + in((k + 0), (j + 0), (i + 1))))
            output(((k * 2) + 1), ((j * 2) + 0), ((i * 2) + 0)) = (half 
     +* (in((k + 0), (j + 0), (i + 0)) + in((k + 1), (j + 0), (i + 0))))
            output(((k * 2) + 0), ((j * 2) + 1), ((i * 2) + 1)) = (fourt
     +h * (((in((k + 0), (j + 1), (i + 0)) + in((k + 0), (j + 0), (i + 0
     +))) + in((k + 0), (j + 0), (i + 1))) + in((k + 0), (j + 1), (i + 1
     +))))
            output(((k * 2) + 1), ((j * 2) + 0), ((i * 2) + 1)) = (fourt
     +h * (((in((k + 1), (j + 0), (i + 1)) + in((k + 0), (j + 0), (i + 0
     +))) + in((k + 0), (j + 0), (i + 1))) + in((k + 1), (j + 0), (i + 0
     +))))
            output(((k * 2) + 1), ((j * 2) + 1), ((i * 2) + 0)) = (fourt
     +h * (((in((k + 1), (j + 1), (i + 0)) + in((k + 0), (j + 0), (i + 0
     +))) + in((k + 1), (j + 0), (i + 0))) + in((k + 0), (j + 1), (i + 0
     +))))
            output(((k * 2) + 1), ((j * 2) + 1), ((i * 2) + 1)) = (eight
     + * (((((((in((k + 1), (j + 1), (i + 0)) + in((k + 0), (j + 1), (i 
     ++ 1))) + in((k + 0), (j + 0), (i + 1))) + in((k + 1), (j + 1), (i 
     ++ 1))) + in((k + 1), (j + 0), (i + 1))) + in((k + 1), (j + 0), (i 
     ++ 0))) + in((k + 0), (j + 0), (i + 0))) + in((k + 0), (j + 1), (i 
     ++ 0))))
          enddo
        enddo
      enddo
      return
      end
