#############################################################
# Makefile to compile softare
#############################################################

#############################################################
# RISCV ISA Configuration
#############################################################

RISCV_ARCH := rv32i
RISCV_ABI  := ilp32

# Default target
PROGRAM ?= coremark

#############################################################
# Prints help message
#############################################################
.PHONY: help
help:
	@echo "Usage:"
	@echo "make software PROGRAM=<program>"
	@echo "- Build the executable for program:"
	@echo "make dasm PROGRAM=<program>  "
	@echo "- Build the executable for the program and also generate the instruction rom"


.PHONY: clean
clean:

#############################################################
# This section is for tool installation
#############################################################

RISCV_GCC     := riscv-none-embed-gcc
RISCV_GXX     := riscv-none-embed-g++
RISCV_OBJDUMP := riscv-none-embed-objdump
RISCV_OBJCOPY := riscv-none-embed-objcopy
RISCV_GDB     := riscv-none-embed-gdb
RISCV_AR      := riscv-none-embed-ar
RISCV_SIZE    := riscv-none-embed-size

#############################################################
# This Section is for Software Compilation
#############################################################
PROGRAM_DIR = $(PROGRAM)
PROGRAM_ELF = $(PROGRAM)/$(PROGRAM)

.PHONY: software_clean
software_clean:
	$(MAKE) -C $(PROGRAM_DIR) clean

.PHONY: software
software: software_clean
	$(MAKE) -C $(PROGRAM_DIR) SIZE=$(RISCV_SIZE) CC=$(RISCV_GCC) RISCV_ARCH=$(RISCV_ARCH) RISCV_ABI=$(RISCV_ABI) AR=$(RISCV_AR)

dasm: software
	$(RISCV_OBJDUMP) -D $(PROGRAM_ELF) > $(PROGRAM_ELF).dump
	$(RISCV_OBJCOPY) $(PROGRAM_ELF) -O verilog $(PROGRAM_ELF).verilog
	sed -i 's/@800/@000/g' $(PROGRAM_ELF).verilog
