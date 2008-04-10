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
## Fine-locking set

# - 2 threads, 2 concurrent ops
test ('fine-locking_list-based_set/soln_2_a-r.sk', extraopts=o),
test ('fine-locking_list-based_set/sketch1_2_a-r.sk', extraopts=o),
test ('fine-locking_list-based_set/sketch2_2_a-r.sk', extraopts=o),

# - 2 threads, 4 concurrent ops
test ('fine-locking_list-based_set/soln_2_a-a-r-r.sk', extraopts=o),
test ('fine-locking_list-based_set/sketch1_2_a-a-r-r.sk', extraopts=o),
test ('fine-locking_list-based_set/sketch2_2_a-a-r-r.sk', extraopts=o),

test ('fine-locking_list-based_set/soln_2_a-r-a-r.sk', extraopts=o),
test ('fine-locking_list-based_set/sketch1_2_a-r-a-r.sk', extraopts=o),
test ('fine-locking_list-based_set/sketch2_2_a-r-a-r.sk', extraopts=o),
)

        sketch.prettyPrint (sketch.runTests (tests, NCPUS))
    finally:
        for tmpfile in tmpfiles:
            if os.access (tmpfile, os.R_OK):         os.remove (tmpfile)
            if os.access (tmpfile+".tmp", os.R_OK):  os.remove (tmpfile+".tmp")
