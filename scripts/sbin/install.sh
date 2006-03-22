#!/bin/bash

PATH=$PATH:"`pwd`"
export PATH

FRONTEND="`pwd`/../";
[ $OSTYPE == cygwin ] && FRONTEND=`cygpath -w "$FRONTEND"`
export FRONTEND

SOLVER=`pwd`;
[ $OSTYPE == cygwin ] && SOLVER=`cygpath -w "$SOLVER"`
export SOLVER


