# -----------------------------------------
# Makefile
# -----------------------------------------

# -----------------------------------------
# Top level Language
# -----------------------------------------
TOPLEVEL_LANG = verilog

# -----------------------------------------
# Path
# -----------------------------------------
REPO_ROOT     = $(shell git rev-parse --show-toplevel)
TB_PATH       = $(REPO_ROOT)/tests/verilog
RTL_SOC_PATH  = $(REPO_ROOT)

# -----------------------------------------
# Source files
# -----------------------------------------
TB_FILES      = $(TB_PATH)/apple_riscv_soc_tb.v
RTL_FILES     = $(RTL_SOC_PATH)/AppleRISCVSoC.v

VERILOG_SOURCES += $(TB_FILES)
VERILOG_SOURCES += $(RTL_FILES)

TOPLEVEL = apple_riscv_soc_tb
MODULE   = sanity_check
TESTCASE ?=

# -----------------------------------------
# Simulator
# -----------------------------------------
SIM 		 ?=icarus
COMPILE_ARGS = -DDUMP_WAVE

include $(shell cocotb-config --makefiles)/Makefile.sim

clean_all: clean
	rm -rf *vcd results.xml __pycache__ *.pyc */__pycache__ */*.pyc

# -----------------------------------------
# Verilator Lint
# -----------------------------------------
lint:
	verilator --lint-only -Wall $(RTL_FILES)
