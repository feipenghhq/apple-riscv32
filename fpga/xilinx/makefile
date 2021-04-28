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
_DEVICE = xc7a35ticsg324-1L

REPO_ROOT     = $(shell git rev-parse --show-toplevel)
XILINX_RTL_PATH = $(REPO_ROOT)/src/main/scala/fpga/xilinx/rtl

_RTL_SRC      += $(REPO_ROOT)/gen/rtl/xilinx/apple_riscv_soc_top.v
_RTL_SRC      += $(XILINX_RTL_PATH)/mmcm.v
_RTL_SRC      += $(XILINX_RTL_PATH)/mmcm_clk_wiz.v

_XDC          += constraints/constraints.xdc
_XDC          += constraints/io.xdc
_XDC          += constraints/timing.xdc

# run vivado at SOC level
soc: export PRJ_NAME 	= soc
soc: export TOP 		= apple_riscv_soc_top
soc: export RTL_SRC     = $(_RTL_SRC)
soc: export XDC         = $(_XDC)
soc: export DEVICE 	    = $(_DEVICE)
soc: clean_soc
	vivado -mode tcl -source run.tcl


# run vivado at Core level - only synthesis is supported
core: export PRJ_NAME 	= core
core: export TOP 		= apple_riscv
core: export DEVICE 	= $(_DEVICE)
core: clean_core
	vivado -mode tcl -source run.tcl -nojournal -nolog

core_gui:
	vivado

clean: clean_core clean_soc

clean_core:
	rm -rf core vivado* .Xil

clean_soc:
	rm -rf soc vivado* .Xil
