import sys
import MySQLdb as sq
import re

db = None

db = sq.connect(host="mysql.csail.mit.edu", user="sketch", passwd="sketch", db="sketch")



class Harness:
    def __init__(self, name):
        self.name = name
        self.iters = 0;
        self.stime = 2000;
        self.ctime = 2000;
        self.nodes = 0;
        self.sclauses = 0;
        self.success = 'OK'
        self.lastIter = None
        self.itinfo = []
    def __str__(self):
        rv = self.name + " iters = " + str(self.iters) + " stime = " + str(self.stime) + " ctime = " + str(self.ctime) + " nodes = " + str(self.nodes)
        for x in self.itinfo:
            rv += "\n       " + str(x)
        return rv;


class ItInfo:
    def __init__(self, stime, ctime, sclauses):
        self.stime = stime
        self.ctime = ctime
        self.sclauses = sclauses
        self.sdag = 0
        self.cdag = 0
    def __str__(self):
        return "stime = " + str(self.stime) + " ctime = "  + str(self.ctime)




def saveStats(stat, db):    
    query = " INSERT INTO Run (Exp_id, run_date, benchmark, nodes, success, "
    query += "sclauses, stime, ctime, iters, tottime, cegistime, betime,maxmem, harnesses) VALUES "
    query += '(' + str(ExpID) + ', NOW(),' + '"' + stat.benchmark + '", '
    query += str(stat.nodes) + ', "' + stat.success + '", ' + str(stat.sclauses) + ', ' + str(stat.stime) + ', '
    query += str(stat.ctime) + ', ' + str(stat.iters) + ', ' + str(stat.tottime) + ', ' + str(stat.cegistime) + ', '
    query += str(stat.betime) + ', ' + str(stat.maxmem) +', ' + str(len(stat.harnesses)) + '); '    
    
    cur = db.cursor()
    cur.execute(query)
    query = 'SELECT MAX(id) as id FROM Run WHERE Exp_id='+ str(ExpID) +' AND benchmark="' + stat.benchmark + '";'
    cur.execute(query)
    rows = cur.fetchall()
    runid = rows[0][0]
    
    for h in stat.harnesses:
        query = 'INSERT INTO Harness(run_id, hname, iters, stime, ctime, sclauses, success) VALUES'
        query += '(' + str(runid) + ', "' + h.name + '", ' + str(h.iters) + ', ' + str(h.stime) + ', ' + str(h.ctime) + ', '
        query +=   str(h.sclauses) + ', "' + h.success + '");'
        cur.execute(query)
        query = 'SELECT MAX(id) as id FROM Harness WHERE run_id='+ str(runid) +';'
        print query
        cur.execute(query)
        rows = cur.fetchall()
        hid = rows[0][0]
        print "harness id is " + str(hid)
        for it in h.itinfo:
            query = 'INSERT INTO Iter(run_id, h_id, stime, ctime, sclauses) VALUES'
            query += '(' + str(runid) + ', ' + str(hid) + ', ' + str(it.stime) + ', ' + str(it.ctime) + ', ' + str(it.sclauses) + ');'
            print query
            cur.execute(query)
        


class StatFile:
    def __init__(self, file):
        self.stime = 0.0;
        self.ctime = 0.0;
        self.benchmark = "dummy";
        self.nodes = 0;
        self.success = "OK";
        self.sclauses = 0;
        self.tottime = 2000;
        self.cegistime= 2000;
        self.betime = 2000;
        self.iters = -1;
        self.maxmem = -1;
        self.harnesses = []
        for x in file:
            self.extract(x)

    def gp(self, line, field, ps, idx):
        p = re.compile(ps)
        m = p.search(line)
        if m != None:
            print m.group()
            setattr(self, field, m.group().split()[idx])            
            print field + ' = ' + getattr(self, field)
            return True
        return False

    def __str__(self):
        rv = " benchmark = " + str(self.benchmark);
        rv += "\n nodes = " + str(self.nodes);
        rv += "\n success = " + str(self.success);
        rv += "\n sclauses = " + str(self.sclauses);        
        rv += "\n stime = " + str(self.stime);
        rv += "\n ctime = " + str(self.ctime);
        rv += "\n iters = " + str(self.iters);
        rv += "\n tottime = " + str(self.tottime);
        rv += "\n cegistime = " + str(self.cegistime);
        rv += "\n betime = " + str(self.betime);
        rv += "\n harnesses = " + str(len(self.harnesses));
        for h in self.harnesses:
            rv +=  "\n    harness: " + str(h)
        return rv;
        

    def extract(self, line):
        if( self.gp(line, 'benchmark','Benchmark =.*', 2) ):
            return
        if( self.gp(line, 'nodes','Final Problem size: Problem nodes.*', 6) ):
            self.lastHarness.nodes = self.nodes
            return
        if( self.gp(line, 'success','Time limit exceeded!', 2) ):
            self.lastHarness.success = "TO"
            self.success = "TO"
            return
        if( self.gp(line, 'tmp',r'FAILED IN.*', 2) ):
            self.lastHarness.success = 'FAILED'
            self.lastHarness.iters = int(self.tmp)
            self.iters += int(self.tmp)
            self.success = "FAILED"
            return
        
        if( self.gp(line, 'sclauses','f#.*clauses.*', 4) ):
            self.lastHarness.sclauses = self.sclauses
            return
        if( self.gp(line, 'tottime','Total time =.*', 3) ):
            self.tottime = float(self.tottime) / 1000.0
            return
        if( self.gp(line, 'tmp','FIND TIME.*CHECK TIME.*', 2) ):
            self.lastHarness.stime = float(self.tmp) / 1000.0
            self.stime += float(self.tmp) / 1000.0
            self.gp(line, 'tmp','FIND TIME.*CHECK TIME.*', 5)
            self.lastHarness.ctime = float(self.tmp) / 1000.0
            self.ctime += float(self.tmp) / 1000.0
            return
        if( self.gp(line, 'cegistime',r'solution time \(s\).*', 4) ):
            return
        if( self.gp(line, 'betime',r'elapsed time \(s\).*', 4) ):
            return
        if( self.gp(line, 'maxmem',r'max memory.*', 5) ):
            return
        if( self.gp(line, 'tmp',r'GOT THE CORRECT ANSWER IN.*', 5) ):
            self.lastHarness.iters = int(self.tmp)
            self.iters += int(self.tmp)
            return
        if( self.gp(line, 'tmp',r' before  EVERYTHING:.*', 6) ):
            t = Harness(self.tmp)
            self.lastHarness = t;
            self.harnesses.append(t)            
            return
        if( self.gp(line, 'tmp','ftime=.*ctime=.*', 1) ):
            stime = float(self.tmp) / 1000.0
            self.gp(line, 'tmp','ftime=.*ctime=.*', 3)
            ctime = float(self.tmp) / 1000.0
            lh = ItInfo(stime, ctime, self.lastHarness.sclauses)
            self.lastHarness.itinfo.append( lh )            
            return
        
        

if(len(sys.argv) != 3):
    print "Usage: outputToDB.py filename exp_id"
    exit(1)


fname = str(sys.argv[1])
ExpID = int(sys.argv[2])

print 'Processing file ' + fname;

file = open(fname)

sf = StatFile(file)
print sf;
saveStats(sf, db)
