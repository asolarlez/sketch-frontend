# @code standards ignore file

SHELL = /bin/bash

VERSION = 1.6.5
# If you change the version you also need to change pom.xml and sketch.compiler.main.PlatformLocalization.java and scripts/windows/final/sketch as well as README and the bitbucket site.

MVN_PATH = $(shell which mvn)

help:
	@echo "NOTE - this makefile is mostly unix aliases. Use 'mvn install' to build."
	@grep -iE "^(###.+|[a-zA-Z0-9_-]+:.*(#.+)?)$$" Makefile | sed -u -r "/ #HIDDEN/d; s/^### /\n/g; s/:.+#/#/g; s/^/    /g; s/#/\\n        /g; s/:$$//g"

help-all: # show uncommon commands as well
	@echo "NOTE - this makefile is mostly unix aliases. Use 'mvn install' to build."
	@grep -iE "^(###.+|[a-zA-Z0-9_-]+:.*(#.+)?)$$" Makefile | sed -u -r "s/ #HIDDEN//g; s/^### /\n/g; s/:.+#/#/g; s/^/    /g; s/#/\\n        /g; s/:$$//g"

show-info:
	@echo "version = $(VERSION)"
	@echo "maven = $(MVN_PATH)"

clean:
	zsh -c "setopt -G; rm -f **/*timestamp **/*pyc **/*~ **/skalch/plugins/type_graph.gxl"
	zsh -c "setopt -G; rm -rf **/(bin|target) .gen **/gen/ **/reports/junit"
	zsh -c "setopt -G; rm -rf ~/.m2/repository/edu/berkeley/cs/sketch/sketch-frontend"

compile: # compile all sources
	$(MVN_PATH) compile

maven-install: compile
	mvn install -Dmaven.test.skip=true

### development #HIDDEN

codegen: # codegen a few files (not very high probability of changing) #HIDDEN
	python scripts/run_jinja2.py
	antlr -o src/main/java/sketch/compiler/parser src/main/other/sketch/compiler/parser/StreamItLex.g
	antlr -o src/main/java/sketch/compiler/parser src/main/other/sketch/compiler/parser/StreamItParserFE.g

renamer-script: #HIDDEN
	[ -f sketch-noarch.jar ] || { make assemble-noarch; cp target/sketch-*-noarch.jar sketch-noarch.jar; }
	python scripts/rewrite_fcns.py

### distribution

assemble-file: #HIDDEN
	cp scripts/build/assembly/$(FILE) tmp-assembly.xml
	mvn -e compile assembly:assembly -Dsketch-backend-proj=../sketch-backend -Dmaven.test.skip=true
	rm tmp-assembly.xml

assemble-noarch:
	make assemble-file FILE=jar_assembly.xml
	make assemble-file FILE=noarch_launchers_assembly.xml
	@echo -e "\n\n\nYour output is in: $$(ls -d target/sketch-$(VERSION)-noarch-launchers*)"

assemble-arch:
	make assemble-file FILE=platform_jar_assembly.xml
	make assemble-file FILE=launchers_assembly.xml
	make assemble-file FILE=tar_src_assembly.xml
	@echo -e "\n\n\nYour output is in: $$(ls -d target/sketch-$(VERSION)-launchers*)"

