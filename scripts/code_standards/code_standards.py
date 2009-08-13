#!/usr/bin/env python2.6
# -*- coding: utf-8 -*-

from __future__ import print_function
import optparse
import path_resolv
from path_resolv import Path

def check_file(f, show_info, override_ignores):
    text = f.read()
    if ("@code standards ignore file" in text) and (not override_ignores):
        return

    if "\r" in text:
        raise Exception("FATAL - dos endlines in %s" %(f))

    for i, line in enumerate(text.split("\n")):
        def warn(text):
            print("%30s %30s :%03d" %("WARNING - " + text, f, i))

        def info(text):
            if show_info:
                print("%30s %30s :%03d" %("INFO - " + text, f, i))

        if "\t" in line:
            warn("tabs present")
        # for now, ignore Eclipse blank comment lines
        if line.endswith(" ") and line.strip() != "*":
            warn("trailing whitespace")

        # the following can be ignored
        if "@code standards ignore" in line and not override_ignores:
            continue
        
        # spaces don't show up as much for variable indent
        relevant_line = line.lstrip('/').strip()
        if float(len(line)) * 0.7 + float(len(relevant_line)) * 0.3 > 90:
            warn("long line")

        # the following only apply to uncommented code
        if line.lstrip().startswith("//"):
            continue

        # the following do not apply to this file
        if f.endswith("build_util/code_standards.py"):
            continue

        if "System.exit" in line:
            warn("raw system exit")
        if "DebugOut.assertSlow" in line:
            info("debug assert slow call")

    def warn(text):
        print("%30s %30s" %("WARNING - " + text, f))

    if f.endswith(".java") and not "http://creativecommons.org/licenses/BSD/" in text:
        warn("no license")

def main(srcdir, file_extensions, **kwargs):
    assert type(file_extensions) == list
    for root, dirs, files in Path(srcdir).walk():
        for f in files:
            f = Path(root, f)
            if f.splitext()[-1][1:] in file_extensions:
                check_file(f, **kwargs)

if __name__ == "__main__":
    cmdopts = optparse.OptionParser(usage="%prog [options]")
    cmdopts.add_option("--srcdir", default=Path("."),
        help="source directory to look through")
    cmdopts.add_option("--file_extensions", default="java,scala,py,sh",
        help="comma-sepated list of file extensions")
    cmdopts.add_option("--show_info", action="store_true",
        help="show info for command")
    cmdopts.add_option("--override_ignores", action="store_true",
        help="ignore \"@code standards ignore [file]\"")
    options, args = cmdopts.parse_args()
    options.file_extensions = options.file_extensions.split(",")
    if not options.show_info:
        print("use --show_info to show more notices")
    main(**options.__dict__)
