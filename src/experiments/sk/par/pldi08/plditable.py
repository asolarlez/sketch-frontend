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

    lfqo = ['--vectorszGuess', '16384', 
            '--reorderEncoding', 'quadratic']

    baro = ['--vectorszGuess', '16384', 
            '--reorderEncoding', 'exponential']

    fnso = lfqo
    lazo = fnso
    philo = lazo

    try:
        tests = (
##-----------------------------------------------------------------------------
## queue
test ('benchmarks/lock-free_queue/sketchE1_2_e-e-d-d.sk', extraopts=lfqo), 
test ('benchmarks/lock-free_queue/sketchE1_2_e-d-e-d.sk', extraopts=lfqo),
test ('benchmarks/lock-free_queue/sketchE1_3_e-e-e_ddd.sk', extraopts=lfqo),

test ('benchmarks/lock-free_queue/sketchD1E1_2_e-e-d-d.sk', extraopts=lfqo),
test ('benchmarks/lock-free_queue/sketchD1E1_2_e-d-e-d.sk', extraopts=lfqo),

test ('benchmarks/lock-free_queue/sketchE2_2_e-d-e-d.sk', extraopts=lfqo),
test ('benchmarks/lock-free_queue/sketchE2_3_e-e-e_ddd.sk', extraopts=lfqo),

test ('benchmarks/lock-free_queue/sketchD1E2_2_e-d-e-d.sk', extraopts=lfqo),


## barrier
test ('benchmarks/barrier/b1_3_2.sk', extraopts=baro),
test ('benchmarks/barrier/b1_3_3.sk', extraopts=baro),

test ('benchmarks/barrier/b2_2_3.sk', extraopts=baro),
test ('benchmarks/barrier/b2_3_3.sk', extraopts=baro),


## fine-locking set
test ('benchmarks/fine-locking_list-based_set/sketch1_2_a-r-a-r.sk', extraopts=fnso),
test ('benchmarks/fine-locking_list-based_set/sketch1_3_a-r-a-r-a-r.sk', extraopts=fnso),
test ('benchmarks/fine-locking_list-based_set/sketch1_4_a-r-a-r.sk', extraopts=fnso),
test ('benchmarks/fine-locking_list-based_set/sketch1_2_a-r-a-r-a-r-a-r.sk', extraopts=fnso),
test ('benchmarks/fine-locking_list-based_set/sketch1_2_a-a-a-a-r-r-r-r.sk', extraopts=fnso),

test ('benchmarks/fine-locking_list-based_set/sketch2_2_a-r-a-r.sk', extraopts=fnso),
test ('benchmarks/fine-locking_list-based_set/sketch2_3_a-r-a-r-a-r.sk', extraopts=fnso),
test ('benchmarks/fine-locking_list-based_set/sketch2_4_a-r-a-r.sk', extraopts=fnso),
test ('benchmarks/fine-locking_list-based_set/sketch2_2_a-r-a-r-a-r-a-r.sk', extraopts=fnso),
test ('benchmarks/fine-locking_list-based_set/sketch2_2_a-a-a-a-r-r-r-r.sk', extraopts=fnso),


## lazy set
test ('benchmarks/lazy_list-based_set/lzsketch1_2_a-a-r-r.sk', extraopts=lazo),
test ('benchmarks/lazy_list-based_set/lzsketch1_2_a-r-a-r.sk', extraopts=lazo),


## dining philosophers
test ('benchmarks/dining_philosophers/sketch_3_5.sk', extraopts=philo),
test ('benchmarks/dining_philosophers/sketch_4_3.sk', extraopts=philo),
test ('benchmarks/dining_philosophers/sketch_5_3.sk', extraopts=philo),
)

        sketch.prettyPrint (sketch.runTests (tests, NCPUS))
    finally:
        for tmpfile in tmpfiles:
            if os.access (tmpfile, os.R_OK):         os.remove (tmpfile)
            if os.access (tmpfile+".tmp", os.R_OK):  os.remove (tmpfile+".tmp")
