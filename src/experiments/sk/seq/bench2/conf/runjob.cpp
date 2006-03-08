#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <unistd.h>
#include <fcntl.h>
#include "pitimer.h"

#define MAX_JOBS 100

char avail[MAX_JOBS];
FILE *file;

void acquire_lock()
{
	while(true)
	{
		int fd=open("mylock.tmp",O_EXCL|O_CREAT);
		if(fd>=0) {
			close(fd);
			return;
		}
		PITimer::sleep_milli(60);
	}
}

void release_lock()
{
	unlink("mylock.tmp");
}

int main(int argc, char** argv)
{
	memset(avail,0,sizeof(avail));
	
	int n,na=0;
	char fn[30];
	char buf[200];
	
	acquire_lock();
	file=fopen("schedule.txt","rt");
	assert(file);
	while(fscanf(file,"%d",&n)==1)
	{
		if(n<0 || n>=MAX_JOBS) continue;
		avail[n]=1;
		na++;
	}
	fclose(file);
	
	if(na==0) return 0;
	for(n=0;n<MAX_JOBS;n++) if(avail[n]) break;
	assert(avail[n]);
	avail[n]=0;
	
	file=fopen("schedule.txt","wt");
	assert(file);
	for(int i=0;i<MAX_JOBS;i++) 
		if(avail[i])
			fprintf(file,"%d ",i);

	fclose(file);
	release_lock();
	
	sprintf(fn,"log%d.txt",n);
	fopen(fn,"wt");
	fprintf(file,"Opened by %s\n",(argc>=2?argv[1]:"?"));
	fclose(file);
	sprintf(buf,"bash work%d.sh >>%s",n,fn);
	return system(buf);
}
