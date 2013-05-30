class Opr(object):
    """ Operators """
    def __init__(self, name, num, func, lb, ub, nan):
        self.name = name
        self.num = num
        self.func = func
        self.lowerbound = lb
        self.upperbound = ub
        # The 'NA/N' value
        self.nan = nan

    def __str__(self):
        return self.name + '@' + str(self.num) + '[' + str(self.lowerbound) + ',' + str(self.upperbound) + ']' + str(self.nan)

    def getName(self):
        return self.name

    def inrange(self, x):
        return x!=self.nan and x>=self.lowerbound and x<=self.upperbound
    
    def __call__(self, x, y):
        if self.inrange(x) and self.inrange(y):
            r = self.func(x, y)
            if self.inrange(r):
                return r
        return self.nan

"""
int PLUS=0;
int MINUS=1;
int TIMES=2;
int DIV=3;
int MOD=4;
"""

Lower = 0
Upper = 11
NaN = -222

Plus = Opr('PLUS', 0, lambda a,b: a+b, Lower, Upper, NaN)
Minus = Opr('MINUS', 1, lambda a,b: a-b, Lower, Upper, NaN)
Times = Opr('TIMES', 2, lambda a,b: a*b, Lower, Upper, NaN)
Div = Opr('DIV', 3, lambda a,b: NaN if b==0 else a/b, Lower, Upper, NaN)
Mod = Opr('MOD', 4, lambda a,b: NaN if b==0 else a%b, Lower, Upper, NaN)

#Oprs = [Plus, Times, Div, Mod]
Oprs = [Plus, Minus, Times, Div, Mod]
AllInt = xrange(Lower, Upper+1)

def gen():
    print 'include "generators.skh";'
    print
    print 'package generators;'
    print
    print 'int NaN =', NaN, ';'
    print 'void checkinbound(int a) {'
    print '    assert a == NaN || ( a >=', Lower, ' && a <=', Upper, ') : "Bounded Integer Overflow!";'
    print '}'
    print
    print 'int boundedopr(int op, int a, int b) {'
    print '    if (a == NaN || b == NaN) { return NaN; }'
    print '    checkinbound(a);'
    print '    checkinbound(b);'

    for op in Oprs:
        print '    if ( op ==', op.getName(), ') {'
        for b in AllInt:
            print '        if ( b ==', b, ') {'
            for a in AllInt:
                print '            if ( a==', a, ') { return', op(a, b), ' ; }'
            print '        }'
        print '    }'
    print '}'

if __name__ == '__main__':
    gen()
    
