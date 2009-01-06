#ifndef LLREVERSE_H
#define LLREVERSE_H

#include "bitvec.h"
#include "fixedarr.h"
class node; 
class list; 
class node{
  public:  node* next;
  bitvec<1> val;
};
class list{
  public:  node* head;
  node* tail;
};
extern void sketch(bitvec<3>  elems_0, int n_1, bitvec<3> & s_2);
extern void spec(bitvec<3>  elems_0, int n_1, bitvec<3> & s_2);
extern void populate(bitvec<3>  elems_0, int n_1, list*& s_2);
extern void reverseSK(list* l_0, list*& s_1);
extern void serialize(list* l_0, int n_1, bitvec<3> & s_2);
extern void reverse(list* l_0, list*& s_1);

#endif
