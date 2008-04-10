#!/bin/bash

exec python $1.py | tee $1.perf
