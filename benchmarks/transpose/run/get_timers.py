import re
import os
import matplotlib.pyplot  as pyplot

def timers_sum(fpath):
    result = 0.0
    with open(fpath) as infile:
        while True:
            line = infile.readline().lower()
            if len(line) == 0:
                break
            if line.find('warm') >= 0:
                continue
            for i in line.split():
                try:
                    result += float(i)
                except ValueError:
                    pass
    return result


FILE_NAME_RE = '[a-z\.]*([0-9]+)[a-z\.]*\.[a-z\.]*$'
FILE_NAME_MATCHER = re.compile(FILE_NAME_RE, re.IGNORECASE)

def get_timers(root):
    result = []
    for f in os.listdir(root):
        fpath = os.path.join(root, f)
        if os.path.isfile(fpath) and FILE_NAME_MATCHER.match(f) != None:
            result.append(timers_sum(fpath))
    return max(result)

def get_all_timers(root):
    result = {}
    for f in os.listdir(root):
        fpath = os.path.join(root, f)
        if os.path.isdir(fpath):
            timers = get_timers(fpath)
            result[int(f)] = timers
    return result


def main(root):
    result = get_all_timers(root)
    for k, v in result.items():
        if k < 64:
            d = 1024
        else:
            d = 2048
        v /= 20.0
        result[k] = (v, d**3/v)
    return result

def plot(root):
    result = main(root)
    x = []
    t = []
    o = []
    for k in sorted(result.keys()):
        x.append(k)
        t.append(result[k][0])
        o.append(result[k][1])
    pyplot.plot(x, o)
    pyplot.semilogx()
    pyplot.semilogy()
    pyplot.show()
    return x, o
