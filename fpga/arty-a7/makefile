############################################################
# makefile for running vivado
############################################################

# Variable
REPO_ROOT     	= $(shell git rev-parse --show-toplevel)
XILINX_RTL_PATH = $(REPO_ROOT)/src/rtl/arty_a7/

_DEVICE = xc7a35ticsg324-1L

TOP_RTL		  = ArtyA7_top.v

_RTL_SRC      += $(REPO_ROOT)/ArtyA7_top.v
_RTL_SRC      += $(XILINX_RTL_PATH)/mmcm.v
_RTL_SRC      += $(XILINX_RTL_PATH)/mmcm_clk_wiz.v

_XDC          += constraints/constraints.xdc
_XDC          += constraints/io.xdc
_XDC          += constraints/timing.xdc

export PRJ_NAME 	= arty
export TOP 			= ArtyA7_top
export RTL_SRC     	= $(_RTL_SRC)
export XDC         	= $(_XDC)
export DEVICE 	    = $(_DEVICE)
export PROGRAM 		= $(_PROGRAM)

OUTPUT = $(PRJ_NAME)/output


all: clean build pgm

build: $(OUTPUT)/$(TOP).bit

pgm:
	vivado -mode tcl -source scripts/program.tcl | tee program.log

$(OUTPUT)/$(TOP).bit: $(TOP_RTL)
	vivado -mode tcl -source scripts/build.tcl | tee build.log

$(TOP_RTL):
	cd $(REPO_ROOT) && sbt "runMain Board.arty_a7.ArtyA7_topMain"

clean:
	rm -rf arty vivado* .Xil *.jou *.log
