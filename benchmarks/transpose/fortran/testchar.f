      program testchar
      implicit none
      call prt('haha')
      end

      subroutine prt(title)
      implicit none
      character title(20)
      write(*, 1000) title
1000  format('prt: ', A)
      return
      end
