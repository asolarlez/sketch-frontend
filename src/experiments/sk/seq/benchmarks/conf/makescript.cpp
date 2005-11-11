#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

FILE *fin,*fout;
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
char *curname;

int makeTest(int n) {
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
	assert(fout);
	fprintf(fout,"echo generating %s\n",rname);
	fprintf(fout,"bash preproc.sh");
	for(int i=0;i<pnum;i++)
		fprintf(fout," -D %s %d",pname[i],pval[i][sel[i]]);
	fprintf(fout," %s.sk &> %s\n",curname,rname);
	fprintf(fout,"bash postproc.sh %s %s.report.txt\n",rname,curname);
}

int parseTest()
{
	pnum=0;
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
		else bail();
	}
	if(pnum==0) return 0;
	sprintf(tmp,reportName[0],curname);
	strcpy(rname,tmp);
	fprintf(fout,"rm -f %s.report.txt\n",curname);
	makeTest(0);
	fprintf(fout,"\n",curname);
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
			fout=fopen(outputFile,"wt");
			if(!fout) {printf("can't write to file %s\n",outputFile); return 1;}
			fprintf(fout,"cd ../../sbin\n");
			fprintf(fout,"source install.sh\n");
			fprintf(fout,"cd ../test/benchmarks\n\n");
		}
		else if(!strcmp(word,"reportname")) {
			if(sscanf(line,"%s %s %s %s",word,reportName[0],reportName[1],reportName[2])!=4) bail();
		}
		else if(!strcmp(word,"test")) {
			char testname[100];
			if(sscanf(line,"%s %s",word,testname)!=2) bail();
			curname=testname;
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
	if(argc<2) {printf("usage %s <config file>\n",argv[0]); return 0;}
	fin=fopen(argv[1],"rt");
	if(!fin) {printf("can't open file %s\n",argv[1]); return 1;}
	if(parseFile()) {printf("error, quitting\n"); return 2;}
	fclose(fin);
	fprintf(fout,"rm -f *.tmp\n");
	if(fout) fclose(fout);
	return 0;
}
