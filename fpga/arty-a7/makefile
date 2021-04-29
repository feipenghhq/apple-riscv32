############################################################
# makefile for running vivado
############################################################

help:
	@echo "#############################################"
	@echo "#        Make flow for running Vivado       #"
	@echo "#############################################"
	@echo "# make clean    clean all the run result    #"
	@echo "# make soc      run vivado on soc           #"
	@echo "# make core     run vivado on core          #"
	@echo "#############################################"

# Variable
REPO_ROOT     	= $(shell git rev-parse --show-toplevel)
XILINX_RTL_PATH = $(REPO_ROOT)/src/rtl/arty_a7/

_DEVICE = xc7a35ticsg324-1L

_RTL_SRC      += $(REPO_ROOT)/src/rtl/gen/arty_a7/apple_riscv_soc_top.v
_RTL_SRC      += $(XILINX_RTL_PATH)/mmcm.v
_RTL_SRC      += $(XILINX_RTL_PATH)/mmcm_clk_wiz.v

_XDC          += constraints/constraints.xdc
_XDC          += constraints/io.xdc
_XDC          += constraints/timing.xdc

export PRJ_NAME 	= soc
export TOP 			= apple_riscv_soc_top
export RTL_SRC     	= $(_RTL_SRC)
export XDC         	= $(_XDC)
export DEVICE 	    = $(_DEVICE)

# run vivado at SOC level
soc: clean_soc
	vivado -mode tcl -source run.tcl

clean:  clean_soc

clean_soc:
	rm -rf soc vivado* .Xil