win-installer: assemble-arch
	basedir=$$(pwd); cd target/*-launchers-windows.dir; mv COPYING *jar installer; cd installer; /cygdrive/c/Program\ Files\ \(x86\)/NSIS/makensis sketch-installer.nsi; cp *exe "$$basedir"
	@ls -l *exe

deploy: compile
	mvn deploy -Dmaven.test.skip=true

osc: assemble-noarch codegen
	mkdir -p "java-build"; cp target/sketch-$(VERSION)-noarch.jar java-build
	python ../sketch-backend/distconfig/linux_rpm/build.py --name sketch-frontend --additional_path java-build --version $(VERSION) --no --osc --commit_msg "[incremental]"
	rm -rf java-build

install-launchers-only: #HIDDEN used for obs (the opensuse build system)
	mkdir -p $(DESTDIR)/usr/bin
	install -m 755 scripts/unix/final/sketch $(DESTDIR)/usr/bin
	install -m 755 scripts/unix/final/psketch $(DESTDIR)/usr/bin
	install -m 755 scripts/unix/final/stensk $(DESTDIR)/usr/bin

system-install: # usage: make system-install DESTDIR=/usr/bin [SUDOINSTALL=1]
	[ "$(DESTDIR)" ] || { echo "no destination directory defined. try make help."; exit 1; }
	make assemble-file FILE=platform_jar_assembly.xml
	make assemble-file FILE=launchers_assembly.xml
	mkdir -p $(DESTDIR)
	sudo=; [ "$(SUDOINSTALL)" ] && { sudo=sudo; }; DESTDIR="$$(readlink -f "$(DESTDIR)")"; cd target/sketch-*-launchers-* && $$sudo install -m 644 *jar "$$DESTDIR" && $$sudo install -m 755 sketch psketch stensk "$$DESTDIR"

### testing

test:
	mvn test | tee target/test_output.txt | grep -E "\[SKETCH\] running test|\[ERROR\]|ASSERT FAILURE"

test-seq: compile
	set -o pipefail; mkdir -p target
	time mvn test "-Dtest=SequentialJunitTest" | tee target/test_output.txt | grep -E "\[SKETCH\] running test|ERROR\]|ASSERT FAILURE"

test-med-release-benchmarks: compile
	set -o pipefail; mkdir -p target
	time mvn test "-Dtest=MediumReleaseBenchmarks" | tee target/test_output.txt | grep -E "\[SKETCH\] running test|ERROR\]|ASSERT FAILURE"

# test-sten:
# 	set -o pipefail; mkdir -p target; mvn test "-Dtest=StencilJunitTest" | tee target/test_output.txt | grep -E "\[SKETCH\] running test|\[ERROR\]|ASSERT FAILURE"

test-release-benchmarks:
	for i in src/release_benchmarks/sk/*.sk; do make run-local-seq EXEC_ARGS="$$i"; done | tee target/test_output.txt | grep -E "Benchmark = src/release_benchmarks|\[ERROR\]|ASSERT FAILURE"

### manually running sketches using the development versions; use $(make <target> EXEC_ARGS=args)

run-platform-seq: # run a test using the platform jar
	[ -f target/sketch-*-all-*.jar ] || make assemble
	java -cp target/sketch-*-all-*.jar -ea sketch.compiler.main.seq.SequentialSketchMain $(EXEC_ARGS)

run-local-seq:
	@export MAVEN_OPTS="-XX:MaxPermSize=256m -Xms40m -Xmx600m -ea -server"; mvn -e compile exec:java "-Dexec.mainClass=sketch.compiler.main.seq.SequentialSketchMain" "-Dexec.args=$(EXEC_ARGS)"

dump-fcn-info: # dump information about functions to a file. usage: EXEC_ARGS=filename.sk
	mvn -e compile exec:java "-Dexec.mainClass=sketch.compiler.main.other.ParseFunctions" "-Dexec.args=$(EXEC_ARGS)"

run-local-par:
	export MAVEN_OPTS="-XX:MaxPermSize=256m -Xms40m -Xmx2300m -ea -server"; mvn -e compile exec:java "-Dexec.mainClass=sketch.compiler.main.par.ParallelSketchMain" "-Dexec.args=$(EXEC_ARGS)"

run-local-sten:
	export MAVEN_OPTS="-XX:MaxPermSize=256m -Xms40m -Xmx2300m -ea -server"; mvn -e compile exec:java "-Dexec.mainClass=sketch.compiler.main.sten.StencilSketchMain" "-Dexec.args=$(EXEC_ARGS)"

light-distr:
	rm -rf ../sketch-$(VERSION)
	rm -rf ../sketch-distr
	mkdir ../sketch-distr
	mkdir ../sketch-distr/src
	cp -r src/main ../sketch-distr/src
	cp -r src/release_benchmarks  ../sketch-distr
	cp -r src/sketchlib  ../sketch-distr
	cp -r src/runtime  ../sketch-distr
	cp -r src/test  ../sketch-distr
	rm -r ../sketch-distr/test/deprecated
	cp src/testrunner.mk ../sketch-distr
	make assemble-noarch
	cp $$(ls -d target/sketch-$(VERSION)-noarch-launchers*)/*.jar ../sketch-distr/.
	cp -r scripts ../sketch-distr
	cp docs/SketchManual/manual.pdf ../sketch-distr/LanguageReference.pdf
	cp scripts/windows/final/sketch ../sketch-distr/.
	chmod +x ../sketch-distr/sketch
	mkdir ../sketch-$(VERSION) 
	mv ../sketch-distr ../sketch-$(VERSION)/sketch-frontend
	cp -r ../sketch-backend ../sketch-$(VERSION)/.
	cat LIGHT_README | sed 's/\\VER/$(VERSION)/g' > ../sketch-$(VERSION)/README
	rm -rf ../sketch-$(VERSION)/sketch-backend/.hg
	cd ../sketch-$(VERSION)/sketch-backend; bash ./autogen.sh; cd ../../sketch-frontend
