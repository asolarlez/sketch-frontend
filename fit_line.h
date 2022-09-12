#ifndef FIT_LINE_H
#define FIT_LINE_H

#include <cstring>

#include "vops.h"

namespace ANONYMOUS{
}
namespace ANONYMOUS{
extern void sketch_main__Wrapper(int n, int* close/* len = n */);
extern void sketch_main__WrapperNospec(int n, int* close/* len = n */);
extern void sketch_main(int n, int* close/* len = n */);
extern void get_error(int _n, int* close/* len = _n */, int& _out);
extern void my_bool(int err, bool& _out);
extern void predict(int x, int& _out);
}

#endif
