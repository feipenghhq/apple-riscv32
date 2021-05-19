############################################################
# makefile For FPGA
############################################################

CMD = ../sdk/tools/UartDownload.py

############################################################
# Arty
############################################################

arty:
	cd arty-a7 && $(MAKE) all

arty_pgm:
	cd arty-a7 && $(MAKE) pgm

blink_arty:
	sudo $(CMD) -size=64 -board=arty -file=program/blink/blink.arty.verilog

uart_arty:
	sudo $(CMD) -size=64 -board=arty -file=program/uart/uart.arty.verilog

coremark_arty:
	sudo $(CMD) -size=64 -board=arty -file=program/coremark/core_main.arty.verilog

############################################################
# DE2
############################################################

de2:
	cd de2 && $(MAKE) all

de2_pgm:
	cd de2 && $(MAKE) pgm

blink_de2:
	sudo $(CMD) -size=32 -board=de2 -file=program/blink/blink.de2.verilog

uart_de2:
	sudo $(CMD) -size=32 -board=de2 -file=program/uart/uart.de2.verilog

coremark_de2:
	sudo $(CMD) -size=32 -board=de2 -file=program/coremark/core_main.de2.verilog