      program mgInterpEasy
      end

      subroutine MGinter(in,half,fourth,eight,output,N)
      real in(0:N-1, 0:N-1, 0:N-1)
      integer half
      integer fourth
      integer eight
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
            if ((((mod(i,2) .EQ. 0) .AND. (mod(j,2) .EQ. 0)) .AND. (mod(k,2) .EQ. 0))) then
              output(i, j, k) = in((i / 2), (j / 2), (k / 2))
            endif
            if ((((mod(i,2) .EQ. 1) .AND. (mod(j,2) .EQ. 0)) .AND. (mod(k,2) .EQ. 0))) then
              output(i, j, k) = (half * (in((i / 2), (j / 2), (k / 2)) + in(((i / 2) + 1), (j / 2), (k / 2))))
            endif
            if ((((mod(i,2) .EQ. 0) .AND. (mod(j,2) .EQ. 1)) .AND. (mod(k,2) .EQ. 0))) then
              output(i, j, k) = (half * (in((i / 2), (j / 2), (k / 2)) + in((i / 2), ((j / 2) + 1), (k / 2))))
            endif
            if ((((mod(i,2) .EQ. 0) .AND. (mod(j,2) .EQ. 0)) .AND. (mod(k,2) .EQ. 1))) then
              output(i, j, k) = (half * (in((i / 2), (j / 2), (k / 2)) + in((i / 2), (j / 2), ((k / 2) + 1))))
            endif
            if ((((mod(i,2) .EQ. 1) .AND. (mod(j,2) .EQ. 1)) .AND. (mod(k,2) .EQ. 0))) then
              output(i, j, k) = (fourth * (((in((i / 2), (j / 2), (k / 2)) + in(((i / 2) + 1), ((j / 2) + 1), (k / 2))) + in((i / 2), ((j / 2) + 1), (k / 2))) + in(((i / 2) + 1), (j / 2), (k / 2))))
            endif
            if ((((mod(i,2) .EQ. 0) .AND. (mod(j,2) .EQ. 1)) .AND. (mod(k,2) .EQ. 1))) then
              output(i, j, k) = (fourth * (((in((i / 2), (j / 2), (k / 2)) + in((i / 2), ((j / 2) + 1), ((k / 2) + 1))) + in((i / 2), ((j / 2) + 1), (k / 2))) + in((i / 2), (j / 2), ((k / 2) + 1))))
            endif
            if ((((mod(i,2) .EQ. 1) .AND. (mod(j,2) .EQ. 0)) .AND. (mod(k,2) .EQ. 1))) then
              output(i, j, k) = (fourth * (((in((i / 2), (j / 2), (k / 2)) + in(((i / 2) + 1), (j / 2), ((k / 2) + 1))) + in(((i / 2) + 1), (j / 2), (k / 2))) + in((i / 2), (j / 2), ((k / 2) + 1))))
            endif
            if ((((mod(i,2) .EQ. 1) .AND. (mod(j,2) .EQ. 1)) .AND. (mod(k,2) .EQ. 1))) then
              output(i, j, k) = (eight * (((((((in((i / 2), (j / 2), (k / 2)) + in(((i / 2) + 1), (j / 2), (k / 2))) + in((i / 2), ((j / 2) + 1), (k / 2))) + in((i / 2), (j / 2), ((k / 2) + 1))) + in(((i / 2) + 1), ((j / 2) + 1), (k / 2))) + in((i / 2), ((j / 2) + 1), ((k / 2) + 1))) + in(((i / 2) + 1), (j / 2), ((k / 2) + 1))) + in(((i / 2) + 1), ((j / 2) + 1), ((k / 2) + 1))))
            endif
          enddo
        enddo
      enddo
      return
      end

      subroutine skMGinter(in,half,fourth,eight,output,N)
      real in(0:N-1, 0:N-1, 0:N-1)
      integer half
      integer fourth
      integer eight
      real output(0:(2 * N)-1, 0:(2 * N)-1, 0:(2 * N)-1)
      integer N
      real z1(0:(N + 1)-1)
      real z2(0:(N + 1)-1)
      real z3(0:(N + 1)-1)
      integer t1
      integer t2
      integer t3
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
          do k = 0, (((N - 2) + 5) - 1)
            t1 = (in((k + 0), (j + 0), (i + 0)) + in((k + 0), (j + 0), (i + 1)))
            t2 = (in((k + 0), (j + 1), (i + 0)) + in((k + 0), (j + 0), (i + 0)))
            t3 = (in((k + 0), (j + 1), (i + 1)) + in((k + 0), (j + 1), (i + 0)))
            t3 = (t3 + t1)
            z1(k) = t1
            z2(k) = t2
            z3(k) = t3
          enddo
          do k = 0, (((N - 2) + 1) - 1)
            output(((k * 2) + 0), ((j * 2) + 0), ((i * 2) + 0)) = in((k + 0), (j + 0), (i + 0))
            output(((k * 2) + 1), ((j * 2) + 0), ((i * 2) + 0)) = (half * (in((k + 1), (j + 0), (i + 0)) + in((k + 0), (j + 0), (i + 0))))
          enddo
          do k = 0, (((N - 2) + 1) - 1)
            output(((k * 2) + 0), ((j * 2) + 0), ((i * 2) + 1)) = (half * z1((k + 0)))
            output(((k * 2) + 1), ((j * 2) + 0), ((i * 2) + 1)) = (fourth * (z1((k + 0)) + z1((k + 1))))
          enddo
          do k = 0, (((N - 2) + 1) - 1)
            output(((k * 2) + 0), ((j * 2) + 1), ((i * 2) + 0)) = (half * z2((k + 0)))
            output(((k * 2) + 1), ((j * 2) + 1), ((i * 2) + 0)) = (fourth * (z2((k + 0)) + z2((k + 1))))
          enddo
          do k = 0, (((N - 2) + 1) - 1)
            output(((k * 2) + 0), ((j * 2) + 1), ((i * 2) + 1)) = (fourth * z3((k + 0)))
            output(((k * 2) + 1), ((j * 2) + 1), ((i * 2) + 1)) = (eight * (z3((k + 0)) + z3((k + 1))))
          enddo
        enddo
      enddo
      return
      end
