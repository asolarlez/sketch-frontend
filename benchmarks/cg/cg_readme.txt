Original CG benchmark:

The processors are arranged in a grid of nprows*npcols. nprows and npcols must both be powers of 2, and when they are not equal, npcols must be nprows*2, i.e. the partition of rows can be a little coarser than the partition of columns, but not too much.

proc_row = me / npcols; proc_col = me - proc_row*npcols

The matrix is sparse, and the dimension is naa*naa.

setup_submatrix_info() is the function to calculate partition and communication plan.
each processor contains (logically) [firstrow..lastrow]*[firstcol..lastcol], all ends inclusive.
local sparse array (CSR format): a is the element value, colidx and rowstr both refer to "local" index, i.e. colidx \in [1..lastcol-firstcol+1], rowstr [1..lastrow-firstrow+1].

when naa is not evenly divisible by npcols and npcols != nprows, then
the columns are first divided by npcols/2 parts (i.e. nprows parts),
each part is indexed by proc_col/2, then each part is further partitioned
to two subparts. In this case, if me is even, it will get a wider subpart.
why this is necessary? why can't we just use proc_col to partition? because there is a transpose phase, where we must guarantee that the two exchanging peer A and B are symmetric, i.e. A's number of rows equals to B's number of columns, and vice versa.

If npcols = nprows: send_start=1, send_len = lastrow-firstrow+1,
exch_proc = (me%nprows)*nprows + me/nprows

If npcols != nprows:
exch_proc = 2*( (me/2)%nprows *nprows + me/2/nprows ) + me%2
if me is even, send_start=1, send_len = (lastrow-firstrow)/2+1
if me is odd, send_start=(lastrow-firstrow)/2+2, send_len = (lastrow-firstrow+1)/2.

exch_recv_length always = lastcol-firstcol+1


makea: make a sparse matrix.
      call makea(naa, nzz, a, colidx, rowstr, nonzer,
     >           firstrow, lastrow, firstcol, lastcol, 
     >           rcond, arow, acol, aelt, v, iv, shift)
c---------------------------------------------------------------------
c  Note: as a result of the above call to makea:
c        values of j used in indexing rowstr go from 1 --> lastrow-firstrow+1
c        values of colidx which are col indexes go from firstcol --> lastcol
c        So:
c        Shift the col index vals from actual (firstcol --> lastcol ) 
c        to local, i.e., (1 --> lastcol-firstcol+1)
c---------------------------------------------------------------------
      do j=1,lastrow-firstrow+1
         do k=rowstr(j),rowstr(j+1)-1
            colidx(k) = colidx(k) - firstcol + 1
         enddo
      enddo


