#!/bin/bash

modpath="$(dirname "$(readlink -f "$0")")"

mkdir -p ~/.vim/syntax
rm -f ~/.vim/syntax/sketch.vim
ln -s "$modpath"/sketch.vim ~/.vim/syntax
if ! [ -f ~/.vimrc ] || ! grep 'au BufNewFile,BufRead \*\.sk,\*\.sk\.jinja2 setf sketch' ~/.vimrc; then
    echo "echo 'au BufNewFile,BufRead *.sk,*.sk.jinja2 setf sketch' >> ~/.vimrc"
    echo 'au BufNewFile,BufRead *.sk,*.sk.jinja2 setf sketch' >> ~/.vimrc
fi
