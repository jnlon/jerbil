MAINCLASS="jerbil.JerbilKt"
all:
	gradle build &
run:
	gradle run &
stop:
	pkill -f $(MAINCLASS)
