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
TB_PATH       	= $(REPO_ROOT)/tests/verilog
RTL_SOC_PATH  	= $(REPO_ROOT)/gen/rtl
RISCV_TEST_PATH = $(REPO_ROOT)/tests/riscv-tests-simple
IMEM_ROM_PATH 	= $(RISCV_TEST_PATH)/generated

# -----------------------------------------
# Source files
# -----------------------------------------
TB_FILES      	= $(TB_PATH)/apple_riscv_soc_tb.v
RTL_FILES     	= $(RTL_SOC_PATH)/apple_riscv_soc.v

VERILOG_SOURCES += $(TB_FILES)
VERILOG_SOURCES += $(RTL_FILES)

TOPLEVEL 	= apple_riscv_soc_tb
MODULE   	= run

# -----------------------------------------
# Simulator config
# -----------------------------------------
SIM 		 ?=icarus
COMPILE_ARGS =
PLUSARGS 	 =

ifeq ($(SIM),icarus)
	COMPILE_ARGS += -DLOAD_INSTR_RAM
	COMPILE_ARGS += -DLOAD_DATA_RAM
endif

# -----------------------------------------
# Test config
# -----------------------------------------
TESTNAME 	?=
RISCVARCH 	?= rv32ui
RUNTIME 	?= 10000

export TEST_NAME 	= $(TESTNAME)
export RISCV_ARCH 	= $(RISCVARCH)
export RUN_TIME 	= $(RUNTIME)

include $(shell cocotb-config --makefiles)/Makefile.sim

.PONY: clean_all

clean_rom:
	rm -rf *.rom

clean_all: clean clean_rom
	rm -rf *vcd results.xml __pycache__ *.pyc */__pycache__ */*.pyc sim_build

