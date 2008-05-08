import os, re, sys, tempfile
import sketch

# How many tests to run in parallel
NCPUS = 1

##-----------------------------------------------------------------------------
if __name__ == '__main__':
    cmd = sys.argv[1:]
    if not len (cmd):  cmd = ['psketch', '--verbosity', '1']
    logf = sys.stdout
    cwd = os.getcwd ()
    tmpfiles = []

    def test (t):
        _, outfile = tempfile.mkstemp ('', t)
        tmpfiles.append (outfile)
        cmdline = cmd + ['--output', outfile]
        path = 'regtest/'+ t
        return sketch.Test (path, cwd, logf, cmdline)

    try:
        sketch.runTests ([test (t) for t in os.listdir ('regtest')
                         if re.match (r'^miniTest[^.]+\.sk$', t)], NCPUS)
    finally:
        for tmpfile in tmpfiles:
            if os.access (tmpfile, os.R_OK):         os.remove (tmpfile)
            if os.access (tmpfile+".tmp", os.R_OK):  os.remove (tmpfile+".tmp")
