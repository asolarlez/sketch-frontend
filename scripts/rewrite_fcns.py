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
import gatoatigrado_lib.subproc
from gatoatigrado_lib.util import GetSingletonEmptyException
import sys

REWR_STR = " /* automatically rewritten */"
FIRST_CHAR = re.compile(r"[^\s]")

def get_info(fname):
    assert fname.isfile()
    try:
        print("running %s" %(fname))
        SubProc(["java", "-classpath", "sketch-noarch.jar",
            "sketch.compiler.main.other.ParseFunctions",
            fname]).start_wait()
    except gatoatigrado_lib.subproc.ProcessException:
        print("failed with %s" %(fname))

couldntfindexception = False

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
                try:
                    get_singleton(v for v in fcns if v.name == fcn.impl).is_toplevel = True
                except GetSingletonEmptyException:
                    global couldntfindexception
                    couldntfindexception = True
                    print("couldn't find function %s for file %s" %(fcn.impl, fcn.srcFile))
        return fcns
    return dict(list(info).equiv_classes(lambda a: Path(str(a.srcFile)))).map_values(setImplements)

class RewriteException(Exception): pass

def try_rewrite(line, first_char, fcn):
    end_whitespace = re.search(r"\s*$", line).group(0)
    if fcn.is_generator and not fcn.is_toplevel:
        line = line[:first_char] + "generator " + line[first_char:].rstrip() + REWR_STR + end_whitespace
    elif not fcn.is_toplevel:
        if "static " in line:
            line = line.replace("static ", "", 1).rstrip() + REWR_STR + end_whitespace
        elif "static" in line:
            line = line.replace("static", "", 1).rstrip() + REWR_STR + end_whitespace
        else:
            raise RewriteException("no static keyword to delete")
    return line


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
    # sketchfiles = sketchfiles[:1000]

    # run the Java program
    outpath = Path("function_list.xml")
    outpath.exists() and outpath.unlink()
    #[get_info(v) for v in sketchfiles]
    for coarse_idx in range(0, len(sketchfiles), 100):
        subset = map(str, sketchfiles[coarse_idx:(coarse_idx + 100)])
        SubProc(["java", "-classpath", "sketch-noarch.jar",
            "sketch.compiler.main.other.ParseFunctions"] + subset).start_wait()

    fcns_by_fname = read_info(outpath)
    if couldntfindexception:
        print("(press enter to continue)", end="")
        sys.stdin.readline()
    
    for fname, fcns in fcns_by_fname.items():
        lines = open(fname).readlines()
        for fcn in fcns:
            success = False
            if not (0 <= fcn.line_idx < len(lines)):
                print("line offset not in range, assuming it came from a header file")
                success = True
            elif not str(fcn.name) in lines[fcn.line_idx]:
                print("function name not in line, assuming it came from a header file")
                success = True
            else:
                for err_offset in [0, -1, 1, -2, 2]:
                    if not (0 <= fcn.line_idx + err_offset < len(lines)):
                        continue
                    
                    line = lines[fcn.line_idx + err_offset]
                    if REWR_STR in line:
                        success = True
                        break

                    first_char = FIRST_CHAR.search(line)
                    if not first_char:
                        continue

                    try:
                        lines[fcn.line_idx + err_offset] = try_rewrite(line, first_char.start(), fcn)
                        success = True
                        break
                    except RewriteException, e:
                        print("WARNING / REWRITE EXCEPTION -- %s -- %s:%d"
                            %(fname, e, fcn.line_idx + err_offset))
                        continue
            if not success:
                print("    WARNING -- couldn't perform rewrite on all neighboring lines!")

        Path(fname + ".rewrite").write("".join(lines))

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
