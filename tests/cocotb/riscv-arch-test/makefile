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
RTL_SOC_PATH  	= $(REPO_ROOT)
RISCV_TEST_PATH = $(REPO_ROOT)/tests/riscv-tests
IMEM_ROM_PATH 	= $(RISCV_TEST_PATH)/generated

# -----------------------------------------
# Source files
# -----------------------------------------
SOC	?=arty

ifeq ($(SOC), arty)
	TB_FILES     += $(TB_PATH)/top/arty_tb.v
	RTL_FILES    += $(RTL_SOC_PATH)/AppleSoC_arty.v
	TOPLEVEL 	 = arty_tb
else ifeq ($(SOC), de2)
	TB_FILES     += $(TB_PATH)/top/de2_tb.v
	TB_FILES     += $(TB_PATH)/model/IS61LV25616.v
	TB_FILES     += $(QUARTUS_PATH)/eda/sim_lib/altera_mf.v
	RTL_FILES    += $(RTL_SOC_PATH)/AppleSoC_de2.v
	RTL_FILES    += $(REPO_ROOT)/src/rtl/de2/intelram_2rw_32kb.v
	TOPLEVEL 	 = de2_tb
endif

VERILOG_SOURCES += $(TB_FILES)
VERILOG_SOURCES += $(RTL_FILES)

MODULE = riscv_arch_test

# -----------------------------------------
# Simulator config
# -----------------------------------------
SIM 		 ?=icarus
DUMP	     ?=0

ifeq ($(SIM),icarus)
	COMPILE_ARGS += -DLOAD_INSTR_RAM
	COMPILE_ARGS += -DLOAD_DATA_RAM
	COMPILE_ARGS += -DSIM_LARGE_INSTR_RAM
	ifeq ($(DUMP),1)
		COMPILE_ARGS += -DDUMP_VCD
	endif
endif

ifeq ($(SIM),modelsim)
	ARCH := i686
	COMPILE_ARGS += +define+LOAD_INSTR_RAM
	COMPILE_ARGS += +define+DLOAD_DATA_RAM
endif

# -----------------------------------------
# Test config
# -----------------------------------------
TESTNAME 	?=
RISCVARCH   ?= rv32i_m
RISCVISA 	?= I
RUNTIME 	?= 10000

export TEST_NAME 	= $(TESTNAME)
export RISCV_ISA 	= $(RISCVISA)
export RUN_TIME 	= $(RUNTIME)
export SoC 			= $(SOC)

include $(shell cocotb-config --makefiles)/Makefile.sim

INSTR_RAM_ADDR_WIDTH = 21

$(REPO_ROOT)/AppleSoC_arty.v:
	cd $(REPO_ROOT) && sbt "runMain AppleRISCVSoC.AppleSoC_artyMain $(INSTR_RAM_ADDR_WIDTH)"

$(REPO_ROOT)/AppleSoC_de2.v:
	cd $(REPO_ROOT) && sbt "runMain AppleRISCVSoC.AppleSoC_de2Main  $(INSTR_RAM_ADDR_WIDTH)"

clean_all:
	@rm -rf __pycache__ *.pyc */__pycache__ */*.pyc *.log
	@rm -rf *vcd results.xml sim_build
	@rm -rf *.rom *.bin *.verilog *.signature
	@rm -rf transcript *wlf .python-version *.ini