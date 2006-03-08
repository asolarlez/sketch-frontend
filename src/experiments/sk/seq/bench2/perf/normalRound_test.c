#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include "normalRound.h"

void normalRoundskTest() {
  unsigned int in0[4];
  unsigned int in1[4];
  unsigned int outsk[4];
  unsigned int outsp[4];
  for(int test=0;test<100;test++) {
    for(int i=0;i<4;i++) {
      in0[i]=rand()+(rand()<<16);
    }
    for(int i=0;i<4;i++) {
      in1[i]=rand()+(rand()<<16);
    }
    for(int i=0;i<4;i++) {
      outsk[i]=0;
    }
    for(int i=0;i<4;i++) {
      outsp[i]=0;
    }
    normalRoundsk(in0,in1,outsk);
    normalRound(in0,in1,outsp);
    for(int i=0;i<4;i++) {
      if(outsk[i]!=outsp[i]) {
        printf("Automated testing failed in normalRoundskTest\n");
        printf("%5s=","in0");
        for(int z=0;z<4;z++) {
          printf("%08x",in0[z]);
        }
        printf("\n");
        printf("%5s=","in1");
        for(int z=0;z<4;z++) {
          printf("%08x",in1[z]);
        }
        printf("\n");
        printf("%5s=","outsk");
        for(int z=0;z<4;z++) {
          printf("%08x",outsk[z]);
        }
        printf("\n");
        printf("%5s=","outsp");
        for(int z=0;z<4;z++) {
          printf("%08x",outsp[z]);
        }
        printf("\n");
        exit(1);
      }
    }
  }
}

int main(void) {
  srand(time(0));
  normalRoundskTest();
  printf("Automated testing passed");
  return 0;
}
