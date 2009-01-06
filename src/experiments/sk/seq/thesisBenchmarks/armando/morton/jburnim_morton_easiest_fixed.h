#ifndef JBURNIM_MORTON_EASIEST_FIXED_H
#define JBURNIM_MORTON_EASIEST_FIXED_H

#include "bitvec.h"
#include "fixedarr.h"
extern void sketch(bitvec<2>  x_0, bitvec<2>  y_1, bitvec<4> & s_2);
extern void interleave_bits(bitvec<2>  x_0, bitvec<2>  y_1, bitvec<4> & s_2);

#endif
