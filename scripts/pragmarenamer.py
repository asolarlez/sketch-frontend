#!/usr/bin/env python
# -*- coding: utf-8 -*-
# author: gatoatigrado (nicholas tung) [ntung at ntung]
# Copyright 2010 University of California, Berkeley

# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain a
# copy of the License at http://www.apache.org/licenses/LICENSE-2.0 .
from __future__ import division, print_function
from collections import namedtuple, defaultdict
from gatoatigrado_lib import (ExecuteIn, Path, SubProc, dict, get_singleton,
    list, memoize_file, pprint, process_jinja2, set, sort_asc, sort_desc)
import re

flagre = re.compile(r'--[^\s;\"]+')

pragma_map = {
        "--unrollamnt": "--bnd-unroll-amnt",
        "--simplifySpin": "--par-simplify-spin",
        "--verif": "--slv-verif",
        "--synth": "--slv-synth",
        "--keeptmpfiles": "--fe-keep-tmp",
        "--arrayOOBPolicy": "--sem-array-OOB-policy",
        "--outputtest": "--fe-output-test",
        "--verbosity": "--debug-verbosity",
        "--forcecodegen": "--fe-force-codegen",
        "--trace": "--debug-trace",
        "--seed": "--slv-seed",
        "--inlineamnt": "--bnd-inline-amnt",
        "--showphase": "--debug-show-phase",
        "--branchamnt": "--bnd-branch-amnt",
        "--main_names": "--sem-main-names",
        "--heapsize": "--bnd-heap-size",
        "--reorderEncoding": "--slv-reorder-encoding",
        "--vectorszGuess": "--par-vectorsz-guess",
        "--inbits": "--bnd-inbits",
        "--cex": "--debug-cex",
        "--sem-main-names": "--sem-main-names",
        "--outputcode": "--sem-main-names",
        "--intbits": "--bnd-intbits",
        "--cbits": "--bnd-cbits",
        "--keepasserts": "--fe-keep-asserts"
    }

files = Path('.').walk_files(['sk'])

def print_pragmas():
    lines = [line.strip() for file in files for line in open(file).readlines()]
    lines = [v.lstrip('pragma options ') for v in lines if v.startswith('pragma options ') and v.endswith(';')]
    result = defaultdict(lambda: 0)
    for flag in [v.strip() for line in lines for v in flagre.findall(line)]:
        result[flag] += 1
    print("=== flags and counts ===")
    result = dict(result)
    pprint(result)

    print("flags not in map: %s" %("\n".join(v for v in result
        if not v in pragma_map and not v in pragma_map.values())))

def do_renames():
    for fname in files:
        lines = open(fname).readlines()
        for i, line in enumerate(lines):
            if line.strip().startswith("pragma options "):
                for k, v in pragma_map.items():
                    line = line.replace(k, v)
                lines[i] = line
        fname.write("".join(lines))

def main(action):
    if action == "print_pragmas":
        print_pragmas()
    elif action == "do_renames":
        do_renames()

if __name__ == "__main__":
    import optparse
    cmdopts = optparse.OptionParser()
    # cmdopts.add_option("--myflag", action="store_true", help="set my flag variable")
    noptions = len(cmdopts.option_list) - 1
    varargs = bool(main.__code__.co_flags & 0x04)
    required_args = main.__code__.co_argcount - noptions
    if varargs:
        cmdopts.usage = "%%prog [options] <<list %s>>" %(main.__code__.co_varnames[0])
    else:
        cmdopts.usage = "%prog [options] " + " ".join(
            v for v in main.__code__.co_varnames[:required_args])
    options, args = cmdopts.parse_args()
    if not varargs and required_args != len(args):
        cmdopts.error("%d arguments required." % (required_args))
    main(*args, **options.__dict__)
