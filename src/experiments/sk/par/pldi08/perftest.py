import os, sys, tempfile
import sketch

# How many tests to run in parallel
NCPUS = 1

##-----------------------------------------------------------------------------
if __name__ == '__main__':
    cmd = sys.argv[1:]
    if not len (cmd):
        cmd = ['psketch', '--heapsize', '25', '--verbosity', '2',
                          '--timeout', '90', '--inlineamnt', '3',
                          '--schedlen', '1000']
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

    try:
        tests = (
test ('benchmarks/regtest/miniTest1.sk'),
test ('benchmarks/regtest/miniTest2.sk'),
test ('benchmarks/regtest/miniTest16.sk', extraopts=['--vectorszGuess', '16384']),
#test ('benchmarks/lock-free_queue/soln_2_e-e_dd.sk'),
#test ('benchmarks/lock-free_queue/enqueueSolution.sk'),
#test ('benchmarks/bigSketches/fineLockingSk1.sk'),
#test ('benchmarks/bigSketches/fineLockingSK2.sk'),
)
        sketch.prettyPrint (sketch.runTests (tests, NCPUS))
    finally:
        for tmpfile in tmpfiles:
            if os.access (tmpfile, os.R_OK):         os.remove (tmpfile)
            if os.access (tmpfile+".tmp", os.R_OK):  os.remove (tmpfile+".tmp")
