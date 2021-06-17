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
QUARTUS_PATH    = /opt/altera/13.0sp1/quartus
TB_PATH       	= $(REPO_ROOT)/tests/tb
RTL_PATH  		= $(REPO_ROOT)

# -----------------------------------------
# Source files
# -----------------------------------------
RTL_FILES 		= $(RTL_PATH)/Ahblite3CacheSetAssociative.v
TB_FILES		= $(TB_PATH)/top/cache_tb.v
TOPLEVEL		= cache_tb

VERILOG_SOURCES += $(TB_FILES)
VERILOG_SOURCES += $(RTL_FILES)

# -----------------------------------------
# Simulator config
# -----------------------------------------
SIM 		 ?=icarus
DUMP	     ?=0
MODULE 		 = cache_test

ifeq ($(SIM),icarus)
	ifeq ($(DUMP),1)
		COMPILE_ARGS += -DDUMP_VCD
	endif
endif

ifeq ($(SIM),modelsim)
	ARCH=i686
	ifeq ($(DUMP),1)
		COMPILE_ARGS += -DDUMP_VCD
	endif
endif

# -----------------------------------------
# Cocotb config
# -----------------------------------------
export COCOTB_REDUCED_LOG_FMT = 1

# -----------------------------------------
# Test config
# -----------------------------------------

include $(shell cocotb-config --makefiles)/Makefile.sim

clean_all:
	@rm -rf __pycache__ *.pyc */__pycache__ */*.pyc *.log
	@rm -rf *vcd results.xml sim_build
	@rm -rf transcript *wlf *.ini