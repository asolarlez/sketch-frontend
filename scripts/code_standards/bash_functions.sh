#/bin/bash

# mostly run from the makefile, so I don't have to excessively escape $'s --gatoatigrado

function trim_whitespace() {
    [ "$1" ] || { echo "usage: trim_whitespace target"; return 1; }
    for i in $(find "$1" -type f); do
        [ "$(file "$i" | grep text)" ] && {
            sed -i -r "s/\s+\$//g" "$i"
        }
    done
}

function set_package_decls() (
    [ -d "$1" ] || { echo "usage: set_package_decls basedir"; return 1; }
    cd "$1"
    for directory in *; do
        echo "setting in directory $directory"
        [ -d "$directory" ] || continue
        for i in $(find "$directory" -iname "*.scala")
                $(find "$directory" -iname "*.java"); do

            endsep=$(python -c "import sys
if \"java\" in sys.argv[1]:
    print(\";\")" "$i")
            nicename="$(echo "$(dirname "$i")" | sed 's/\//./g')"
            echo "setting package for $i to $nicename"
            sed -i -r "s/^package.+\$/package $nicename$endsep/g" "$i"
        done
    done
)
