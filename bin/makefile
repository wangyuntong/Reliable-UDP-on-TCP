SOURCES = TCPHeader.java sender.java receiver.java 
CLASSES = TCPHeader.class sender.class receiver.class

JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class