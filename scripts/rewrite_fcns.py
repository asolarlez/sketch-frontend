#!/usr/bin/env python
# -*- coding: utf-8 -*-
# author: gatoatigrado (nicholas tung) [ntung at ntung]
# Copyright 2010 University of California, Berkeley

# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain a
# copy of the License at http://www.apache.org/licenses/LICENSE-2.0 .
from __future__ import division, print_function
from collections import namedtuple
from amara import bindery
from gatoatigrado_lib import (ExecuteIn, Path, SubProc, dict, get_singleton,
    list, memoize_file, pprint, process_jinja2, set, sort_asc, sort_desc)

def get_info(fname):
    SubProc(['mvn', '-e', 'exec:java',
        "-Dexec.mainClass=sketch.compiler.main.other.ParseFunctions",
        "-Dexec.args=%s" % (fname)]).start_wait()

def read_info(fname):
    all = bindery.parse(fname.read()).vector
    import ipdb; ipdb.set_trace()

def main(*sketchfiles):
    with SubProc(["mvn", "-e", "compile"]).wait_block():
        if not sketchfiles:
            sketchfiles = [v for v in Path(".").walk_files() if v.extension() in ["sk", "skh"]]
        else:
            sketchfiles = [Path(v) for v in sketchfiles]
    outpath = Path("function_list.xml")
    outpath.exists() and outpath.unlink()
    [get_info(v) for v in sketchfiles[:10]]
    read_info(outpath)

if __name__ == "__main__":
    import optparse
    cmdopts = optparse.OptionParser()
    noptions = len(cmdopts.option_list) - 1
    varargs = bool(main.__code__.co_flags & 0x04)
    required_args = main.__code__.co_argcount - noptions
    if varargs:
        cmdopts.usage = "%%prog [options] <<list %s>>" % (main.__code__.co_varnames[0])
    else:
        cmdopts.usage = "%prog [options] " + " ".join(
            v for v in main.__code__.co_varnames[:required_args])
    options, args = cmdopts.parse_args()
    if not varargs and required_args != len(args):
        cmdopts.error("%d arguments required." % (required_args))
    main(*args, **options.__dict__)
