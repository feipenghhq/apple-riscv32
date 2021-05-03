# -----------------------------------------
# Makefile
# -----------------------------------------

# -----------------------------------------
# Top level Language
# -----------------------------------------
TOPLEVEL_LANG = verilog

# -----------------------------------------
# Path Variable
# -----------------------------------------
REPO_ROOT     	= $(shell git rev-parse --show-toplevel)
RTL_SOC_PATH  	= $(REPO_ROOT)/src/rtl/gen/generic
TB_PATH       	= $(REPO_ROOT)/tests/verilog

# -----------------------------------------
# Source files
# -----------------------------------------
TB_FILES      	= $(TB_PATH)/apple_riscv_soc_tb.v
RTL_FILES     	= $(RTL_SOC_PATH)/apple_riscv_soc.v

VERILOG_SOURCES += $(TB_FILES)
VERILOG_SOURCES += $(RTL_FILES)

TOPLEVEL 	= apple_riscv_soc_tb


# -----------------------------------------
# Simulator config
# -----------------------------------------
SIM 		 ?=icarus

# -----------------------------------------
# Test config
# -----------------------------------------
MODULE   	= uart2imem_random

export RUN_TIME    = 1000

include $(shell cocotb-config --makefiles)/Makefile.sim

.PONY: clean_all

clean_rom:
	rm -rf *.rom

clean_all: clean clean_rom
	rm -rf *vcd results.xml __pycache__ *.pyc */__pycache__ */*.pyc sim_build

