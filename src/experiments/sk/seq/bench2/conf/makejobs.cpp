#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

FILE *fin,*fout=0;
int ln=0;
char buffer[1000];
char tmp[100];
char outputFile[100];
char reportName[3][100];
#define bail() do{printf("error on line %d\n",ln); if(fout) fclose(fout); return 1;}while(false)

char* getLine()
{
	if(!fgets(buffer,sizeof(buffer),fin)) return 0;
	ln++;
	char *line=buffer;
	while(*line==' ') line++;
	char *p=line+strlen(line)-1;
	while(p>=line && (*p==13 || *p==10)) *(p--)=0;
	p=strstr(line,"//");
	if(p) *p=0;
	return line;
}

int pnum;
int pvn[5];
int pval[5][20];
char pname[5][100];
int sel[5];
char rname[200];
char skname[200];
char gflags[500];
char lflags[500];
char *curname;
int njobs=0;

int makeTest(int n) {
	FILE *f;
	char buf[30];
//printf("makeTest %d\n",n);

	if(n<pnum) {
		int l=strlen(rname);
		for(int i=0;i<pvn[n];i++) {
			sel[n]=i;
			sprintf(tmp,reportName[1],pname[n],pval[n][i]);
			strcat(rname,tmp);
			makeTest(n+1);
			rname[l]=0;
		}
		return 0;
	}
	strcat(rname,reportName[2]);

	njobs++;
	sprintf(buf,"work%d.sh",njobs);
	f=fopen(buf,"wt");
	assert(f);

	fprintf(f,"cd /home/eecs/asolar/research/bodik/edu.berkeley.asolar.streamBitFrontend/sbin\n");
	fprintf(f,"source install.sh\n");
	fprintf(f,"cd ~/sketch\n\n");
	fprintf(f,"echo generating %s\n",rname);
	fprintf(f,"rm -f %s.sk\n",rname);
	fprintf(f,"cp %s %s.sk\n",skname,rname);
	fprintf(f,"bash preproc.sh");
	for(int i=0;i<pnum;i++)
		fprintf(f," -D %s %d",pname[i],pval[i][sel[i]]);
	fprintf(f," %s %s ",gflags,lflags);
	fprintf(f," %s.sk &> %s\n",rname,rname);
	fprintf(f,"rm -f %s.sk\n",rname);
	fprintf(f,"echo Done.\n",rname);
	fprintf(f,"bash conf/postproc.sh %s\n",rname);

	fclose(f);
}

int parseTest()
{
	pnum=0;
	lflags[0]=0;
	char *line;
//printf("doing test %s\n",curname);
	while(line=getLine()) {
//printf("line: %s\n",line);
		if(line[0]==0) break;
		char word[100];
		if(sscanf(line,"%s",word)!=1) break;
		if(!strcmp(word,"param")) {
			assert(pnum<5);
			char *t=strtok(line," ");
			t=strtok(0," ");
			if(!t) bail();
			strcpy(pname[pnum],t);
//printf("var: %s\n",pname[pnum]);
			pvn[pnum]=0;
			while(t=strtok(0," ")) {
				int v=atoi(t);
//printf("val: %d\n",v);
				assert(pvn[pnum]<20);
				pval[pnum][pvn[pnum]++]=v;
			}
			pnum++;
		}
		else if(!strcmp(word,"file")) {
			sscanf(line,"%s %s",word,skname);
		}
		else if(!strcmp(word,"flags")) {
			assert(line[5]!=0);
			strcpy(lflags,line+6);
		}
		else bail();
	}
	if(pnum==0) return 0;
	sprintf(tmp,reportName[0],curname);
	strcpy(rname,tmp);
	makeTest(0);
	return 0;
}

int parseFile()
{
	int nt=0;
	char *line=0;
	while(line=getLine())
	{
		if(line[0]==0) continue;
		char word[100];
		if(sscanf(line,"%s",word)!=1) continue;
		if(!strcmp(word,"output")) {
			if(sscanf(line,"%s %s",word,outputFile)!=2) bail();
		}
		else if(!strcmp(word,"reportname")) {
			if(sscanf(line,"%s %s %s %s",word,reportName[0],reportName[1],reportName[2])!=4) bail();
		}
		else if(!strcmp(word,"flags")) {
			assert(line[5]!=0);
			strcpy(gflags,line+6);
		}
		else if(!strcmp(word,"test")) {
			char name[100];
			if(sscanf(line,"%s %s",word,name)!=2) bail();
			curname=name;
			strcpy(skname,name);
			strcat(skname,".sk");
			nt++;
			if(parseTest()) return 1;
		}
		else bail();
	}
	return 0;
}

int main(int argc, char **argv)
{
	outputFile[0]=0;
	reportName[0][0]=0;
	gflags[0]=0;
	if(argc<2) {printf("usage %s <config file>\n",argv[0]); return 0;}
	fin=fopen(argv[1],"rt");
	if(!fin) {printf("can't open file %s\n",argv[1]); return 1;}
	if(parseFile()) {printf("error, quitting\n"); return 2;}
	fclose(fin);
	return 0;
}
