#include "ImgHelper.h"
#include "vops.h"

namespace Img{
class Image{
  public:
  int  r;
  int  c;
  int*  im;
  Image(){}
  Image(  int  r_,   int  c_,   int*  im_, int im_len){
    r =  r_;
    c =  c_;
    im = new int [(3 * c_) * r_];
    CopyArr(im, im_, (3 * c_) * r_, im_len ); 
  }
string filename;
unsigned char head[60];
};
}

using namespace Img;

Image* readImage(const string& s){
		FILE *FU;
	Image* rv = new Image();	
	rv->filename = s;
	unsigned char curBit;
	int tempInt=0;
	cout<<"Opening "<<s<<endl;
	FU=fopen(s.c_str(), "rb");

	for(int x=0; x<54; x++){
		curBit=getc(FU);
		rv->head[x]=curBit;
	}
	cout<<"Read header"<<endl;
	
	tempInt=rv->head[18];
	tempInt=tempInt+(rv->head[19]<<8);
	tempInt=tempInt+(rv->head[20]<<16);
	tempInt=tempInt+(rv->head[21]<<24);
	rv->c = tempInt;
	int width = rv->c;

	tempInt=rv->head[22];
	tempInt=tempInt+(rv->head[23]<<8);
	tempInt=tempInt+(rv->head[24]<<16);
	tempInt=tempInt+(rv->head[25]<<24);
	rv->r=tempInt;
	int height = rv->r;
	cout<<" Dims = "<<height<<" x "<<width<<endl;
	rv->im = new int[width*height*3];
	int tempW=(width *3 + (4 - ((width*3) % 4))%4);
	for ( int y=0; y<height; y++){
		for ( int x=0; x< tempW ; x++){
			if (FU==NULL){
				cout<<"ERROR"<<endl;
				exit(0);
			}
			rv->im[y*width*3+x]=getc(FU);
			
		}
	}
	fclose(FU);
	cout<<"Done Reading"<<endl;
	return rv;
}

void copyMD(Img::Image* from, Img::Image* to){
	to->filename = from->filename;
	memcpy(to->head, from->head, 60*sizeof(unsigned char));
}

void writeImage(Image* im){
	FILE *FU;
	int tempInt=0;
	string s = im->filename;
	s += ".out.bmp";
	FU=fopen(s.c_str(), "wb");

	cout<<"Writing to file "<<s<<endl;
	int width = im->c;
	int height = im->r;

	im->head[18] = width & 0xFF;
	im->head[19] = (width >>8) & 0xFF;
	im->head[20] = (width >>16) & 0xFF;
	im->head[21] = (width >>24) & 0xFF;

	im->head[22] = height & 0xFF;
	im->head[23] = (height >>8) & 0xFF;
	im->head[24] = (height >>16) & 0xFF;
	im->head[25] = (height >>24) & 0xFF;

	for(int x=0; x<54; x++){
		putc(im->head[x], FU);
	}
	int tempW=(width * 3  + (4 - ((width*3) % 4))%4);
	for ( int y=0; y<height; y++)
		for (int x=0; x< tempW ; x++){
			putc(im->im[y*width*3+x], FU);
		}

		fclose(FU);

}