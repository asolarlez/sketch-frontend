#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include "llreverse.h"

void sketchTest() {
  bitvec<3> in0;
  int in1;
  bitvec<3> outsk;
  bitvec<3> outsp;
  for(int _test_=0;_test_<100;_test_++) {
    for(int i=0;i<3;i++) {
      in0[i]=rand() % 2;
    }
    in1=abs(rand()+(rand()<<16)) % 5;
    for(int i=0;i<3;i++) {
      outsk[i]=0U;
    }
    for(int i=0;i<3;i++) {
      outsp[i]=0U;
    }
    sketch(in0,in1,outsk);
    spec(in0,in1,outsp);

	cout<<"in0 = "<<in0<<endl;
	cout<<"in1 = "<<in1<<endl;
	cout<<"outsk = "<<outsk<<endl;
	cout<<"outsp = "<<outsp<<endl;
    for(int i=0;i<3;i++) {
      if(outsk.sub<1>(i)!=outsp.sub<1>(i)) {
        printf("Automated testing failed in sketchTest\n");
        cout<<"in0 = "<<in0<<endl;
        cout<<"in1 = "<<in1<<endl;
        cout<<"outsk = "<<outsk<<endl;
        cout<<"outsp = "<<outsp<<endl;
        exit(1);
      }
    }
  }
}

int main(void) {
  srand(time(0));
  sketchTest();
  printf("Automated testing passed for llreverse\n");
  return 0;
}
