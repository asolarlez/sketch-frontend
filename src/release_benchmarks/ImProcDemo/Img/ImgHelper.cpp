#include "ImgHelper.h"
#include "vops.h"


using namespace Img;

Image* readImage(const string& s){
		FILE *FU;
	
	unsigned char curBit;
	int tempInt=0;
	cout<<"Opening "<<s<<endl;
	FU=fopen(s.c_str(), "rb");
	unsigned char head[60];
	for(int x=0; x<54; x++){
		curBit=getc(FU);
		head[x]=curBit;
	}
	cout<<"Read header"<<endl;
	
	tempInt=head[18];
	tempInt=tempInt+(head[19]<<8);
	tempInt=tempInt+(head[20]<<16);
	tempInt=tempInt+(head[21]<<24);
	int c = tempInt;
	int width = c;

	tempInt=head[22];
	tempInt=tempInt+(head[23]<<8);
	tempInt=tempInt+(head[24]<<16);
	tempInt=tempInt+(head[25]<<24);
	int r=tempInt;
	int height = r;
	cout<<" Dims = "<<height<<" x "<<width<<endl;
	
	Image* rv = Image::create(r, c, NULL, 0);	
	rv->filename = s;
	memcpy(rv->head, head, 60*sizeof(unsigned char));
	
	
	
	int tempW=(width *3 + (4 - ((width*3) % 4))%4);
	unsigned char* tb = new unsigned char[tempW];
	for ( int y=0; y<height; y++){
		fread(tb, 1, tempW, FU);		
		for ( int x=0; x< tempW ; x+=1){				
			rv->im[y*width*3+x+0]= tb[x+0];
			//rv->im[y*width*3+x+1]= tb[x+1];
			//rv->im[y*width*3+x+2]= tb[x+2];
			//rv->im[y*width*3+x+3]= tb[x+3];			
		}		
	}
	delete tb;
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
	unsigned char* tb = new unsigned char[tempW];
	for ( int y=0; y<height; y++){
		for (int x=0; x< tempW ; x++){
			tb[x] = im->im[y*width*3+x]; 			
		}
		fwrite(tb, 1, tempW, FU);
	}
	delete tb;
		fclose(FU);

}
