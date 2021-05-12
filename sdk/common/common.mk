#############################################################
# Makefile to compile C program
#############################################################

.PHONY: all
all: $(TARGET)



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
LDFLAGS += -L$(ENV_DIR)

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
	$(SIZE) $@

$(ASM_OBJS): %.o: %.S
	$(CC) $(CFLAGS) $(INCLUDES) -c -o $@ $<

$(C_OBJS): %.o: %.c
	$(CC) $(CFLAGS) $(INCLUDES) -include sys/cdefs.h -c -o $@ $<

.PHONY: clean
clean:
	rm -f $(TARGET) $(CLEAN_OBJS)
