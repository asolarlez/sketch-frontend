#!/usr/bin/env python2.6
# -*- coding: utf-8 -*-
# NOTE - this file is mostly for examining compilation results,
# not for building the project

from __future__ import print_function
from collections import namedtuple
import optparse, os, shutil, subprocess, types

home=os.path.expanduser("~")
assert os.path.isdir(home), "home directory must be present"

class Path(str):
    def __new__(cls, *unix_paths):
        args = []
        for path in unix_paths:
            if not path:
                raise Exception("no path", path)
            v = path[1:].split("/") # abs path handling
            v[0] = path[0] + v[0]
            args.extend(v)
        args[0] = home if args[0] == "~" else args[0]
        path = os.path.realpath(os.path.join(*args))
        return str.__new__(cls, path)

    def __getattr__(self, key):
        if key in dir(os):
            fcn = getattr(os, key)
        elif key in dir(shutil):
            fcn = getattr(shutil, key)
        else:
            assert key in dir(os.path), key
            fcn = getattr(os.path, key)
        assert (type(fcn) == types.FunctionType or
            type(fcn) == types.BuiltinFunctionType)
        def inner(*argv, **kwargs):
            result = fcn(self, *argv, **kwargs)
            if type(result) == str:
                pathresult = Path(result)
                if pathresult.exists() or result == str(pathresult):
                    return pathresult
            return result
        return inner

    def subpath(self, *argv):
        return Path(self, *argv)

    def remove_tree(self):
        self.rmtree() if self.isdir() else self.unlink()

    def read(self):
        return open(self, "r").read()

    def extension(self):
        return self.splitext()[1][1:]

    def __repr__(self): return "Path[\"%s\"]" %(self)

    @classmethod
    def pathjoin(cls, *argv):
        assert all([type(v) != list for v in argv]), "pathjoin takes varargs"
        return os.path.pathsep.join([str(v) for v in argv])

class PathResolver:
    def get_file(self, basename):
        raise NotImplementedError("abstract")

class DirArrayPathResolver:
    def __init__(self, array):
        self.dir_arr = []
        self.add_paths(array)

    def add_paths(self, array):
        for dirname in array:
            if dirname:
                path = Path(dirname)
                if path.exists() and not path in self.dir_arr:
                        self.dir_arr.append(path)

    def get_resolve_list(self, basename):
        subpaths = [dir_.subpath(basename) for dir_ in self.dir_arr]
        return [subpath for subpath in subpaths if subpath.exists()]

class EnvironPathResolver(DirArrayPathResolver):
    def __init__(self):
        DirArrayPathResolver.__init__(self, [])
        for k, v in os.environ.items():
            self.add_paths(v.split(os.path.pathsep))

class DefaultPathResolver(DirArrayPathResolver):
    def __init__(self):
        DirArrayPathResolver.__init__(self, [".", "~/sandbox/eclipse"])

resolvers = [DefaultPathResolver(), EnvironPathResolver()]

def resolve(path):
    results = []
    for resolver in resolvers:
        results.extend(resolver.get_resolve_list(path))
    results = [path for i, path in enumerate(results) if not path in results[:i]]
    if not results:
        raise Exception("can't resolve path '%s'" %(path))
    elif len(results) > 1:
        print("WARNING - multiple resolutions for '%s':" %(path), results)
    return results[0]

class ExecuteIn:
    def __init__(self, path):
        self.prev_path = Path(".")
        self.path = path

    def __enter__(self):
        self.path.chdir()

    def __exit__(self, type_, value, tb):
        self.prev_path.chdir()
