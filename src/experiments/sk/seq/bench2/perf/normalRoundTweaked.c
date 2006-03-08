#include "normalRound.h"
#include "aestables.h"

#define SK_BITASSIGN(a,i,x) a=((a)&(~(1<<(i))))|(((x)&1)<<(i))
void normalRoundsk(unsigned int* _in, unsigned int* rkey, unsigned int* _out_0) { // normalRound.sk:6
  { // normalRound.sk:6
	  unsigned int in[4];
    unsigned int _frv_0[4]; // normalRound.sk:12
    for (unsigned int __sa49 = 0; (__sa49 < 4); __sa49++) _frv_0[__sa49] = 0; // normalRound.sk:12
    ShiftRows(_in, _frv_0); // normalRound.sk:12
    for (unsigned int __sa50 = 0; (__sa50 < 4); __sa50++) in[__sa50] = _frv_0[__sa50]; // normalRound.sk:12
    unsigned int output[4]; // normalRound.sk:14
    for (unsigned int __sa51 = 0; (__sa51 < 4); __sa51++) output[__sa51] = 0; // normalRound.sk:14
    for (unsigned int i = 0; (i < 4); ++i) { // normalRound.sk:15
      { // normalRound.sk:15
        unsigned char inv0; // normalRound.sk:16
        inv0 = 0; // normalRound.sk:16
        unsigned char inv1; // normalRound.sk:17
        inv1 = 0; // normalRound.sk:17
        unsigned char inv2; // normalRound.sk:18
        inv2 = 0; // normalRound.sk:18
        unsigned char inv3; // normalRound.sk:19
        inv3 = 0; // normalRound.sk:19
        for (unsigned int j = 0; (j < 8); ++j) { // normalRound.sk:20
          { // normalRound.sk:20
            SK_BITASSIGN(inv0, j, ((in[(((i * 32) + j) / 32)] >> (((i * 32) + j) % 32)) & 1)); // normalRound.sk:21
            SK_BITASSIGN(inv1, j, ((in[((((i * 32) + 8) + j) / 32)] >> ((((i * 32) + 8) + j) % 32)) & 1)); // normalRound.sk:22
            SK_BITASSIGN(inv2, j, ((in[((((i * 32) + 16) + j) / 32)] >> ((((i * 32) + 16) + j) % 32)) & 1)); // normalRound.sk:23
            SK_BITASSIGN(inv3, j, ((in[((((i * 32) + 24) + j) / 32)] >> ((((i * 32) + 24) + j) % 32)) & 1)); // normalRound.sk:24
          }; // normalRound.sk:20
        }; // normalRound.sk:20
        unsigned int i0; // normalRound.sk:26
        i0 = ((unsigned int)(inv0)); // normalRound.sk:26
        unsigned int i1; // normalRound.sk:27
        i1 = ((unsigned int)(inv1)); // normalRound.sk:27
        unsigned int i2; // normalRound.sk:28
        i2 = ((unsigned int)(inv2)); // normalRound.sk:28
        unsigned int i3; // normalRound.sk:29
        i3 = ((unsigned int)(inv3)); // normalRound.sk:29
        for (unsigned int j = 0; (j < 32); ++j) { // normalRound.sk:30
          { // normalRound.sk:30
            unsigned char tmp; // normalRound.sk:31
            tmp = (((((T0[(((i0 * 32) + j) / 32)] >> (((i0 * 32) + j) % 32)) & 1) ^ ((T1[(((i1 * 32) + j) / 32)] >> (((i1 * 32) + j) % 32)) & 1)) ^ ((T2[(((i2 * 32) + j) / 32)] >> (((i2 * 32) + j) % 32)) & 1)) ^ ((T3[(((i3 * 32) + j) / 32)] >> (((i3 * 32) + j) % 32)) & 1)); // normalRound.sk:31
            SK_BITASSIGN(output[(((i * 32) + j) / 32)], (((i * 32) + j) % 32), (tmp ^ ((rkey[(((i * 32) + j) / 32)] >> (((i * 32) + j) % 32)) & 1))); // normalRound.sk:32
          }; // normalRound.sk:30
        }; // normalRound.sk:30
      }; // normalRound.sk:15
    }; // normalRound.sk:15
    for (unsigned int __sa52 = 0; (__sa52 < 4); __sa52++) _out_0[__sa52] = output[__sa52]; // normalRound.sk:35
  }; // normalRound.sk:6
}
void normalRound(unsigned int* in, unsigned int* rkey, unsigned int* _out_0) { // normalRound.sk:40
  unsigned int _frv_0[4]; // normalRound.sk:41
  for (unsigned int __sa53 = 0; (__sa53 < 4); __sa53++) _frv_0[__sa53] = 0; // normalRound.sk:41
  ByteSub(in, _frv_0); // normalRound.sk:41
  unsigned int tmp1[4]; // normalRound.sk:41
  for (unsigned int __sa54 = 0; (__sa54 < 4); __sa54++) tmp1[__sa54] = 0; // normalRound.sk:41
  for (unsigned int __sa55 = 0; (__sa55 < 4); __sa55++) tmp1[__sa55] = _frv_0[__sa55]; // normalRound.sk:41
  unsigned int _frv_1[4]; // normalRound.sk:42
  for (unsigned int __sa56 = 0; (__sa56 < 4); __sa56++) _frv_1[__sa56] = 0; // normalRound.sk:42
  ShiftRows(tmp1, _frv_1); // normalRound.sk:42
  unsigned int tmp2[4]; // normalRound.sk:42
  for (unsigned int __sa57 = 0; (__sa57 < 4); __sa57++) tmp2[__sa57] = 0; // normalRound.sk:42
  for (unsigned int __sa58 = 0; (__sa58 < 4); __sa58++) tmp2[__sa58] = _frv_1[__sa58]; // normalRound.sk:42
  unsigned int _frv_2[4]; // normalRound.sk:43
  for (unsigned int __sa59 = 0; (__sa59 < 4); __sa59++) _frv_2[__sa59] = 0; // normalRound.sk:43
  MixColumns(tmp2, _frv_2); // normalRound.sk:43
  unsigned int tmp3[4]; // normalRound.sk:43
  for (unsigned int __sa60 = 0; (__sa60 < 4); __sa60++) tmp3[__sa60] = 0; // normalRound.sk:43
  for (unsigned int __sa61 = 0; (__sa61 < 4); __sa61++) tmp3[__sa61] = _frv_2[__sa61]; // normalRound.sk:43
  for (unsigned int __sa62 = 0; (__sa62 < 4); __sa62++) _out_0[__sa62] = (tmp3[__sa62] ^ rkey[__sa62]); // normalRound.sk:44
}
void ByteSub(unsigned int* in, unsigned int* _out_0) { // normalRound.sk:47
  unsigned int result[4]; // normalRound.sk:48
  for (unsigned int __sa63 = 0; (__sa63 < 4); __sa63++) result[__sa63] = 0; // normalRound.sk:48
  for (unsigned int i = 0; (i < 16); ++i) { // normalRound.sk:49
    { // normalRound.sk:49
      unsigned char word; // normalRound.sk:50
      word = 0; // normalRound.sk:50
      for (unsigned int j = 0; (j < 8); j++) { // normalRound.sk:51
        { // normalRound.sk:51
          SK_BITASSIGN(word, j, ((in[(((i * 8) + j) / 32)] >> (((i * 8) + j) % 32)) & 1)); // normalRound.sk:52
        }; // normalRound.sk:51
      }; // normalRound.sk:51
      unsigned char _frv_0; // normalRound.sk:54
      _frv_0 = 0; // normalRound.sk:54
      ByteSubInd(word, _frv_0); // normalRound.sk:54
      word = _frv_0; // normalRound.sk:54
      for (unsigned int j = 0; (j < 8); j++) { // normalRound.sk:55
        { // normalRound.sk:55
          SK_BITASSIGN(result[(((i * 8) + j) / 32)], (((i * 8) + j) % 32), ((word >> j) & 1)); // normalRound.sk:56
        }; // normalRound.sk:55
      }; // normalRound.sk:55
    }; // normalRound.sk:49
  }; // normalRound.sk:49
  for (unsigned int __sa64 = 0; (__sa64 < 4); __sa64++) _out_0[__sa64] = result[__sa64]; // normalRound.sk:59
}
void ShiftRows(unsigned int* in, unsigned int* _out_0) { // normalRound.sk:62
  unsigned int shift[4] = {0,1,2,3}; // normalRound.sk:63
  unsigned int Sin[4]; // normalRound.sk:64
  for (unsigned int __sa65 = 0; (__sa65 < 4); __sa65++) Sin[__sa65] = 0; // normalRound.sk:64
  unsigned int Sout[4]; // normalRound.sk:65
  for (unsigned int __sa66 = 0; (__sa66 < 4); __sa66++) Sout[__sa66] = 0; // normalRound.sk:65
  unsigned int k; // normalRound.sk:66
  k = 0; // normalRound.sk:66
  for (unsigned int c = 0; (c < 4); ++c) { // normalRound.sk:67
    { // normalRound.sk:67
      for (unsigned int r = 0; (r < 4); ++r) { // normalRound.sk:68
        { // normalRound.sk:68
          for (unsigned int i = 0; (i < 8); ++i) { // normalRound.sk:69
            { // normalRound.sk:69
              SK_BITASSIGN(Sin[((i + (8 * (r + (4 * c)))) / 32)], ((i + (8 * (r + (4 * c)))) % 32), ((in[(k / 32)] >> (k % 32)) & 1)); // normalRound.sk:70
              k = (k + 1); // normalRound.sk:70
            }; // normalRound.sk:69
          }; // normalRound.sk:69
        }; // normalRound.sk:68
      }; // normalRound.sk:68
    }; // normalRound.sk:67
  }; // normalRound.sk:67
  for (unsigned int c = 0; (c < 4); ++c) { // normalRound.sk:74
    { // normalRound.sk:74
      for (unsigned int r = 0; (r < 4); ++r) { // normalRound.sk:75
        { // normalRound.sk:75
          unsigned int newc; // normalRound.sk:76
          newc = ((c + shift[r]) % 4); // normalRound.sk:76
          for (unsigned int i = 0; (i < 8); ++i) { // normalRound.sk:77
            SK_BITASSIGN(Sout[((i + (8 * (r + (4 * c)))) / 32)], ((i + (8 * (r + (4 * c)))) % 32), ((Sin[((i + (8 * (r + (4 * newc)))) / 32)] >> ((i + (8 * (r + (4 * newc)))) % 32)) & 1)); // normalRound.sk:77
          }; // normalRound.sk:77
        }; // normalRound.sk:75
      }; // normalRound.sk:75
    }; // normalRound.sk:74
  }; // normalRound.sk:74
  for (unsigned int __sa67 = 0; (__sa67 < 4); __sa67++) _out_0[__sa67] = Sout[__sa67]; // normalRound.sk:80
}
void intToBit8(unsigned int& v, unsigned char& _out_0) { // normalRound.sk:83
  unsigned char output; // normalRound.sk:84
  output = 0; // normalRound.sk:84
  for (unsigned int i = 0; (i < 8); ++i) { // normalRound.sk:85
    { // normalRound.sk:85
      SK_BITASSIGN(output, i, ((v % 2) > 0)); // normalRound.sk:86
      v = (v / 2); // normalRound.sk:87
    }; // normalRound.sk:85
  }; // normalRound.sk:85
  _out_0 = output; // normalRound.sk:89
}
void ByteSubInd(unsigned char& in, unsigned char& _out_0) { // normalRound.sk:93
  unsigned int SBTable[256] = {99,124,119,123,242,107,111,197,48,1,103,43,254,215,171,118,202,130,201,125,250,89,71,240,173,212,162,175,156,164,114,192,183,253,147,38,54,63,247,204,52,165,229,241,113,216,49,21,4,199,35,195,24,150,5,154,7,18,128,226,235,39,178,117,9,131,44,26,27,110,90,160,82,59,214,179,41,227,47,132,83,209,0,237,32,252,177,91,106,203,190,57,74,76,88,207,208,239,170,251,67,77,51,133,69,249,2,127,80,60,159,168,81,163,64,143,146,157,56,245,188,182,218,33,16,255,243,210,205,12,19,236,95,151,68,23,196,167,126,61,100,93,25,115,96,129,79,220,34,42,144,136,70,238,184,20,222,94,11,219,224,50,58,10,73,6,36,92,194,211,172,98,145,149,228,121,231,200,55,109,141,213,78,169,108,86,244,234,101,122,174,8,186,120,37,46,28,166,180,198,232,221,116,31,75,189,139,138,112,62,181,102,72,3,246,14,97,53,87,185,134,193,29,158,225,248,152,17,105,217,142,148,155,30,135,233,206,85,40,223,140,161,137,13,191,230,66,104,65,153,45,15,176,84,187,22}; // normalRound.sk:94
  unsigned int x; // normalRound.sk:110
  x = SBTable[((unsigned int)(in))]; // normalRound.sk:110
  unsigned char _frv_0; // normalRound.sk:111
  _frv_0 = 0; // normalRound.sk:111
  intToBit8(x, _frv_0); // normalRound.sk:111
  _out_0 = _frv_0; // normalRound.sk:111
}
void GFM01(unsigned char& in, unsigned char& _out_0) { // normalRound.sk:118
  _out_0 = in; // normalRound.sk:119
}
void GFM03(unsigned char& input, unsigned char& _out_0) { // normalRound.sk:122
  unsigned short int in; // normalRound.sk:124
  in = 0x0; // normalRound.sk:124
  for (unsigned int i = 0; (i < 8); ++i) { // normalRound.sk:127
    SK_BITASSIGN(in, (i + 1), ((input >> i) & 1)); // normalRound.sk:127
  }; // normalRound.sk:126
  for (unsigned int i = 0; (i < 8); ++i) { // normalRound.sk:130
    SK_BITASSIGN(in, i, (((in >> i) & 1) ^ ((input >> i) & 1))); // normalRound.sk:130
  }; // normalRound.sk:129
  unsigned short int modpoly; // normalRound.sk:140
  modpoly = 0x11b; // normalRound.sk:140
  if ((((in >> 8) & 1) == 1)) { // normalRound.sk:142
    { // normalRound.sk:142
      for (unsigned int k = 0; (k < 8); ++k) { // normalRound.sk:144
        SK_BITASSIGN(in, k, (((in >> k) & 1) ^ ((modpoly >> k) & 1))); // normalRound.sk:144
      }; // normalRound.sk:143
    }; // normalRound.sk:142
  } // normalRound.sk:142
  unsigned char out; // normalRound.sk:146
  out = 0; // normalRound.sk:146
  for (unsigned int i = 0; (i < 8); ++i) { // normalRound.sk:147
    { // normalRound.sk:147
      SK_BITASSIGN(out, i, ((in >> i) & 1)); // normalRound.sk:148
    }; // normalRound.sk:147
  }; // normalRound.sk:147
  _out_0 = out; // normalRound.sk:150
}
void GFM02(unsigned char& input, unsigned char& _out_0) { // normalRound.sk:153
  unsigned short int in; // normalRound.sk:154
  in = 0x0; // normalRound.sk:154
  for (unsigned int i = 0; (i < 8); ++i) { // normalRound.sk:156
    SK_BITASSIGN(in, (i + 1), ((input >> i) & 1)); // normalRound.sk:156
  }; // normalRound.sk:155
  unsigned short int modpoly; // normalRound.sk:165
  modpoly = 0x11b; // normalRound.sk:165
  for (unsigned int k = 0; (k < 8); ++k) { // normalRound.sk:168
    { // normalRound.sk:168
      if ((((in >> 8) & 1) == 1)) { // normalRound.sk:169
        { // normalRound.sk:169
          SK_BITASSIGN(in, k, (((in >> k) & 1) ^ ((modpoly >> k) & 1))); // normalRound.sk:170
        }; // normalRound.sk:169
      } // normalRound.sk:169
    }; // normalRound.sk:168
  }; // normalRound.sk:168
  unsigned char out; // normalRound.sk:173
  out = 0; // normalRound.sk:173
  for (unsigned int i = 0; (i < 8); ++i) { // normalRound.sk:174
    { // normalRound.sk:174
      SK_BITASSIGN(out, i, ((in >> i) & 1)); // normalRound.sk:175
    }; // normalRound.sk:174
  }; // normalRound.sk:174
  _out_0 = out; // normalRound.sk:177
}
void row1Fil(unsigned int& input, unsigned char& _out_0) { // normalRound.sk:180
  unsigned char in1; // normalRound.sk:181
  in1 = 0; // normalRound.sk:181
  unsigned char in2; // normalRound.sk:182
  in2 = 0; // normalRound.sk:182
  unsigned char in3; // normalRound.sk:183
  in3 = 0; // normalRound.sk:183
  unsigned char in4; // normalRound.sk:184
  in4 = 0; // normalRound.sk:184
  for (unsigned int i = 0; (i < 8); ++i) { // normalRound.sk:185
    { // normalRound.sk:185
      SK_BITASSIGN(in1, i, ((input >> i) & 1)); // normalRound.sk:186
      SK_BITASSIGN(in2, i, ((input >> (i + 8)) & 1)); // normalRound.sk:187
      SK_BITASSIGN(in3, i, ((input >> (i + 16)) & 1)); // normalRound.sk:188
      SK_BITASSIGN(in4, i, ((input >> (i + 24)) & 1)); // normalRound.sk:189
    }; // normalRound.sk:185
  }; // normalRound.sk:185
  unsigned char _frv_0; // normalRound.sk:191
  _frv_0 = 0; // normalRound.sk:191
  GFM02(in1, _frv_0); // normalRound.sk:191
  in1 = _frv_0; // normalRound.sk:191
  unsigned char _frv_1; // normalRound.sk:192
  _frv_1 = 0; // normalRound.sk:192
  GFM03(in2, _frv_1); // normalRound.sk:192
  in2 = _frv_1; // normalRound.sk:192
  unsigned char _frv_2; // normalRound.sk:193
  _frv_2 = 0; // normalRound.sk:193
  GFM01(in3, _frv_2); // normalRound.sk:193
  in3 = _frv_2; // normalRound.sk:193
  unsigned char _frv_3; // normalRound.sk:194
  _frv_3 = 0; // normalRound.sk:194
  GFM01(in4, _frv_3); // normalRound.sk:194
  in4 = _frv_3; // normalRound.sk:194
  _out_0 = (((in1 ^ in2) ^ in3) ^ in4); // normalRound.sk:195
}
void row2Fil(unsigned int& input, unsigned char& _out_0) { // normalRound.sk:198
  unsigned char in1; // normalRound.sk:199
  in1 = 0; // normalRound.sk:199
  unsigned char in2; // normalRound.sk:200
  in2 = 0; // normalRound.sk:200
  unsigned char in3; // normalRound.sk:201
  in3 = 0; // normalRound.sk:201
  unsigned char in4; // normalRound.sk:202
  in4 = 0; // normalRound.sk:202
  for (unsigned int i = 0; (i < 8); ++i) { // normalRound.sk:203
    { // normalRound.sk:203
      SK_BITASSIGN(in1, i, ((input >> i) & 1)); // normalRound.sk:204
      SK_BITASSIGN(in2, i, ((input >> (i + 8)) & 1)); // normalRound.sk:205
      SK_BITASSIGN(in3, i, ((input >> (i + 16)) & 1)); // normalRound.sk:206
      SK_BITASSIGN(in4, i, ((input >> (i + 24)) & 1)); // normalRound.sk:207
    }; // normalRound.sk:203
  }; // normalRound.sk:203
  unsigned char _frv_0; // normalRound.sk:209
  _frv_0 = 0; // normalRound.sk:209
  GFM01(in1, _frv_0); // normalRound.sk:209
  in1 = _frv_0; // normalRound.sk:209
  unsigned char _frv_1; // normalRound.sk:210
  _frv_1 = 0; // normalRound.sk:210
  GFM02(in2, _frv_1); // normalRound.sk:210
  in2 = _frv_1; // normalRound.sk:210
  unsigned char _frv_2; // normalRound.sk:211
  _frv_2 = 0; // normalRound.sk:211
  GFM03(in3, _frv_2); // normalRound.sk:211
  in3 = _frv_2; // normalRound.sk:211
  unsigned char _frv_3; // normalRound.sk:212
  _frv_3 = 0; // normalRound.sk:212
  GFM01(in4, _frv_3); // normalRound.sk:212
  in4 = _frv_3; // normalRound.sk:212
  _out_0 = (((in1 ^ in2) ^ in3) ^ in4); // normalRound.sk:213
}
void row3Fil(unsigned int& input, unsigned char& _out_0) { // normalRound.sk:218
  unsigned char in1; // normalRound.sk:219
  in1 = 0; // normalRound.sk:219
  unsigned char in2; // normalRound.sk:220
  in2 = 0; // normalRound.sk:220
  unsigned char in3; // normalRound.sk:221
  in3 = 0; // normalRound.sk:221
  unsigned char in4; // normalRound.sk:222
  in4 = 0; // normalRound.sk:222
  for (unsigned int i = 0; (i < 8); ++i) { // normalRound.sk:223
    { // normalRound.sk:223
      SK_BITASSIGN(in1, i, ((input >> i) & 1)); // normalRound.sk:224
      SK_BITASSIGN(in2, i, ((input >> (i + 8)) & 1)); // normalRound.sk:225
      SK_BITASSIGN(in3, i, ((input >> (i + 16)) & 1)); // normalRound.sk:226
      SK_BITASSIGN(in4, i, ((input >> (i + 24)) & 1)); // normalRound.sk:227
    }; // normalRound.sk:223
  }; // normalRound.sk:223
  unsigned char _frv_0; // normalRound.sk:229
  _frv_0 = 0; // normalRound.sk:229
  GFM01(in1, _frv_0); // normalRound.sk:229
  in1 = _frv_0; // normalRound.sk:229
  unsigned char _frv_1; // normalRound.sk:230
  _frv_1 = 0; // normalRound.sk:230
  GFM01(in2, _frv_1); // normalRound.sk:230
  in2 = _frv_1; // normalRound.sk:230
  unsigned char _frv_2; // normalRound.sk:231
  _frv_2 = 0; // normalRound.sk:231
  GFM02(in3, _frv_2); // normalRound.sk:231
  in3 = _frv_2; // normalRound.sk:231
  unsigned char _frv_3; // normalRound.sk:232
  _frv_3 = 0; // normalRound.sk:232
  GFM03(in4, _frv_3); // normalRound.sk:232
  in4 = _frv_3; // normalRound.sk:232
  _out_0 = (((in1 ^ in2) ^ in3) ^ in4); // normalRound.sk:233
}
void row4Fil(unsigned int& input, unsigned char& _out_0) { // normalRound.sk:237
  unsigned char in1; // normalRound.sk:238
  in1 = 0; // normalRound.sk:238
  unsigned char in2; // normalRound.sk:239
  in2 = 0; // normalRound.sk:239
  unsigned char in3; // normalRound.sk:240
  in3 = 0; // normalRound.sk:240
  unsigned char in4; // normalRound.sk:241
  in4 = 0; // normalRound.sk:241
  for (unsigned int i = 0; (i < 8); ++i) { // normalRound.sk:242
    { // normalRound.sk:242
      SK_BITASSIGN(in1, i, ((input >> i) & 1)); // normalRound.sk:243
      SK_BITASSIGN(in2, i, ((input >> (i + 8)) & 1)); // normalRound.sk:244
      SK_BITASSIGN(in3, i, ((input >> (i + 16)) & 1)); // normalRound.sk:245
      SK_BITASSIGN(in4, i, ((input >> (i + 24)) & 1)); // normalRound.sk:246
    }; // normalRound.sk:242
  }; // normalRound.sk:242
  unsigned char _frv_0; // normalRound.sk:248
  _frv_0 = 0; // normalRound.sk:248
  GFM03(in1, _frv_0); // normalRound.sk:248
  in1 = _frv_0; // normalRound.sk:248
  unsigned char _frv_1; // normalRound.sk:249
  _frv_1 = 0; // normalRound.sk:249
  GFM01(in2, _frv_1); // normalRound.sk:249
  in2 = _frv_1; // normalRound.sk:249
  unsigned char _frv_2; // normalRound.sk:250
  _frv_2 = 0; // normalRound.sk:250
  GFM01(in3, _frv_2); // normalRound.sk:250
  in3 = _frv_2; // normalRound.sk:250
  unsigned char _frv_3; // normalRound.sk:251
  _frv_3 = 0; // normalRound.sk:251
  GFM02(in4, _frv_3); // normalRound.sk:251
  in4 = _frv_3; // normalRound.sk:251
  _out_0 = (((in1 ^ in2) ^ in3) ^ in4); // normalRound.sk:252
}
void MixColumns(unsigned int* input, unsigned int* _out_0) { // normalRound.sk:255
  unsigned int output[4]; // normalRound.sk:256
  for (unsigned int __sa68 = 0; (__sa68 < 4); __sa68++) output[__sa68] = 0; // normalRound.sk:256
  for (unsigned int i = 0; (i < 4); ++i) { // normalRound.sk:257
    { // normalRound.sk:257
      unsigned int word; // normalRound.sk:258
      word = 0; // normalRound.sk:258
      for (unsigned int j = 0; (j < 32); ++j) { // normalRound.sk:259
        { // normalRound.sk:259
          SK_BITASSIGN(word, j, ((input[(((i * 32) + j) / 32)] >> (((i * 32) + j) % 32)) & 1)); // normalRound.sk:260
        }; // normalRound.sk:259
      }; // normalRound.sk:259
      unsigned int _frv_0; // normalRound.sk:262
      _frv_0 = 0; // normalRound.sk:262
      MixColumnsWord(word, _frv_0); // normalRound.sk:262
      word = _frv_0; // normalRound.sk:262
      for (unsigned int j = 0; (j < 32); ++j) { // normalRound.sk:263
        { // normalRound.sk:263
          SK_BITASSIGN(output[(((i * 32) + j) / 32)], (((i * 32) + j) % 32), ((word >> j) & 1)); // normalRound.sk:264
        }; // normalRound.sk:263
      }; // normalRound.sk:263
    }; // normalRound.sk:257
  }; // normalRound.sk:257
  for (unsigned int __sa69 = 0; (__sa69 < 4); __sa69++) _out_0[__sa69] = output[__sa69]; // normalRound.sk:267
}
void MixColumnsWord(unsigned int& input, unsigned int& _out_0) { // normalRound.sk:270
  unsigned char _frv_0; // normalRound.sk:271
  _frv_0 = 0; // normalRound.sk:271
  row1Fil(input, _frv_0); // normalRound.sk:271
  unsigned char o1; // normalRound.sk:271
  o1 = 0; // normalRound.sk:271
  o1 = _frv_0; // normalRound.sk:271
  unsigned char _frv_1; // normalRound.sk:272
  _frv_1 = 0; // normalRound.sk:272
  row2Fil(input, _frv_1); // normalRound.sk:272
  unsigned char o2; // normalRound.sk:272
  o2 = 0; // normalRound.sk:272
  o2 = _frv_1; // normalRound.sk:272
  unsigned char _frv_2; // normalRound.sk:273
  _frv_2 = 0; // normalRound.sk:273
  row3Fil(input, _frv_2); // normalRound.sk:273
  unsigned char o3; // normalRound.sk:273
  o3 = 0; // normalRound.sk:273
  o3 = _frv_2; // normalRound.sk:273
  unsigned char _frv_3; // normalRound.sk:274
  _frv_3 = 0; // normalRound.sk:274
  row4Fil(input, _frv_3); // normalRound.sk:274
  unsigned char o4; // normalRound.sk:274
  o4 = 0; // normalRound.sk:274
  o4 = _frv_3; // normalRound.sk:274
  unsigned int output; // normalRound.sk:275
  output = 0; // normalRound.sk:275
  for (unsigned int i = 0; (i < 8); ++i) { // normalRound.sk:276
    { // normalRound.sk:276
      SK_BITASSIGN(output, i, ((o1 >> i) & 1)); // normalRound.sk:277
      SK_BITASSIGN(output, (i + 8), ((o2 >> i) & 1)); // normalRound.sk:278
      SK_BITASSIGN(output, (i + 16), ((o3 >> i) & 1)); // normalRound.sk:279
      SK_BITASSIGN(output, (i + 24), ((o4 >> i) & 1)); // normalRound.sk:280
    }; // normalRound.sk:276
  }; // normalRound.sk:276
  _out_0 = output; // normalRound.sk:282
}
