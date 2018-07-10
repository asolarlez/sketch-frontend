import sys

there = sys.argv[1]
found = sys.argv[2]


def loadFile(fname):
    with open(fname) as f:
        smalldb = {}
        for line in f:
            tt = line.split(':')
            name = tt[0]
            smalldb.setdefault(name, set()).add(line)
        return smalldb

fthere = loadFile(there)
ffound = loadFile(found)

for x in fthere:
    therelist = fthere[x]
    if x in ffound:
        foundlist = ffound[x]
        intersect = therelist & foundlist
        if len(intersect) == 0:
            print("PROBLEM: " + x + " THERE " + str(therelist) + " FOUND " + str(foundlist))
    else:
        print("PROBLEM: " + x)
