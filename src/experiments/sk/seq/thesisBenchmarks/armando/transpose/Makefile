# @author Nicholas Tung

TARGET=sse_transpose
OBJFILES=sse_transpose.o

default: $(TARGET)

clean:
	rm -f $(TARGET) *.o *~

$(TARGET): $(OBJFILES)
	@rm -f *~
	g++ -g -O3 -msse -o $@ $<

%.o: %.cc
	g++ -g -O3 -msse -c $<
