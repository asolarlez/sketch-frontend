#!/bin/bash

modpath="$(dirname "$(readlink -f "$0")")"

set -v
mkdir -p ~/.vim/syntax
rm -f ~/.vim/syntax/sketch.vim
ln -s "$modpath"/syntax/sketch.vim ~/.vim/syntax
ln -s "$modpath"/ftdetect/sketch.vim ~/.vim/ftdetect
