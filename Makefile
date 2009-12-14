# @code standards ignore file

SHELL = /bin/bash

VERSION = $(shell python -c "from amara import bindery; print(bindery.parse(open('pom.xml', 'r').read()).project.version)" 2>/dev/null)

OPT_BUILDR = $(shell (which buildr >/dev/null && which buildr) || which mvn)

help:
	@echo "NOTE - this makefile is mostly unix aliases. Use 'mvn install' to build."
	@grep -iE "^(###.+|[a-zA-Z0-9_-]+:.*(#.+)?)$$" Makefile | sed -u -r "s/^### /\n/g; s/:.+#/#/g; s/^/    /g; s/#/\\n        /g; s/:$$//g"

show-info:
	@echo "version = $(VERSION)"
	@echo "buildr or maven = $(OPT_BUILDR)"

target/classes/%s:
	mvn compile

target/version.txt: target/classes/sketch/compiler/localization.properties
	cat target/classes/sketch/compiler/localization.properties | head -n 1 | sed -u "s/version = //g" > target/version.txt

codegen: # codegen a few files (not very high probability of changing)
	scripts/run_jinja2.py

compile:
	$(OPT_BUILDR) compile

install: compile
	mvn install -Dmaven.test.skip=true

### distribution

assemble-file: # internal step
	cp $(FILE) tmp-assembly.xml
	mvn -e assembly:assembly -Dsketch-backend-proj=../sketch-backend -Dmaven.test.skip=true
	rm tmp-assembly.xml

assemble-noarch:
	make assemble-file FILE=jar_assembly.xml

assemble-arch:
	make assemble-file FILE=platform_jar_assembly.xml
	make assemble-file FILE=launchers_assembly.xml
	make assemble-file FILE=tar_src_assembly.xml

assemble: target/version.txt # build all related jar files, assuming sketch-backend is at ../sketch-backend
	mvn -e -q clean compile
	make assemble-noarch assemble-arch
	chmod 755 target/sketch-*-launchers.dir/dist/*/install
	cd target; tar cf sketch-$(VERSION).tar sketch-*-all-*.jar

dist: assemble # alias for assemble

deploy: compile
	mvn deploy -Dmaven.test.skip=true

osc: assemble-noarch
	mkdir -p "java-build"; cp target/sketch-$(VERSION)-noarch.jar java-build
	../sketch-backend/distconfig/linux_rpm/build.py --name sketch-frontend --additional_path java-build --version $(VERSION) --no --osc --commit_msg "[incremental]"
	rm -rf java-build

install-launchers-only:
	mkdir -p $(DESTDIR)/usr/bin
	install -m 755 scripts/new_launchers/unix/sketch $(DESTDIR)/usr/bin
	install -m 755 scripts/new_launchers/unix/psketch $(DESTDIR)/usr/bin
	install -m 755 scripts/new_launchers/unix/stensk $(DESTDIR)/usr/bin

### testing

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
