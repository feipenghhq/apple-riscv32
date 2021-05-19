#############################################################
# Makefile to include driver files
#############################################################

REPO_ROOT   = $(shell git rev-parse --show-toplevel)
DIRVER_PATH = $(REPO_ROOT)/sdk/common/driver

INCLUDES 	+= -I$(DIRVER_PATH)/peripherals
INCLUDES 	+= -I$(DIRVER_PATH)/platform
INCLUDES 	+= -I$(DIRVER_PATH)/platform
INCLUDES 	+= -I$(BSP_BASE)/$(BOARD)

C_SRCS   	+= $(DIRVER_PATH)/peripherals/uart.c