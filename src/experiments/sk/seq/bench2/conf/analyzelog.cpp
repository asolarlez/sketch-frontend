#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

char* NUM_UNK_STR="Original Num Variables";
char* ITER_STR="GOT THE CORRECT ANSWER IN";
char* FIND_STR="FIND TIME";
char* CHECK_STR="CHECK TIME";

FILE *fin;
char buffer[1000];

int extractNum(char* line, char* sub)
{
	char *p=strstr(line,sub);
	if(p==NULL) return -1;
	p+=strlen(sub);
	return atoi(p);
}

int main(int argc, char **argv)
{
	int unk;
	int iter=0;
	int find,check;
	
	assert(argc>=2);
	fin=fopen(argv[1],"rt");
	assert(fin);
	while(fgets(buffer,sizeof(buffer),fin))
	{
		int t=extractNum(buffer,NUM_UNK_STR);
		if(t>0) unk=t;
		iter=extractNum(buffer,ITER_STR);
		if(iter>0)
		{
			fgets(buffer,sizeof(buffer),fin);
			find=extractNum(buffer,FIND_STR);
			check=extractNum(buffer,CHECK_STR);
			printf("Unknowns:   %d\n",unk);
			printf("Iterations: %d\n",iter);
			printf("Find time:  %d\n",find);
			printf("Check time: %d\n",check);
			break;
		}
	}
	if(iter<=0) printf("Did not resolve!\n");
	fclose(fin);
	return 0;
}
