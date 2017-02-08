SRC = jerbil.kt
MAINCLASS = JerbilKt
OUT = $(MAINCLASS).class
TOCLEAN = META-INF *.class
KOTC = kotlinc-jvm
RUNCMD = java -cp /usr/share/kotlin/lib/kotlin-runtime.jar:. $(MAINCLASS)

all: $(OUT) ;

run: $(OUT)
	$(RUNCMD)

$(OUT): $(SRC)
	$(KOTC) $(KOTOPTS) $(SRC)

clean:
	rm -r $(TOCLEAN)
