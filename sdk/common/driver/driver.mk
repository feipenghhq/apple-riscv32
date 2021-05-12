#############################################################
# Makefile to include driver files
#############################################################

REPO_ROOT   = $(shell git rev-parse --show-toplevel)
DIRVER_PATH = $(REPO_ROOT)/sdk/common/driver

INCLUDES 	+= -I$(DIRVER_PATH)/periphals
INCLUDES 	+= -I$(DIRVER_PATH)/platform

C_SRCS   	+= $(DIRVER_PATH)/periphals/uart.c