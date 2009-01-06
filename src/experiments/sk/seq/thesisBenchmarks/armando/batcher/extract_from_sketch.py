#!/usr/bin/env python3.0
import re, sys
cregex = re.compile("if \(\(in_[012]\.sub<1>\(([0-9]+)\)\) > \(in_[012].sub<1>\(([0-9]+)\)\)\)")
print([tuple([int(c) for c in a]) for a in cregex.findall(open(sys.argv[1]).read())])
