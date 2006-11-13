      program rb2dEasypp
      parameter (N=1000)
      parameter (prtarr=0)

      parameter (ce=0.5, no=0.1, so=0.2, ea=0.3, we=0.4)
      real in(0:N-1, 0:N-1)
      real f(0:N-1, 0:N-1)
      real out(0:N-1, 0:N-1)
      real out2(0:N-1, 0:N-1)
      integer clockrate, t0, t1
      integer k

      call system_clock(count_rate=clockrate)

      do i_1 = 0, N-1
        do i_0 = 0, N-1
          in(i_0, i_1) = 1.0 + sin(real(k))
          k=mod(k*k+73,997)
          f(i_0, i_1) = 1.0 + sin(real(k))
          k=mod(k*k+73,997)
        enddo
      enddo

      if(prtarr .GT. 0) print *, "input"
      if(prtarr .GT. 0) print *, in
      if(prtarr .GT. 0) print *, f
      
      call system_clock(count=t0)
      call rbGaussSeidel(in,f,ce,no,so,ea,we,out,N)
      call system_clock(count=t1)
      print *, "spec"
      if(prtarr .GT. 0) print *, out
      print *, real(t1-t0)/(real(clockrate)/1000.0)

      call system_clock(count=t0)
      call rbGaussSeidelSK(in,f,ce,no,so,ea,we,out2,N)
      call system_clock(count=t1)
      print *, "sketch"
      if(prtarr .GT. 0) print *, out2
      print *, real(t1-t0)/(real(clockrate)/1000.0)

      do i_1 = 0, N-1
        do i_0 = 0, N-1
          if(abs(out(i_0,i_1)-out2(i_0,i_1)) .GT. 0.0001) then
            print *,"Found a difference in output!"
            print *,i_0,i_1,out(i_0,i_1),out2(i_0,i_1)
            stop
          endif
        enddo
      enddo
      print *,"No differences found between the 2 outputs"
      end

      subroutine rbGaussSeidel(in,f,ce,no,so,ea,we,output2,N)
      real in(0:N-1, 0:N-1)
      real f(0:N-1, 0:N-1)
      real ce
      real no
      real so
      real ea
      real we
      real output2(0:N-1, 0:N-1)
      integer N
      real tmp2(0:N-1, 0:N-1)
      do i_0 = 0, (N - 1)
        do i_1 = 0, (N - 1)
          tmp2(i_0, i_1) = 0
        enddo
      enddo
      do i_0 = 0, (N - 1)
        do i_1 = 0, (N - 1)
          output2(i_0, i_1) = 0
        enddo
      enddo
      do i2 = 1, ((N - 1) - 1)
        do j2 = 1, ((N - 1) - 1)
          if ((mod(i2,2) .EQ. mod(j2,2))) then
            tmp2(i2, j2) = (((((f(i2, j2) + (ce * in(i2, j2))) + (no * i
     +n((i2 - 1), j2))) + (so * in((i2 + 1), j2))) + (ea * in(i2, (j2 + 
     +1)))) + (we * in(i2, (j2 - 1))))
          endif
        enddo
      enddo
      do i3 = 1, ((N - 1) - 1)
        do j3 = 1, ((N - 1) - 1)
          if (.NOT. (mod(i3,2) .EQ. mod(j3,2))) then
            output2(i3, j3) = (((((f(i3, j3) + (ce * in(i3, j3))) + (no 
     +* tmp2((i3 - 1), j3))) + (so * tmp2((i3 + 1), j3))) + (ea * tmp2(i
     +3, (j3 + 1)))) + (we * tmp2(i3, (j3 - 1))))
          else
            output2(i3, j3) = tmp2(i3, j3)
          endif
        enddo
      enddo
      return
      end

      subroutine rbGaussSeidelSK(in,f,ce,no,so,ea,we,output2,N)
      real in(0:N-1, 0:N-1)
      real f(0:N-1, 0:N-1)
      real ce
      real no
      real so
      real ea
      real we
      real output2(0:N-1, 0:N-1)
      integer N
      integer ta2
      integer tb2
      integer ta3
      integer tb3
      integer ta4
      integer tb4
      integer ta5
      integer tb5
      integer ta6
      integer tb6
      integer ta7
      integer tb7
      integer ta8
      integer tb8
      integer ta9
      integer tb9
      real tmp2(0:N-1, 0:N-1)
      do i_0 = 0, (N - 1)
        do i_1 = 0, (N - 1)
          output2(i_0, i_1) = 0
        enddo
      enddo
      if ((mod(N,2) .EQ. 0)) then
        do j2 = 1, ((N / 2) - 1)
          ta2 = 1
          tb2 = ((2 * j2) - 1)
          output2(1, ((2 * j2) - 1)) = (((((f(ta2, tb2) + (ce * in(ta2, 
     +tb2))) + (no * in((ta2 - 1), tb2))) + (so * in((ta2 + 1), tb2))) +
     + (ea * in(ta2, (tb2 + 1)))) + (we * in(ta2, (tb2 - 1))))
          ta3 = 2
          tb3 = (2 * j2)
          output2(2, (2 * j2)) = (((((f(ta3, tb3) + (ce * in(ta3, tb3)))
     + + (no * in((ta3 - 1), tb3))) + (so * in((ta3 + 1), tb3))) + (ea *
     + in(ta3, (tb3 + 1)))) + (we * in(ta3, (tb3 - 1))))
        enddo
        do i2 = 2, ((N / 2) - 1)
          do j3 = 1, ((N / 2) - 1)
            ta4 = ((2 * i2) - 1)
            tb4 = ((2 * j3) - 1)
            output2(((2 * i2) - 1), ((2 * j3) - 1)) = (((((f(ta4, tb4) +
     + (ce * in(ta4, tb4))) + (no * in((ta4 - 1), tb4))) + (so * in((ta4
     + + 1), tb4))) + (ea * in(ta4, (tb4 + 1)))) + (we * in(ta4, (tb4 - 
     +1))))
            ta5 = (2 * i2)
            tb5 = (2 * j3)
            output2((2 * i2), (2 * j3)) = (((((f(ta5, tb5) + (ce * in(ta
     +5, tb5))) + (no * in((ta5 - 1), tb5))) + (so * in((ta5 + 1), tb5))
     +) + (ea * in(ta5, (tb5 + 1)))) + (we * in(ta5, (tb5 - 1))))
            ta6 = (((2 * i2) - 1) - 2)
            tb6 = (2 * j3)
            output2((((2 * i2) - 1) - 2), (2 * j3)) = (((((f(ta6, tb6) +
     + (ce * in(ta6, tb6))) + (no * output2((ta6 - 1), tb6))) + (so * ou
     +tput2((ta6 + 1), tb6))) + (ea * output2(ta6, (tb6 + 1)))) + (we * 
     +output2(ta6, (tb6 - 1))))
            ta7 = ((2 * i2) - 2)
            tb7 = ((2 * j3) - 1)
            output2(((2 * i2) - 2), ((2 * j3) - 1)) = (((((f(ta7, tb7) +
     + (ce * in(ta7, tb7))) + (no * output2((ta7 - 1), tb7))) + (so * ou
     +tput2((ta7 + 1), tb7))) + (ea * output2(ta7, (tb7 + 1)))) + (we * 
     +output2(ta7, (tb7 - 1))))
          enddo
        enddo
        do j4 = 1, ((N / 2) - 1)
          ta8 = (N - 2)
          tb8 = ((2 * j4) - 1)
          output2((N - 2), ((2 * j4) - 1)) = (((((f(ta8, tb8) + (ce * in
     +(ta8, tb8))) + (no * output2((ta8 - 1), tb8))) + (so * output2((ta
     +8 + 1), tb8))) + (ea * output2(ta8, (tb8 + 1)))) + (we * output2(t
     +a8, (tb8 - 1))))
          ta9 = (N - 3)
          tb9 = (2 * j4)
          output2((N - 3), (2 * j4)) = (((((f(ta9, tb9) + (ce * in(ta9, 
     +tb9))) + (no * output2((ta9 - 1), tb9))) + (so * output2((ta9 + 1)
     +, tb9))) + (ea * output2(ta9, (tb9 + 1)))) + (we * output2(ta9, (t
     +b9 - 1))))
        enddo
      else
        do i_0 = 0, (N - 1)
          do i_1 = 0, (N - 1)
            tmp2(i_0, i_1) = 0
          enddo
        enddo
        do i3 = 1, ((N - 1) - 1)
          do j5 = 1, ((N - 1) - 1)
            if ((mod(i3,2) .EQ. mod(j5,2))) then
              tmp2(i3, j5) = (((((f(i3, j5) + (ce * in(i3, j5))) + (no *
     + in((i3 - 1), j5))) + (so * in((i3 + 1), j5))) + (ea * in(i3, (j5 
     ++ 1)))) + (we * in(i3, (j5 - 1))))
            endif
          enddo
        enddo
        do i4 = 1, ((N - 1) - 1)
          do j6 = 1, ((N - 1) - 1)
            if (.NOT. (mod(i4,2) .EQ. mod(j6,2))) then
              output2(i4, j6) = (((((f(i4, j6) + (ce * in(i4, j6))) + (n
     +o * tmp2((i4 - 1), j6))) + (so * tmp2((i4 + 1), j6))) + (ea * tmp2
     +(i4, (j6 + 1)))) + (we * tmp2(i4, (j6 - 1))))
            else
              output2(i4, j6) = tmp2(i4, j6)
            endif
          enddo
        enddo
      endif
      return
      end
