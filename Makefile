PROJECT = jerbil
SRC = jerbilconf.kt jerbil.kt
MAINCLASS = JerbilKt
OUT = $(MAINCLASS).class
TOCLEAN = META-INF *.class
KOTC = kotlinc-jvm
CLASSPATH = /usr/share/kotlin/lib/kotlin-runtime.jar:.
RUNCMD = java -cp $(CLASSPATH) $(JAVAOPTS) $(PROJECT).$(MAINCLASS)

all: $(OUT) ;

run: $(OUT)
	$(RUNCMD)

$(OUT): $(SRC)
	$(KOTC) $(KOTOPTS) $(SRC)

clean:
	rm -r $(TOCLEAN)
