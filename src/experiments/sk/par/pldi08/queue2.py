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
## Queue

# - 3 threads, 3 concurrent enqueues
test ('benchmarks/lock-free_queue/soln_3_e-e-e_ddd.sk', extraopts=o),
test ('benchmarks/lock-free_queue/sketchE1_3_e-e-e_ddd.sk', extraopts=o),
test ('benchmarks/lock-free_queue/sketchE2_3_e-e-e_ddd.sk', extraopts=o),

# - 3 threads, 3 concurrent dequeues
test ('benchmarks/lock-free_queue/soln_3_eee_d-d-d.sk', extraopts=o),
test ('benchmarks/lock-free_queue/sketchD1_3_eee_d-d-d.sk', extraopts=o),

# - 2 threads, 4 concurrent enqueues
test ('benchmarks/lock-free_queue/soln_2_e-e-e-e_dddd.sk', extraopts=o),
test ('benchmarks/lock-free_queue/sketchE1_2_e-e-e-e_dddd.sk', extraopts=o),
test ('benchmarks/lock-free_queue/sketchE2_2_e-e-e-e_dddd.sk', extraopts=o),

# - 2 threads, 4 concurrent dequeues
test ('benchmarks/lock-free_queue/soln_2_eeee_d-d-d-d.sk', extraopts=o),
test ('benchmarks/lock-free_queue/sketchD1_2_eeee_d-d-d-d.sk', extraopts=o),

# - 4 threads, 4 concurrent ops
test ('benchmarks/lock-free_queue/soln_4_e-d-e-d.sk', extraopts=o),
)

        sketch.prettyPrint (sketch.runTests (tests, NCPUS))
    finally:
        for tmpfile in tmpfiles:
            if os.access (tmpfile, os.R_OK):         os.remove (tmpfile)
            if os.access (tmpfile+".tmp", os.R_OK):  os.remove (tmpfile+".tmp")
