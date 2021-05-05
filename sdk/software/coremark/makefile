#############################################################
# Makefile for coremark
#############################################################

TARGET = coremark

#############################################################
# Config
#############################################################

CFLAGS := -O2 -fno-common -funroll-loops -finline-functions --param max-inline-insns-auto=20 -falign-functions=4 -falign-jumps=4 -falign-loops=4
CFLAGS += -DFLAGS_STR=\""$(CFLAGS)"\"
CFLAGS += -DITERATIONS=2000

BSP_BASE = ../../bsp

#############################################################
# SRC files
#############################################################

C_SRCS += core_list_join.c
C_SRCS += core_main.c
C_SRCS += core_matrix.c
C_SRCS += core_portme.c
C_SRCS += core_state.c
C_SRCS += core_util.c
C_SRCS += cvt.c
C_SRCS += ee_printf.c

HEADERS += coremark.h
HEADERS += core_portme.h

include $(BSP_BASE)/env/common.mk
