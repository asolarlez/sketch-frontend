IPATH=..\\..\\..\\
RTIME=../../../


include ../../../testrunner.mk

all: ${ALLSK:.sk=.output}
	grep 'Error.* typeTest.*:[0-9]*' typeTest*.output | grep -o ' typeTest.*:[0-9]*' | awk '{print $$1}' | sort | uniq > errorsFound
	grep -n TypeError typeTest*.sk* | tr ':' ' ' | awk '{print $$1":"$$2}' | sort > errorsThere
	echo "LISTED BELOW ARE THE FAILED TESTS (IF ANY)"
	diff -w errorsFound errorsThere > result; cat result; 
	echo "END OF LIST"
	rm cur

clean: 
	rm *.output
