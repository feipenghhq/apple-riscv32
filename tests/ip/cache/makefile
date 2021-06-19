# -----------------------------------------
# Makefile
# -----------------------------------------

# -----------------------------------------
# Top level Language
# -----------------------------------------
TOPLEVEL_LANG 	= verilog

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
RTL_FILES 		= $(RTL_PATH)/Ahblite3Cache.v
TOPLEVEL		= Ahblite3Cache

VERILOG_SOURCES += $(RTL_FILES)

# -----------------------------------------
# Simulator config
# -----------------------------------------
#SIM 		 ?=icarus
SIM 		 ?=verilator
DUMP	     ?=0
MODULE 		 ?=basicTests

ifeq ($(SIM),icarus)
	TB_FILES		= $(TB_PATH)/top/cache_tb.v
	VERILOG_SOURCES += $(TB_FILES)
	ifeq ($(DUMP),1)
		COMPILE_ARGS += -DDUMP_VCD
	endif
endif

ifeq ($(SIM),verilator)
	ifeq ($(DUMP),1)
		EXTRA_ARGS += --trace --trace-structs
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
DBG 	?= 0
export 	DEBUG = $(DBG)

# -----------------------------------------
# Test config
# -----------------------------------------

include $(shell cocotb-config --makefiles)/Makefile.sim

clean1:
	@rm -rf __pycache__ *.pyc */__pycache__ */*.pyc *.log
	@rm -rf *vcd results.xml sim_build
	@rm -rf transcript *wlf *.ini

# -----------------------------------------
# Diff tests config
# -----------------------------------------

basic:
	$(MAKE) MODULE=randomTests DBG=$(DBG)

random:
	$(MAKE) MODULE=randomTests DBG=$(DBG)