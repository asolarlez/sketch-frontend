SIZE=16
echo "MODEL  sketch_test";
echo "CONST ";
LAST=` grep -o '\! *int *\([a-zA-Z0-9_]*\)' $1 | sed 's/\! *int *//g' | tr '\n' '@' | sed 's/@/ != / ' | sed 's/@//'`
cat $1 | \
sed 's/ = / := /g' | \
sed "s/+/+_${SIZE} /g" | \
sed "s/-/-_${SIZE} /g" |  \
sed "s/\*/*_${SIZE} /g" |  \
sed 's/=\(.*\)?\(.*\):\(.*\);/= case \1 : \2; default: \3; esac; /g' | \
grep -v '^ *;' | sed 's/==/ = /g' |  grep -v 'Filter MAIN' | sed 's/\/\/.*//g' |  \
sed "s/int \([a-zA-Z0-9_]*\),/ \1 : BITVEC[${SIZE}];\n /g"

echo "root := ${LAST} ;"

echo "EXEC"
echo "decide(root);" 


