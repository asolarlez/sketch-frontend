# @code standards ignore file

help:
	@echo "NOTE - this makefile is mostly unix aliases. Use 'mvn install' to build."
	@grep -iE "^(###.+|[a-zA-Z0-9_-]+:.*(#.+)?)$$" Makefile | sed -u -r "s/^### /\n/g; s/:.+#/#/g; s/^/    /g; s/#/\\n        /g; s/:$$//g"

target/classes/%s:
	mvn compile

target/version.txt: target/classes/sketch/compiler/localization.properties
	cat target/classes/sketch/compiler/localization.properties | head -n 1 | sed -u "s/version = //g" > target/version.txt

### distribution and testing

assemble: target/version.txt # build all related jar files, assuming sketch-backend is at ../sketch-backend
	mvn -e -q clean compile
	mvn -e -q assembly:assembly -Dsketch-backend-proj=../sketch-backend -Dmaven.test.skip=true
	chmod 755 target/sketch-*-launchers.dir/dist/*/install
	cd target; tar cf sketch-$$(cat version.txt).tar sketch-*-all-*.jar

dist: assemble # alias for assemble

test:
	mvn test | tee target/test_output.txt | grep -E "\[SKETCH\] running test|\[ERROR\]"

test-seq:
	set -o pipefail; mvn test "-Dtest=SequentialJunitTest" | tee target/test_output.txt | grep -E "\[SKETCH\] running test|\[ERROR\]"

test-par:
	set -o pipefail; mvn test "-Dtest=ParallelJunitTest" | tee target/test_output.txt | grep -E "\[SKETCH\] running test|\[ERROR\]"

test-sten:
	set -o pipefail; mvn test "-Dtest=StencilJunitTest" | tee target/test_output.txt | grep -E "\[SKETCH\] running test|\[ERROR\]"

### manually running sketches using the development versions; use $(make <target> EXEC_ARGS=args)

run-platform-seq: target/version.txt # run a test using the platform jar
	[ -f target/sketch-*-all-*.jar ] || make assemble
	java -cp target/sketch-*-all-*.jar -ea sketch.compiler.main.seq.SequentialSketchMain $(EXEC_ARGS)

run-local-seq:
	mvn -e compile exec:java "-Dexec.mainClass=sketch.compiler.main.seq.SequentialSketchMain" "-Dexec.args=$(EXEC_ARGS)"

run-local-par:
	mvn -e compile exec:java "-Dexec.mainClass=sketch.compiler.main.par.ParallelSketchMain" "-Dexec.args=$(EXEC_ARGS)"

run-local-sten:
	mvn -e compile exec:java "-Dexec.mainClass=sketch.compiler.main.sten.StencilSketchMain" "-Dexec.args=$(EXEC_ARGS)"
