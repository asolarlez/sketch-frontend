#!/usr/bin/zsh 

htmlpath="${1:?usage: sk2html.zsh filename}".html

[ -f $htmlpath ] && rm $htmlpath

vim -e -s "$1" >/dev/null <<-EOF
:syntax enable
:setf sketch
:let html_use_css = 1
:runtime! syntax/2html.vim
:w!
:quit
EOF

[ -f $htmlpath ] || { echo "output to $htmlpath failed!" 1>&2; exit 1; }

output=$(cat $htmlpath | awk '
BEGIN {
    printed=0
    in_ss=0
}
{
    if ($0 ~ "<style type") {
        in_ss=1
    }
    if (in_ss) {
        if ($0 ~ "</style") {
            in_ss=0
        }
        if (!printed) {
            printed=1
            print "<style type=\"text/css\">"
            print "<!--"
            print ".PreProc { color: #6090ff; }"
            print ".Type { color: #009f00; font-weight: bold; }"
            print ".Statement { color: #cfaf00; font-weight: bold; }"
            print ".Comment { color: #009cec; font-weight: bold; }"
            print ".Special { color: #ff6060; font-weight: bold; }"
            print "pre { font-family: \"Droid Sans\", sans-serif; font-size: 10pt; }"
            print "body { font-family: \"Droid Sans\", sans-serif; font-size: 10pt; }"
            print "-->"
            print "</style>"
        }
    } else {
        print $0
    }
}
')

echo $output > $htmlpath
