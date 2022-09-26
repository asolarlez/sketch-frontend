#ifndef SKETCH_H
#define SKETCH_H

#include <cstring>

#include "vops.h"

namespace ANONYMOUS{
class Data; 
}
namespace ANONYMOUS{
class Data; 
class Data{
  public:
  int  num_doubles;
  double  doubles[];
  Data(){}
template<typename T_0>
  static Data* create(  int  num_doubles_,   T_0* doubles_, int doubles_len);
  ~Data(){
  }
  void operator delete(void* p){ free(p); }
};
extern void main__Wrapper(int num_doubles, double* doubles/* len = num_doubles */, bool out);
extern void main__WrapperNospec(int num_doubles, double* doubles/* len = num_doubles */, bool out);
extern void glblInit_num_units__ANONYMOUS_s67(int& num_units__ANONYMOUS_s66);
extern void _main(int num_doubles, double* doubles/* len = num_doubles */, bool out, int& num_units__ANONYMOUS_s57);
extern void print_init_or();
extern void print_init_and();
extern void print_unit(int id0, int id1, double const0, double const1);
extern void print_end_and();
extern void print_end_or();
extern void print_not();
extern void print_init_if();
extern void print_end_if();
}

#endif
