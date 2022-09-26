#include <cstdio>
#include <assert.h>
#include <iostream>
using namespace std;
#include "vops.h"
#include "sketch.h"
namespace ANONYMOUS{

template<typename T_0>
Data* Data::create(int  num_doubles_, T_0* doubles_, int doubles_len){
  int tlen_doubles = num_doubles_; 
  void* temp= malloc( sizeof(Data)   + sizeof(double )*tlen_doubles); 
  Data* rv = new (temp)Data();
  rv->num_doubles =  num_doubles_;
  CopyArr(rv->doubles, doubles_, tlen_doubles, doubles_len ); 
  return rv;
}
void main__Wrapper(int num_doubles, double* doubles/* len = num_doubles */, bool out) {
  int  num_units__ANONYMOUS_s58=0;
  glblInit_num_units__ANONYMOUS_s67(num_units__ANONYMOUS_s58);
  _main(num_doubles, doubles, out, num_units__ANONYMOUS_s58);
}
void main__WrapperNospec(int num_doubles, double* doubles/* len = num_doubles */, bool out) {}
void glblInit_num_units__ANONYMOUS_s67(int& num_units__ANONYMOUS_s66) {
  num_units__ANONYMOUS_s66 = 0;
}
void _main(int num_doubles, double* doubles/* len = num_doubles */, bool out, int& num_units__ANONYMOUS_s57) {
  if ((num_doubles) >= (1)) {
    Data*  data=Data::create(num_doubles, doubles, num_doubles);
    print_init_or();
    print_init_and();
    num_units__ANONYMOUS_s57 = num_units__ANONYMOUS_s57 + 1;
    print_not();
    print_unit(6, 8, 0.1, 0.2);
    bool  bool=((data->doubles[6])) <= ((((data->doubles[8]) * 0.1) + 0.2));
    bool  rez_0=1;
    if (!(!(bool))) {
      rez_0 = 0;
    }
    bool  rez_1=rez_0;
    num_units__ANONYMOUS_s57 = num_units__ANONYMOUS_s57 + 1;
    print_not();
    print_unit(9, 11, 0.1, 0.2);
    bool  bool_0=((data->doubles[9])) <= ((((data->doubles[11]) * 0.1) + 0.2));
    if (!(!(bool_0))) {
      rez_1 = 0;
    }
    print_end_and();
    bool  rez=0;
    if (rez_1) {
      rez = 1;
    }
    print_end_or();
    assert ((rez) == (out));;
  }
}
void print_init_or() {}
void print_init_and() {}
void print_unit(int id0, int id1, double const0, double const1) {}
void print_end_and() {}
void print_end_or() {}
void print_not() { 
	/* This was defined as an uninterpreted function. 
	   Add your own body here. */ 

}
void print_init_if() { 
	/* This was defined as an uninterpreted function. 
	   Add your own body here. */ 

}
void print_end_if() { 
	/* This was defined as an uninterpreted function. 
	   Add your own body here. */ 

}

}
