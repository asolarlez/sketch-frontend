#include <cstdio>
#include <assert.h>
#include "jburnim_morton_easiest_fixed.h"
void sketch(bitvec<2>  x_0, bitvec<2>  y_1, bitvec<4> & s_2) {
  bitvec<4> x2_3 = "0000";
  bitvec<4> mask_4 = "1010";
  x2_3 = (x_0 | (x_0 << (unsigned)1)) & mask_4;
  bitvec<4> mask_5 = "1010";
  x2_3 = (x2_3 | (x2_3 << (unsigned)0)) & mask_5;
  bitvec<4> mask_6 = "1010";
  x2_3 = (x2_3 | (x2_3 << (unsigned)0)) & mask_6;
  bitvec<4> mask_7 = "1010";
  x2_3 = (x2_3 | (x2_3 << (unsigned)0)) & mask_7;
  bitvec<4> y2_8 = "0000";
  bitvec<4> mask_9 = "1010";
  y2_8 = (y_1 | (y_1 << (unsigned)1)) & mask_9;
  s_2 = x2_3 | (y2_8 << (unsigned)1);
}
void interleave_bits(bitvec<2>  x_0, bitvec<2>  y_1, bitvec<4> & s_2) {
  bitvec<4> ret_3 = bitvec<1>(0U);
  for (int i_4 = 0; (i_4) < (2); i_4 = i_4 + 1) {
    ret_3[i_4 * 2] = x_0.sub<1>(i_4);
    ret_3[(i_4 * 2) + 1] = y_1.sub<1>(i_4);
  }
  s_2 = ret_3;
}
