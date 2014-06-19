#ifndef IMG_HELPER_H
#define IMG_HELPER_H

#include <string>
#include <fstream>
#include <cstdio>
#include <cstdlib>
#include <iostream>

namespace Img{
class Image;
}

using namespace std;

Img::Image* readImage(const string& s);

void writeImage(Img::Image* im);

void copyMD(Img::Image* from, Img::Image* to);
#endif
