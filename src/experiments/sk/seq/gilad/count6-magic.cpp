#include <cstdio>
#include <assert.h>
#include "count6-magic.h"
void count_sk(unsigned char* a_0, unsigned int& s_1) {
  s_1 = ((unsigned int)(a_0));
}
void count(unsigned char* a_0, unsigned int& s_1) {
  unsigned int ret_2 = 0;
  for (unsigned int i_3 = 0; i_3 < 8; i_3++) {
    ret_2 = ret_2 + a_0[i_3];
  }
  s_1 = ret_2;
}
