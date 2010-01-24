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
import re

REWR_STR = " /* automatically rewritten */"
FIRST_CHAR = re.compile(r"[^\s]")

def get_info(fname):
    assert fname.isfile()
    SubProc(["java", "-classpath", "sketch-noarch.jar",
        "sketch.compiler.main.other.ParseFunctions",
        fname]).start_wait()

def read_info(fname):
    all = bindery.parse(fname.read()).vector
    info = [v for v in all.xml_children if v.xml_type == "element"]

    def setImplements(fcns):
        for fcn in fcns:
            fcn.name = str(fcn.nameStr)
            fcn.impl = getattr(fcn, "implName", None)
            fcn.impl = (str(fcn.impl) if fcn.impl else fcn.impl)
            fcn.line_idx = int(str(fcn.lineNum)) - 1
            fcn.is_generator = str(fcn.isGenerator) == "true"
        for fcn in fcns:
            fcn.is_toplevel = getattr(fcn, "is_toplevel", False) or bool(fcn.impl)
            if fcn.impl:
                get_singleton(v for v in fcns if v.name == fcn.impl).is_toplevel = True
        return fcns
    return dict(list(info).equiv_classes(lambda a: Path(str(a.srcFile)))).map_values(setImplements)

def main(*sketchfiles):
    # make sure sketch-noarch.jar exists
    assembly_file = Path("sketch-noarch.jar")
    if not assembly_file.isfile():
        raise Exception("please run renamer-script to generate sketch-noarch.jar")

    # use all sketch files which are subpaths of directory if they exist
    if not sketchfiles:
        sketchfiles = [v for v in Path(".").walk_files() if v.extension() in ["sk", "skh"]]
    else:
        sketchfiles = [Path(v) for v in sketchfiles]

    # run the Java program
    outpath = Path("function_list.xml")
    outpath.exists() and outpath.unlink()
    [get_info(v) for v in sketchfiles]

    fcns_by_fname = read_info(outpath)
    print(fcns_by_fname)
    
    return

    for fname, fcns in fcns_by_fname.items():
        lines = fname.read().split("\n")
        for fcn in fcns:
            line = lines[fcn.line_idx]
            line = line.replace("\r", "")
            if REWR_STR in line:
                continue
            first_char = FIRST_CHAR.search(line).start()
            if fcn.is_generator and not fcn.is_toplevel:
                line = line[:first_char] + "generator " + line[first_char:] + REWR_STR
            elif not fcn.is_toplevel:
                assert "static " in line
                line = line.replace("static ", "", 1)
            lines[fcn.line_idx] = line
        print("\n".join(lines))

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
