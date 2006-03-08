#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

int main(int argc, char** argv)
{
	int n;
	char buf[200];
	FILE *file;
	
	if(argc<2) {printf("You must supply the number of jobs\n");return 1;}
	n=atoi(argv[1]);
	if(n<0 || n>=100) {printf("Bad number of jobs (%d)\n",n);return 1;}
	
	file=fopen("schedule.txt","wt");
	for(int i=1;i<=n;i++) 
		fprintf(file,"%d ",i);
	fclose(file);
	
	sprintf(buf,"gexec -n %d bash runajob.sh",n);
	printf("Run: %s\n",buf);
//	return system(buf);
}
