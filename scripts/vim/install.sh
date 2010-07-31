#!/bin/bash
mkdir -p ~/.vim/syntax
rm -f ~/.vim/syntax/sketch.vim
ln -s "$(pwd)"/sketch.vim ~/.vim/syntax
if ! [ -f ~/.vimrc ] || ! grep 'au BufNewFile,BufRead \*\.sk setf sketch' ~/.vimrc; then
    echo "echo 'au BufNewFile,BufRead *.sk setf sketch' >> ~/.vimrc"
    echo 'au BufNewFile,BufRead *.sk setf sketch' >> ~/.vimrc
fi
