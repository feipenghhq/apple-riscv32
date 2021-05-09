#############################################################
# Makefile to include driver files
#############################################################

REPO_ROOT   = $(shell git rev-parse --show-toplevel)
DIRVER_PATH = $(REPO_ROOT)/sdk/bsp/driver

INCLUDES += -I$(DIRVER_PATH)/system
INCLUDES += -I$(DIRVER_PATH)/periphals

C_SRCS   += $(DIRVER_PATH)/system/io.c
C_SRCS   += $(DIRVER_PATH)/periphals/uart.c