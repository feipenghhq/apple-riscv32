##################################################################################################
##
## Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
##
## ~~~ Hardware in SpinalHDL ~~~
##
## Author: Heqing Huang
## Date Created: 06/17/2021
##
## ================== Description ==================
##
## Test environment
##
##################################################################################################

import cocotb
import cocotb_bus
from cocotb.clock import Clock
from cocotb.triggers import FallingEdge, RisingEdge, Timer
import random
import os

from AhbBFM import AHB3Bus, AHB3Driver, AHB3Generator, AHB3Monitor
from MemoryBFM import *
from CacheScoreboard import *

#########################################################################

import logging
from logging.handlers import WatchedFileHandler
from cocotb.log import SimLogFormatter

root_logger = logging.getLogger()

file_handler = WatchedFileHandler("run.log", mode="w")
file_handler.setFormatter(SimLogFormatter())
root_logger.addHandler(file_handler)

#########################################################################

if int(os.environ['DEBUG']) == 1:
    debug = True
else:
    debug = False

async def reset(dut, time=20):
    """ Reset the design """
    dut.reset = 1
    await Timer(time, units="ns")
    await RisingEdge(dut.clk)
    dut.reset = 0
    await RisingEdge(dut.clk)

def setup(dut, memDepth = 4096):
    memoryAhbBus = AHB3Bus(dut, 'io_mem_ahb')
    memory       = MemoryModel(dut, memDepth, 32, memoryAhbBus, debug=debug)
    cacheAhbMon  = AHB3Monitor(dut, 'io_cache_ahb', dut.clk, reset=dut.reset, debug=debug)
    cacheSB      = CacheScoreboard(dut, memory.getMemory(), cacheAhbMon, debug=debug)
    cacheAhbDrv  = AHB3Driver(dut, 'io_cache_ahb', dut.clk)
    cacheAhbGen  = AHB3Generator(dut.clk, cacheAhbDrv, cacheSB)
    cacheAhbGen.reset()
    clock = Clock(dut.clk, 20, units="ns")  # Create a 20 ns period clock on port clk
    cocotb.fork(clock.start())
    cocotb.fork(memory.start())
    return cacheAhbGen
