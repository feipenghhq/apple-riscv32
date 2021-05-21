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
QUARTUS_PATH    = /opt/altera/13.0sp1/quartus
REPO_ROOT     	= $(shell git rev-parse --show-toplevel)
TB_PATH       	= $(REPO_ROOT)/tests/verilog
RTL_SOC_PATH  	= $(REPO_ROOT)
RISCV_TEST_PATH = $(REPO_ROOT)/tests/riscv-tests-simple
IMEM_ROM_PATH 	= $(RISCV_TEST_PATH)/generated

# -----------------------------------------
# Source files
# -----------------------------------------
SOC	?=arty

ifeq ($(SOC), arty)
	TB_FILES     += $(TB_PATH)/arty_tb.v
	RTL_FILES    += $(RTL_SOC_PATH)/AppleSoC_arty.v
	TOPLEVEL 	  = arty_tb
else ifeq ($(SOC), de2)
	TB_FILES     += $(TB_PATH)/de2_tb.v
	TB_FILES     += $(TB_PATH)/IS61LV25616.v
	RTL_FILES    += $(RTL_SOC_PATH)/AppleSoC_de2.v
	RTL_FILES    += $(REPO_ROOT)/src/rtl/de2/intelram_2rw_32kb.v
	RTL_FILES    += $(QUARTUS_PATH)/eda/sim_lib/altera_mf.v
	TOPLEVEL 	  = de2_tb
endif

VERILOG_SOURCES += $(TB_FILES)
VERILOG_SOURCES += $(RTL_FILES)

MODULE   	= run

# -----------------------------------------
# Simulator config
# -----------------------------------------
SIM 		 ?=icarus
DUMP	     ?=0
COMPILE_ARGS =
PLUSARGS 	 =

ifeq ($(SIM),icarus)
	COMPILE_ARGS += -DLOAD_INSTR_RAM
	COMPILE_ARGS += -DLOAD_DATA_RAM
	ifeq ($(DUMP),1)
		COMPILE_ARGS += -DDUMP_VCD
	endif
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

clean_all: clean
	rm -rf *vcd results.xml __pycache__ *.pyc */__pycache__ */*.pyc sim_build *.rom *.bin

