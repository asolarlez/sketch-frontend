#include <cstdio>
#include <assert.h>
#include "llreverse.h"
void sketch(bitvec<3>  elems_0, int n_1, bitvec<3> & s_2) {
  int n_3 = n_1;
  if ((n_1) >= (3)) {
    n_3 = 3;
  }
  list* s_4 = NULL;
  populate(elems_0, n_3, s_4);
  list* s_5 = NULL;
  reverseSK(s_4, s_5);
  bitvec<3> s_6 = bitvec<1>(0U);
  serialize(s_5, n_3, s_6);
  s_2 = s_6;
}
void spec(bitvec<3>  elems_0, int n_1, bitvec<3> & s_2) {
  int n_3 = n_1;
  if ((n_1) >= (3)) {
    n_3 = 3;
  }
  list* s_4 = NULL;
  populate(elems_0, n_3, s_4);
  list* s_5 = NULL;
  reverse(s_4, s_5);
  bitvec<3> s_6 = bitvec<1>(0U);
  serialize(s_5, n_3, s_6);
  s_2 = s_6;
}
void populate(bitvec<3>  elems_0, int n_1, list*& s_2) {
  list* l_3 = new list();
  node* prev_4 = NULL;
  for (int i_5 = 0; (i_5) < (n_1); i_5 = i_5 + 1) {
    node* t_6 = new node();
    t_6->val = elems_0.sub<1>(i_5);
    if ((prev_4) != (NULL)) {
      prev_4->next = t_6;
    }
    prev_4 = t_6;
    if ((i_5) == (0)) {
      l_3->head = t_6;
    }
  }
  l_3->tail = prev_4;
  s_2 = l_3;
}
void reverseSK(list* l_0, list*& s_1) {
  list* nl_2 = new list();
  nl_2->head = NULL;
  nl_2->tail = NULL;
  bitvec<1> c_3 = bitvec<1>(0U);
  bitvec<1> s_4 = bitvec<1>(0U);
  node* t_5 = l_0->head;
  node* s_6 = NULL;
  s_4 = (t_5) != (s_6);
  c_3 = s_4;
  node* tmp_7 = NULL;
  while (c_3) {
    bitvec<1> s_8 = bitvec<1>(0U);
    node* t_9 = l_0->head;
    node* t_10 = l_0->tail;
    s_8 = (t_9) != (t_10);
    if (s_8) {
      node* t_11 = l_0->head;
      t_11 = t_11->next;
      tmp_7 = t_11;
    }
    bitvec<1> s_12 = bitvec<1>(0U);
    node* t_13 = l_0->tail;
    node* t_14 = nl_2->tail;
    s_12 = (t_13) != (t_14);
    if (s_12) {
      node* t_15 = nl_2->head;
      l_0->head->next = t_15;
    }
    bitvec<1> s_16 = bitvec<1>(0U);
    node* t_17 = nl_2->tail;
    node* t_18 = l_0->tail;
    s_16 = (t_17) != (t_18);
    if (s_16) {
      node* t_19 = l_0->head;
      nl_2->head = t_19;
    }
    bitvec<1> s_20 = bitvec<1>(0U);
    node* t_21 = nl_2->tail;
    node* t_22 = l_0->tail;
    s_20 = (t_21) != (t_22);
    if (s_20) {
      l_0->head = tmp_7;
    }
    bitvec<1> s_23 = bitvec<1>(0U);
    node* t_24 = nl_2->tail;
    node* s_25 = NULL;
    s_23 = (t_24) == (s_25);
    if (s_23) {
      node* t_26 = nl_2->head;
      nl_2->tail = t_26;
    }
    bitvec<1> s_27 = bitvec<1>(0U);
    node* t_28 = nl_2->head;
    node* t_29 = l_0->tail;
    s_27 = (t_28) != (t_29);
    c_3 = s_27;
  }
  s_1 = nl_2;
}
void serialize(list* l_0, int n_1, bitvec<3> & s_2) {
  bitvec<3> out_3 = bitvec<1>(0U);
  node* t_4 = l_0->head;
  for (int i_5 = 0; (i_5) < (n_1); i_5 = i_5 + 1) {
    out_3[i_5] = t_4->val;
    t_4 = t_4->next;
  }
  s_2 = out_3;
}
void reverse(list* l_0, list*& s_1) {
  if ((l_0->head) == (NULL)) {
    s_1 = l_0;
  } else {
    node* n_2 = l_0->head;
    l_0->head = l_0->head->next;
    if ((l_0->head) == (NULL)) {
      l_0->tail = NULL;
    }
    list* l2_3 = new list();
    l2_3->head = n_2;
    l2_3->tail = n_2;
    n_2->next = NULL;
    list* s_4 = NULL;
    reverse(l_0, s_4);
    list* s_5 = NULL;
    if ((s_4->head) == (NULL)) {
      s_5 = l2_3;
    } else {
      if ((l2_3->head) == (NULL)) {
        s_5 = s_4;
      } else {
        s_4->tail->next = l2_3->head;
        s_4->tail = l2_3->tail;
        s_5 = s_4;
      }
    }
    s_1 = s_5;
  }
}
