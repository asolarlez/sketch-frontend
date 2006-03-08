#ifndef NORMALROUND_H
#define NORMALROUND_H

extern void normalRoundsk(unsigned int* in, unsigned int* rkey, unsigned int* _out_0);
extern void normalRound(unsigned int* in, unsigned int* rkey, unsigned int* _out_0);
extern void ByteSub(unsigned int* in, unsigned int* _out_0);
extern void ShiftRows(unsigned int* in, unsigned int* _out_0);
extern void intToBit8(unsigned int& v, unsigned char& _out_0);
extern void ByteSubInd(unsigned char& in, unsigned char& _out_0);
extern void GFM01(unsigned char& in, unsigned char& _out_0);
extern void GFM03(unsigned char& input, unsigned char& _out_0);
extern void GFM02(unsigned char& input, unsigned char& _out_0);
extern void row1Fil(unsigned int& input, unsigned char& _out_0);
extern void row2Fil(unsigned int& input, unsigned char& _out_0);
extern void row3Fil(unsigned int& input, unsigned char& _out_0);
extern void row4Fil(unsigned int& input, unsigned char& _out_0);
extern void MixColumns(unsigned int* input, unsigned int* _out_0);
extern void MixColumnsWord(unsigned int& input, unsigned int& _out_0);

#endif
