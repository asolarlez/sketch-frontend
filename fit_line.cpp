#include <cstdio>
#include <assert.h>
#include <iostream>
using namespace std;
#include "vops.h"
#include "fit_line.h"
namespace ANONYMOUS{

void sketch_main__Wrapper(int n, int* close/* len = n */) {
  sketch_main(n, close);
}
void sketch_main__WrapperNospec(int n, int* close/* len = n */) {}
void sketch_main(int n, int* close/* len = n */) {
  int  err_s1=0;
  get_error(n, close, err_s1);
  bool  ret_s3=0;
  my_bool(err_s1, ret_s3);
}
void get_error(int _n, int* close/* len = _n */, int& _out) {
  assert ((_n) == (24));;
  int _tt0[24] = {22, 22, 23, 24, 25, 26, 13, 14, 15, 16, 17, 53, 52, 32, 32, 40, 42, 43, 45, 46, 48, 49, 51, 52};
  int*  _predict= new int [24]; CopyArr<int >(_predict,_tt0, 24, 24);
  int _tt1[24] = {13, 8, 4, 0, 7, 1, 5, 4, 12, 10, 13, 5, 9, 15, 3, 1, 13, 16, 6, 8, 13, 14, 2, 5};
  int*  _diffs= new int [24]; CopyArr<int >(_diffs,_tt1, 24, 24);
  int _tt2[24] = {35, 30, 27, 24, 32, 25, 18, 10, 27, 26, 30, 48, 61, 17, 29, 41, 55, 59, 51, 54, 61, 63, 53, 57};
  int*  _close= new int [24]; CopyArr<int >(_close,_tt2, 24, 24);
  _out = 0;
  for (int  i=0;(i) < (24);i = i + 1){
    assert (((_close[i])) == ((close[i])));;
    int  _out_s5=0;
    predict(i, _out_s5);
    assert (((_predict[i])) == (_out_s5));;
    int  diff_s7=0;
    predict(i, diff_s7);
    int  diff=0;
    diff = (close[i]) - diff_s7;
    if ((diff) < (0)) {
      diff = diff * -1;
    }
    assert (((_diffs[i])) == (diff));;
    assert ((diff) < (17));;
    _out = _out + diff;
  }
  delete[] _predict;
  delete[] _diffs;
  delete[] _close;
  return;
}
void my_bool(int err, bool& _out) {
  assert ((187) == (err));;
  _out = (err) <= (1024);
  return;
}
void predict(int x, int& _out) {
  int  _out_s9=0;
  if ((x) < (6)) {
    _out_s9 = ((x * 4) / 5) + 22;
  } else {
    int  _out_s15=0;
    if ((x) < (1)) {
      _out_s15 = (x / 2) + -9;
    } else {
      int  _out_s15__0=0;
      if ((x) < (11)) {
        _out_s15__0 = ((x * 7) / 6) + 6;
      } else {
        int  _out_s15_0=0;
        if ((x) < (13)) {
          _out_s15_0 = ((x * -5) / 6) + 62;
        } else {
          int  _out_s15__0_0=0;
          if ((x) < (1)) {
            _out_s15__0_0 = (x / 3) + -16;
          } else {
            int  _out_s15_1=0;
            if ((x) < (8)) {
              _out_s15_1 = ((x * -4) / 5) + -58;
            } else {
              int  _out_s15__0_1=0;
              if ((x) < (15)) {
                _out_s15__0_1 = ((x * -2) / 5) + 37;
              } else {
                int  _out_s15_2=0;
                if ((x) < (31)) {
                  _out_s15_2 = ((x * 3) / 2) + 18;
                } else {
                  assert ((x) < (4));;
                  _out_s15_2 = ((x * -7) / 3) + -30;
                }
                _out_s15__0_1 = _out_s15_2;
              }
              _out_s15_1 = _out_s15__0_1;
            }
            _out_s15__0_0 = _out_s15_1;
          }
          _out_s15_0 = _out_s15__0_0;
        }
        _out_s15__0 = _out_s15_0;
      }
      _out_s15 = _out_s15__0;
    }
    _out_s9 = _out_s15;
  }
  _out = _out_s9;
  return;
}

}
