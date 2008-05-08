import os, sys, tempfile, time

import sketch

# How many tests to run in parallel
NCPUS = 1

##-----------------------------------------------------------------------------
if __name__ == '__main__':
    print 'Renice me, please!\n'
    print 'sudo renice -15', os.getpid ()
    print ''
    sys.stdout.flush ()

    try:
        time.sleep (15)
    except KeyboardInterrupt:
        pass

    cmd = sys.argv[1:]
    if not len (cmd):
        cmd = ['psketch', '--heapsize', '10', '--verbosity', '2',
                          '--timeout', '60', '--seed', '10']
    logf = sys.stdout
    cwd = os.getcwd ()
    tmpfiles = []

    def test (path, workdir=None, logfile=logf, extraopts=[]):
        dir, file = os.path.split (path)
        if not workdir:  workdir = dir
        _, outfile = tempfile.mkstemp ('', file)
        tmpfiles.append (outfile)
        cmdline = cmd + extraopts + ['--output', outfile]
        return sketch.Test (path, workdir, logfile, cmdline)

    o = ['--vectorszGuess', '16384', 
         '--reorderEncoding', 'quadratic']

    try:
        tests = (
##-----------------------------------------------------------------------------
## Snacking philosophers
test ('benchmarks/dining_philosophers/soln_3_1.sk', extraopts=o),
test ('benchmarks/dining_philosophers/soln_3_2.sk', extraopts=o),
test ('benchmarks/dining_philosophers/soln_3_5.sk', extraopts=o),
test ('benchmarks/dining_philosophers/soln_4_1.sk', extraopts=o),
test ('benchmarks/dining_philosophers/soln_4_2.sk', extraopts=o),
test ('benchmarks/dining_philosophers/soln_4_3.sk', extraopts=o),
test ('benchmarks/dining_philosophers/soln_5_1.sk', extraopts=o),
test ('benchmarks/dining_philosophers/soln_5_2.sk', extraopts=o),
test ('benchmarks/dining_philosophers/soln_5_3.sk', extraopts=o),

test ('benchmarks/dining_philosophers/sketch_3_1.sk', extraopts=o),
test ('benchmarks/dining_philosophers/sketch_3_2.sk', extraopts=o),
test ('benchmarks/dining_philosophers/sketch_3_5.sk', extraopts=o),
test ('benchmarks/dining_philosophers/sketch_4_1.sk', extraopts=o),
test ('benchmarks/dining_philosophers/sketch_4_2.sk', extraopts=o),
test ('benchmarks/dining_philosophers/sketch_4_3.sk', extraopts=o),
test ('benchmarks/dining_philosophers/sketch_5_1.sk', extraopts=o),
test ('benchmarks/dining_philosophers/sketch_5_2.sk', extraopts=o),
test ('benchmarks/dining_philosophers/sketch_5_3.sk', extraopts=o),
)

        sketch.prettyPrint (sketch.runTests (tests, NCPUS))
    finally:
        for tmpfile in tmpfiles:
            if os.access (tmpfile, os.R_OK):         os.remove (tmpfile)
            if os.access (tmpfile+".tmp", os.R_OK):  os.remove (tmpfile+".tmp")
