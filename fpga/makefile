############################################################
# makefile For FPGA
############################################################

BOARD ?=

############################################################
# FPGA build
############################################################

fpga:
	cd $(BOARD) && $(MAKE) all

pgm:
	cd $(BOARD) && $(MAKE) pgm

############################################################
# Compile and Load Program
############################################################

REPO_ROOT = $(shell git rev-parse --show-toplevel)
CMD = $(REPO_ROOT)/sdk/tools/UartDownload.py

demo = blink uart gpio interrupt mem_test pwm
program = coremark
size ?=

boardCheck:
ifeq ($(BOARD), )
$(error BOARD not set)
endif
ifeq ($(BOARD), arty)
size := 64
endif
ifeq ($(BOARD), de2)
size := 32
endif

$(demo): boardCheck
	cd $(REPO_ROOT)/sdk/demo/$@ && make BOARD=$(BOARD)
	sudo $(CMD) -size=$(size) -board=$(BOARD) -file=$(REPO_ROOT)/sdk/demo/$@/$@.verilog

$(program): boardCheck
	cd $(REPO_ROOT)/sdk/benchmark/$@ && make BOARD=$(BOARD)
	sudo $(CMD) -size=$(size) -board=$(BOARD) -file=$(REPO_ROOT)/sdk/benchmark/$@/$@.verilog
