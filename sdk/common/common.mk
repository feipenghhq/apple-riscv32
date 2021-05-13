#############################################################
# Makefile to compile C program
#############################################################

.PHONY: all software dasm
all: dasm

#############################################################
# RISCV ISA Configuration
#############################################################

RISCV_ARCH := rv32im
RISCV_ABI  := ilp32

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

SIZE		  = $(RISCV_SIZE)
CC			  = $(RISCV_GCC)
AR			  = $(RISCV_AR)

#############################################################
# Path
#############################################################
REPO_ROOT   = $(shell git rev-parse --show-toplevel)
BSP_BASE    = $(REPO_ROOT)/sdk/bsp
COMMON_BASE = $(REPO_ROOT)/sdk/common
BOARD		?= arty_a7

#############################################################
# Additional Start up code and newlib stub file
#############################################################


ASM_SRCS += $(COMMON_BASE)/boot/trap_entry.S
ASM_SRCS += $(COMMON_BASE)/boot/start.S

C_SRCS 	 += $(COMMON_BASE)/boot/init.c
C_SRCS 	 += $(COMMON_BASE)/boot/trap.c

LINKER_SCRIPT := $(BSP_BASE)/$(BOARD)/link_bram.lds

include $(COMMON_BASE)/newlib/newlib.mk
include $(COMMON_BASE)/driver/driver.mk


#############################################################
# Compilation Flag
#############################################################

LDFLAGS += -T $(LINKER_SCRIPT) -nostartfiles -Wl,--gc-sections  -Wl,--check-sections

# reduce code size
LDFLAGS += --specs=nano.specs

ASM_OBJS     := $(ASM_SRCS:.S=.o)
C_OBJS       := $(C_SRCS:.c=.o)
DUMP_OBJS    := $(C_SRCS:.c=.dump)
VERILOG_OBJS := $(C_SRCS:.c=.verilog)

LINK_OBJS += $(ASM_OBJS) $(C_OBJS)
LINK_DEPS += $(LINKER_SCRIPT)

CLEAN_OBJS += $(TARGET) $(LINK_OBJS) $(DUMP_OBJS) $(VERILOG_OBJS)

CFLAGS += -g
CFLAGS += -march=$(RISCV_ARCH)
CFLAGS += -mabi=$(RISCV_ABI)
CFLAGS += -ffunction-sections -fdata-sections -fno-common


#############################################################
# Command
#############################################################

$(TARGET): $(LINK_OBJS) $(LINK_DEPS)
	$(CC) $(CFLAGS) $(INCLUDES) $(LINK_OBJS) -o $@ $(LDFLAGS)
	$(RISCV_SIZE) $@

$(ASM_OBJS): %.o: %.S
	$(CC) $(CFLAGS) $(INCLUDES) -c -o $@ $<

$(C_OBJS): %.o: %.c
	$(CC) $(CFLAGS) $(INCLUDES) -include sys/cdefs.h -c -o $@ $<

clean:
	rm -f $(TARGET) $(CLEAN_OBJS)


#############################################################
# Prints help message
#############################################################

help:
	@echo "Usage:"
	@echo "make software"
	@echo "- Build the executable for program:"
	@echo "make dasm"
	@echo "- Build the executable for the program and also generate the instruction rom"



#############################################################
# This Section is for Software Compilation
#############################################################

software: $(TARGET)

dasm: software
	$(RISCV_OBJDUMP) -D $(PROGRAM_ELF) > $(PROGRAM_ELF).dump
	$(RISCV_OBJCOPY) $(PROGRAM_ELF) -O verilog $(PROGRAM_ELF).verilog
	sed -i 's/@800/@000/g' $(PROGRAM_ELF).verilog
