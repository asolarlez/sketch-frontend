#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

int main(int argc, char** argv)
{
	int n,x;
	char buf[100];
	char line[500];
	FILE *file;
	
	if(argc<2) {printf("You must supply the number of jobs\n");return 1;}
	n=atoi(argv[1]);
	if(n<0) {printf("Bad number of jobs (%d)\n",n);return 1;}
	
	for(int i=1;i<=n;i++) {
		sprintf(buf,"log%d.txt",i);
		file=fopen(buf,"rt");
		fgets(line,sizeof(line),file);
		fgets(line,sizeof(line),file);
		sscanf(line,"generating %s",buf);
		printf("%s,",buf);
		fgets(line,sizeof(line),file);
		if(!strstr(line,"Done")) {
			printf("noresult\n");
			goto closefile;
		}
		fgets(line,sizeof(line),file);
		if(sscanf(line,"Unknowns:%d",&x)!=1) {
			printf("failed\n");
			goto closefile;
		}
		printf("good");
		printf(",%d",x);
		fgets(line,sizeof(line),file);
		sscanf(line,"Iterations:%d",&x);
		printf(",%d",x);
		fgets(line,sizeof(line),file);
		sscanf(line,"Find time:%d",&x);
		printf(",%d",x);
		fgets(line,sizeof(line),file);
		sscanf(line,"Check time:%d",&x);
		printf(",%d",x);
		printf("\n");
closefile:
		fclose(file);
	}
	return 0;
}
