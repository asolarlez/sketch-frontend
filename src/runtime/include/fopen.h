#include "stdlib.h"
#include "stdio.h"


namespace fileIO{

inline File* _fopen(char *filename){
	File *f = new File();
	FILE *inputFile;
	inputFile = std::fopen(filename, "r");

	if (!inputFile){
		printf("*** data could not be opened\n");
		return NULL;
	}
	else{
		f->cfile = inputFile; //both are pointers! 
		return f;
        }
}

}