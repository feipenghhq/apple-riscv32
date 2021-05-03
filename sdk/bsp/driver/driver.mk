#############################################################
# Makefile to include driver files
#############################################################

REPO_ROOT   = $(shell git rev-parse --show-toplevel)
DIRVER_PATH = $(REPO_ROOT)/sdk/bsp/driver

INCLUDES += -I$(DIRVER_PATH)/common
INCLUDES += -I$(DIRVER_PATH)/gpio

C_SRCS   += $(DIRVER_PATH)/common/io.c