#!/usr/bin/env python
# -*- coding: utf-8 -*-
# author: gatoatigrado (nicholas tung) [ntung at ntung]
# Copyright 2009 University of California, Berkeley

# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain a
# copy of the License at http://www.apache.org/licenses/LICENSE-2.0 .
from __future__ import division, print_function
from collections import namedtuple

try:
    from gatoatigrado_lib import (ExecuteIn, Path, SubProc, dict, get_singleton,
        list, memoize_file, pprint, process_jinja2, set, sort)
except:
    raise ImportError("please install gatoatigrado's utility library from "
            "bitbucket.org/gatoatigrado/gatoatigrado_lib")

modpath = Path(__file__).parent()
includepath = modpath.subpath("includes")

def main():
    from jinja2 import Environment, FileSystemLoader
    import jinja2.ext
    mvnfile = Path("pom.xml")
    assert mvnfile.exists(), "please run in project root (with ./pom.xml)"
    version = "1.6.0"

    paths = list(modpath.walk_files(["jinja2"]))
    dirs = frozenset(v.parent() for v in paths).union((includepath,))
    env = Environment(loader=FileSystemLoader(list(dirs)),
        trim_blocks=True, extensions=[jinja2.ext.do])
    env.globals.update({"version": version})

    for fname in Path("scripts").walk_files(["jinja2"]):
        output = env.get_template(fname.basename()).render()
        outname = fname.parent().subpath("final", fname.basename().strip(".jinja2"))
        Path(outname).write_if_different(output)
        if "launchers" in fname:
            Path(outname).chmod(0755)

if __name__ == "__main__":
    import optparse
    cmdopts = optparse.OptionParser(usage="%prog",
        description="process all .jinja2 files with jinja2")
    options, args = cmdopts.parse_args()
    main(*args, **options.__dict__)
