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

def main():
    mvnfile = Path("pom.xml")
    assert mvnfile.exists(), "please run in project root (with ./pom.xml)"
    from amara import bindery
    version = str(bindery.parse(mvnfile.read()).project.version)
    files = [v for v in Path("scripts").walk_files() if v.endswith(".jinja2")]
    def outfcn(fname, output):
        Path(fname.parent().subpath("final", fname.basename().strip(".jinja2"))).write(output)
    process_jinja2(files=files, glbls={"version": version}, output_fcn=outfcn)
    for fname in files:
        if "launchers" in fname and "unix" in fname:
            Path(fname.replace(".jinja2", "")).chmod(0755)

if __name__ == "__main__":
    import optparse
    cmdopts = optparse.OptionParser(usage="%prog",
        description="process all .jinja2 files with jinja2")
    options, args = cmdopts.parse_args()
    main(*args, **options.__dict__)
