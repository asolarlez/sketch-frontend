import os, re, subprocess, sys, tempfile
from threading import Thread
from Queue import Queue

# How many tests to run in parallel
NCPUS = 2

##-----------------------------------------------------------------------------
class Test:
    def __init__ (self, file, workdir, logfile, cmd):
        '''Create a tester of CWD/file that runs from WORKDIR.'''
        assert file and workdir and logfile
        workdir = os.path.abspath (workdir)
        path = os.path.abspath (file)
        assert os.access (path, os.R_OK)

        self.name = os.path.basename (path)
        self.logfile = logfile
        self.workdir = workdir
        self.args = list (cmd) + [path]

    def run (self):
        print >>self.logfile, 'Running test', self.name, '...'
        self.logfile.flush ()
        p = subprocess.Popen (self.args, cwd=self.workdir,
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                              close_fds=True, universal_newlines=True)
        self.out, self.err = p.communicate ()
        self.exitcode = p.returncode

        print >>self.logfile, '  ... test', self.name, 'done!'
        self.logfile.flush ()

        if self.exitcode or self.err:
            print >>sys.stderr, '  Test', self.name, 'failed.  Error output:'
            print >>sys.stderr, self.err

        return self

##-----------------------------------------------------------------------------
_NL = r'(?:\r\n|\n|\r)'
_FLOAT = r'(\d+\.\d+)'
_INT = r'(\d+)'

class TestStats:
    def __init__ (self, test):
        assert test.out
        self.test = test
        self.stats = { }
        self.parseStats ()

    def parseStats (self):
        self.getSummaryStats ()
        self.getSynthStats ()
        self.getVerifStats ()
        self.getFEStats ()

    def getSummaryStats (self):
        # inner text, except the first match, is for labeling only
        m = re.search (
            r'Solved.+?(true|false)'+          _NL +
            r'.+iterations.+?'+       _INT   + _NL +
            r'.+memory usage.+?'+     _FLOAT + _NL +
            r'.+elapsed time.+?'+     _FLOAT + _NL +
            r'.+% frontend.+?'+       _FLOAT + _NL +
            r'.+% verification.+?'+   _FLOAT + _NL +
            r'.+% synthesis.+?'+      _FLOAT + _NL,
            self.test.out)
        assert m

        for i, stat in enumerate (('allSolved', 'allIters', 'allMaxmem', 'allElapsed',
                                   'fe%All', 'verif%All', 'synth%All')):
            self.stats[stat] = m.group (i+1)

    def getSynthStats (self):  self.getSolverStats (r'Synthesizer', 'synth')
    def getVerifStats (self):  self.getSolverStats (r'Verifier', 'verif')

    def getSolverStats (self, label, pfx):
        m = re.search (
            r'.+'+ label +r'.+'+               _NL +r'.+'+ _NL +
            r'.+calls.+?'+            _INT   + _NL +
            r'.+elapsed.+?'+          _FLOAT + _NL +
            r'.+model building.+?'+   _FLOAT + _NL +
            r'.+solution.+?'+         _FLOAT + _NL +
            r'.+memory usage.+?'+     _FLOAT + _NL +
            r'.+elapsed time.+?'+     _FLOAT + _NL +
            r'.+model building.+?'+   _FLOAT + _NL +
            r'.+solution.+?'+         _FLOAT + _NL +
            r'.+memory usage.+?'+     _FLOAT + _NL,
            self.test.out)
        assert m

        for i, stat in enumerate (('Calls', 'Elapsed', 'Model', 'Soln', 'Maxmem',
                                   'AvgElapsed', 'AvgModel', 'AvgSoln', 'Avgmem')):
            self.stats[pfx+stat] = m.group (i+1)

    def getFEStats (self):
        m = re.search (
            r'.+Frontend statistics.+'+        _NL +
            r'.+elapsed time.+?'+     _FLOAT + _NL +
            r'.+memory usage.+?'+     _FLOAT + _NL,
            self.test.out)
        assert m

        for i, stat in enumerate (('feElapsed', 'feMaxmem')):
            self.stats[stat] = m.group (i+1)

def runTests (tests, maxthreads=1):
    assert len (tests) > 0 and maxthreads > 0

    doneQ = Queue ()

    class TestThread (Thread):
        def __init__ (self, test):
            Thread.__init__ (self)
            self.test = test
        def run (self):
            try:
                self.test.run ()
            finally:
                doneQ.put (self.test)

    stats = []
    i = 0    # 'i' is the next thread to run

    for i in xrange (min (maxthreads, len (tests))):
        TestThread (tests[i]).start ()

    while True:
        stats.append (TestStats (doneQ.get ()))
        if len (stats) == len (tests):  break
        i += 1
        if i < len (tests):  TestThread (tests[i]).start ()

    return stats


def prettyPrint (stats, out=sys.stdout):
    '''Output a tabular representation of the list of TestStats, STATS.'''
    cols = { 'allElapsed':    'Total time (s)',
             'allMaxmem':     'Max mem (MiB)',
             'allIters':      'Iterations',
             'fe%All':        'Frontend time (%)',
             'feMaxmem':      'Frontend mem (MiB)',
             'synth%All':     'Synth time (%)',
             'synthModel':    'Synth model (s)',
             'synthSoln':     'Synth soln (s)',
             'synthMaxmem':   'Synth mem (MiB)',
             'verif%All':     'Verif time (%)',
             'verifModel':    'Verif model (s)',
             'verifSoln':     'Verif soln (s)',
             'verifMaxmem':   'Verif mem (MiB)' }
    colwidth = 20

    def adj (s):  return ' ' * (colwidth - len (s))
    def lfill (s):  return s + adj (s)
    def rfill (s):  return adj (s) + s

    out.write (lfill ('Name'))
    for col, head in sorted (cols.iteritems ()):
        out.write (' || '+ rfill (head))
    print >>out
    print >>out, '-' * colwidth * (len (cols) + len (' || '))
    for s in stats:
        out.write (lfill (s.test.name))
        for col, head in sorted (cols.iteritems ()):
            out.write ('  | '+ rfill (s.stats[col]))
        print >>out

##-----------------------------------------------------------------------------
if __name__ == '__main__':
    cmd = sys.argv[1:]
    if not len (cmd):
        cmd = ['sketch', '--heapsize', '25', '--verbosity', '1',
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
        return Test (path, workdir, logfile, cmdline)

    try:
        tests = (
#test ('regtest/miniTest1.sk'),
#test ('regtest/miniTest2.sk'),
test ('regtest/miniTest16.sk', extraopts=['--vectorszGuess', '16384']),
#test ('lock-free_queue/soln_2_e-e_dd.sk'),
#test ('lock-free_queue/enqueueSolution.sk'),
#test ('bigSketches/fineLockingSk1.sk'),
#test ('bigSketches/fineLockingSK2.sk'),
)
        prettyPrint (runTests (tests, NCPUS))
    finally:
        for tmpfile in tmpfiles:
            if os.access (tmpfile, os.R_OK):         os.remove (tmpfile)
            if os.access (tmpfile+".tmp", os.R_OK):  os.remove (tmpfile+".tmp")
