
SATSOLVER="MINI"

ALLSK=${wildcard *.sk}

all: ${ALLSK:.sk=.output}
	ls *.sk | sed 's\.sk\.output\g'> ref
	grep 'DONE' *.output | tr ':' ' ' | awk '{ print $$1; }' > cur  
	echo "LISTED BELOW ARE THE FAILED TESTS (IF ANY)"
	diff -w cur ref > result; cat result; wc `cat result | awk '/>/{print $$2}' | sed 's/\.output/\.sk/g'`
	echo "END OF LIST"
	rm cur
	

short: 
	bash  ./regtest.sh
	
short-abc:
	bash ./regtest.sh ABC

long: ${ALLSK:.sk=.eout}
	ls mini*.sk | sed 's\.sk\.eout\g'> ref
	grep 'passed' *.eout | tr ':' ' ' | awk '{ print $$1; }' > cur
	echo "LISTED BELOW ARE THE FAILED TESTS (IF ANY)"
	diff -w cur ref > result; cat result; wc `(cat result | awk '/>/{print $$2}' | sed 's/\.output/\.sk/g');echo "cur"`
	echo "END OF LIST"
	rm cur
	
clean:
	./clean.sh

%.eout: %.cpp
	g++ -I "${RTIME}/runtime/include" -o $*.exe $*.cpp $*_test.cpp; echo "fa"
	./$*.exe &> $*.eout ; echo "";
	
%.cpp: %.sk
	(bash sketch -V 5 --fe-inc '${IPATH}\sketchlib'  --slv-timeout 10 --fe-output-code --fe-output-test --slv-synth ${SATSOLVER} --slv-verif ${SATSOLVER}   $*.sk > $*.output 2>&1) ; echo

%.output: %.sk
	(bash sketch -V 5 --fe-inc '${IPATH}\sketchlib' --slv-timeout 10 --slv-synth ${SATSOLVER} --slv-verif ${SATSOLVER}   $*.sk > $@ 2>&1) ; echo
