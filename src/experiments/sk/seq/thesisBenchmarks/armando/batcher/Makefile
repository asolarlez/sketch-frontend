#!/usr/bin/make

batcher_sort: batcher_sort.cpp
	@:

clean:
	rm *.cpp *.h

%.cpp : %.sk
# 	gcc -E -x c -P $< > .preprocessed.sk
	sketch --inbits 2 --cbits 3 --keepasserts --outputcode $<
# 	@mv .preprocessed.cpp $@
# 	@./extract_from_sketch.py $@
