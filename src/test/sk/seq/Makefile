
SATSOLVER="MINI"

ALLSK=${wildcard *.sk}

all: ${ALLSK:.sk=.output}
	ls mini*.sk | sed 's\.sk\.output\g'> ref
	grep 'DONE' *.output | tr ':' ' ' | awk '{ print $$1; }' > cur  
	echo "LISTED BELOW ARE THE FAILED TESTS (IF ANY)"
	diff -w cur ref 
	echo "END OF LIST"
	rm cur
	

short: 
	bash  ./regtest.sh
	
short-abc:
	bash ./regtest.sh ABC

long:
	bash ./regtest2.sh
	
clean:
	./clean.sh

%.output: %.sk
	(bash sketch -V 5 --fe-inc '..\..\..\sketchlib' --slv-synth ${SATSOLVER} --slv-verif ${SATSOLVER}   $*.sk &> $@) ; echo
