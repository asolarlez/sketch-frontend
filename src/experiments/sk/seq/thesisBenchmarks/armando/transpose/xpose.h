#ifndef XPOSE_H
#define XPOSE_H

#include "bitvec.h"
#include "fixedarr.h"
extern void sse_transpose(fixedarr<int, 16>  mx_0, fixedarr<int, 16> & s_1);
extern void transpose(fixedarr<int, 16>  mx_0, fixedarr<int, 16> & s_1);

#endif
