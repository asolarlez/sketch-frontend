import os, re, subprocess, sys

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
        self.logfile.write ('\nRunning test %s ...'% (self.name))
        self.logfile.flush ()
        p = subprocess.Popen (self.args, cwd=self.workdir,
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                              close_fds=True, universal_newlines=True)
        p.wait ()
        print >>self.logfile, 'done!'

        self.exitcode = p.returncode
        self.out = p.stdout.read ()
        self.err = p.stderr.read ()

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
                                   'all%frontend', 'all%verif', 'all%synth')):
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


def runTests (tests):
    return [TestStats (t.run ()) for t in tests]

def _usage ():
    print >>sys.stderr, '''
Usage:
     python %s sketchCmd...
'''% (sys.argv[0])
    sys.exit (1)

##-----------------------------------------------------------------------------
if __name__ == '__main__':
    cmd = sys.argv[1:]
    if not len (cmd):  _usage ()
    logf = sys.stderr
    cwd = os.getcwd ()

    tests = (
Test ('regtest/miniTest1.sk',                   '..',  logf,
      cmd),
Test ('regtest/miniTest16.sk',                   cwd,  logf,
      cmd + ['--vectorszGuess', '16384']),
Test ('lock-free_queue/enqueueSolution.sk',      cwd,  logf,
      cmd),
#Test ('bigSketches/fineLockingSk1.sk',          '..',  logf,
#      cmd)
)

    for stat in runTests (tests):
        print '\nResults for test', stat.test.name
        for k, v in sorted (stat.stats.iteritems ()):
            print '  %s = %s' %(k, v)
