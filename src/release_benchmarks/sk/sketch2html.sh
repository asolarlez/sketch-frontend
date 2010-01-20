echo "<html><head><title>$1</title></head>"
echo "<pre>"
cat $1 | sed 's/<=/\&le;/g' | sed 's/>=/\&ge;/g'| sed 's/</\&lt;/g' | sed 's/>/\&gt;/g'  \
| sed 's/\/\/.*$/<font color="#348781">&<\/font>/g' \
| sed 's/\/\*/<font color="#348781">\/\*/g' \
| sed 's/\*\//\*\/<\/font>/g' \
| sed 's/include.*\"\(.*\)\"/include "<a href="\1.html">\1<\/a>"/g' \
| sed 's/include/<font color="#800517">include<\/font>/g' \
| sed 's/\(pragma options\) \(".*"\)/<font color="#800517">\1<\/font> <font color="	#151B8D">\2<\/font>/g' \
| sed 's/\([^a-zA-Z0-9]\|^\)\(int\)\([^a-zA-Z0-9]\|$\)/\1<font color="#800517">\2<\/font>\3/g' \
| sed 's/\([^a-zA-Z0-9]\|^\)\(bit\)\([^a-zA-Z0-9]\|$\)/\1<font color="#800517">\2<\/font>\3/g' \
| sed 's/\([^a-zA-Z0-9]\|^\)\(for\)\([^a-zA-Z0-9]\|$\)/\1<font color="#800517">\2<\/font>\3/g' \
| sed 's/\([^a-zA-Z0-9]\|^\)\(while\)\([^a-zA-Z0-9]\|$\)/\1<font color="#800517">\2<\/font>\3/g' \
| sed 's/\([^a-zA-Z0-9]\|^\)\(if\)\([^a-zA-Z0-9]\|$\)/\1<font color="#800517">\2<\/font>\3/g' \
| sed 's/\([^a-zA-Z0-9]\|^\)\(assert\)\([^a-zA-Z0-9]\|$\)/\1<font color="#800517">\2<\/font>\3/g' \
| sed 's/\([^a-zA-Z0-9]\|^\)\(static\)\([^a-zA-Z0-9]\|$\)/\1<font color="#800517">\2<\/font>\3/g' \
| sed 's/\([^a-zA-Z0-9]\|^\)\(struct\)\([^a-zA-Z0-9]\|$\)/\1<font color="#800517">\2<\/font>\3/g' \
| sed 's/\([^a-zA-Z0-9]\|^\)\(return\)\([^a-zA-Z0-9]\|$\)/\1<font color="#800517">\2<\/font>\3/g' \
| sed 's/\([^a-zA-Z0-9]\|^\)\(implements\)\([^a-zA-Z0-9]\|$\)/\1<font color="#800517">\2<\/font>\3/g' 
echo "</pre>"
echo "</html>"