#############################################################
# Makefile to include newlib files
#############################################################

REPO_ROOT   = $(shell git rev-parse --show-toplevel)
NEWLIB_PATH = $(REPO_ROOT)/sdk/common/newlib

C_SRCS   	+= $(NEWLIB_PATH)/_exit.c
C_SRCS   	+= $(NEWLIB_PATH)/close.c
C_SRCS   	+= $(NEWLIB_PATH)/fstat.c
C_SRCS   	+= $(NEWLIB_PATH)/isatty.c
C_SRCS   	+= $(NEWLIB_PATH)/lseek.c
C_SRCS   	+= $(NEWLIB_PATH)/read.c
C_SRCS   	+= $(NEWLIB_PATH)/sbrk.c
C_SRCS   	+= $(NEWLIB_PATH)/time.c
C_SRCS   	+= $(NEWLIB_PATH)/write.c
