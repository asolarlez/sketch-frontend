#ifndef PITIMER_H
#define PITIMER_H

class PITimer
{
  private:
    unsigned long prev;
    void *timedata;
  public:
    PITimer();
    ~PITimer();
    void reset(int v=0);
    unsigned long getValue();
    void adjustTime(int x);
    static void sleep_milli(int ms);
};

#endif
