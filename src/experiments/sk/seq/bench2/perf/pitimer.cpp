#include "pitimer.h"

#ifdef _WIN32
	#include <windows.h>
  #define TIMEDAT ((unsigned long*)timedata)
#else
	#include <time.h>
	#include <sys/time.h>
  #include <sys/types.h>
  #define TIMEDAT ((struct timeval*)timedata)
#endif

PITimer::PITimer()
{
  #ifdef _WIN32
    timedata=new unsigned long;
  #else
    timedata=new struct timeval;
  #endif
  reset(0);
}

PITimer::~PITimer()
{
  delete TIMEDAT;
}

void PITimer::reset(int v)
{
  prev=v;
  #ifdef _WIN32
    *TIMEDAT=GetTickCount();
  #else
    gettimeofday(TIMEDAT,0);
  #endif
}

void PITimer::adjustTime(int x) 
{
	if(x<0) 
		prev-=(unsigned long)(-x); 
	else 
		prev+=(unsigned long)x; 
}

unsigned long PITimer::getValue()
{
  #ifdef _WIN32
    unsigned long newval=GetTickCount();
    prev+=(newval-*TIMEDAT);
    *TIMEDAT=newval;
  #else
    struct timeval newval;
    gettimeofday(&newval,0);
    prev+=(newval.tv_sec-TIMEDAT->tv_sec)*1000L+(newval.tv_usec-TIMEDAT->tv_usec+500)/1000;
    *TIMEDAT=newval;
  #endif
  return prev;
}

void PITimer::sleep_milli(int ms)
{
  #ifdef _WIN32
    Sleep(ms);
  #else
    static struct timeval delay;
    delay.tv_sec=ms/1000;
    delay.tv_usec=(ms%1000)*1000;
    select(0,0,0,0,&delay);
  #endif
}
