##################################################################################################
##
## Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
##
## ~~~ Hardware in SpinalHDL ~~~
##
## Author: Heqing Huang
## Date Created: 06/16/2021
##
## ================== Description ==================
##
## Basic Tests for Cache
##
##################################################################################################

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import FallingEdge, RisingEdge, Timer
from env import *

@cocotb.test()
async def cacheReadMissHit(dut):
    """ Cache Read Test Miss then Hit
        - Start with empty cache.
        - Test read miss cases
        - and then test read hit
    """
    cacheAhbGen = setup(dut)
    await reset(dut)
    await cacheAhbGen.read(0x0)
    await cacheAhbGen.read(0x8)
    await cacheAhbGen.read(0x0)
    await cacheAhbGen.read(0x4)
    await cacheAhbGen.read(0x8)
    await cacheAhbGen.read(0xC)

@cocotb.test()
async def cacheWriteHit(dut):
    """ Cache Write Hit
        - Start with empty cache.
        - Test write hit
    """
    cacheAhbGen = setup(dut)
    await reset(dut)
    await cacheAhbGen.read(0x0)
    await cacheAhbGen.write(0x0, 0x12)
    await cacheAhbGen.read(0x0)
    await cacheAhbGen.write(0x4, 0x34)
    await cacheAhbGen.read(0x4)

@cocotb.test()
async def cacheWriteMiss(dut):
    """ Cache Write Miss
        - Start with empty cache.
        - Test write miss cases
    """
    cacheAhbGen = setup(dut)
    await reset(dut)
    await cacheAhbGen.write(0x8, 0xaa)
    await cacheAhbGen.write(0xc, 0xbb)
    await cacheAhbGen.read(0x8)
    await cacheAhbGen.read(0xc)

@cocotb.test()
async def cacheReadSet(dut):
    """ Cache Read same Set twice
    """
    cacheAhbGen = setup(dut)
    await reset(dut)
    await cacheAhbGen.read(0x0)
    await cacheAhbGen.read(0x400)

@cocotb.test()
async def cacheReadSetReplace(dut):
    """ Cache Read same Set multiple time and requires a replace
    """
    cacheAhbGen = setup(dut)
    await reset(dut)
    await cacheAhbGen.read(0x0)
    await cacheAhbGen.read(0x400)
    await cacheAhbGen.read(0x800)

@cocotb.test()
async def cacheReadSetReplaceDirty(dut):
    """ Cache Read same Set multiple time and requires a replace with dirty cache line
    """
    cacheAhbGen = setup(dut)
    await reset(dut)
    await cacheAhbGen.write(0x0, 0xaa)
    await cacheAhbGen.write(0x4, 0xaa)
    await cacheAhbGen.write(0x400, 0xbb)
    await cacheAhbGen.read(0x0)
    await cacheAhbGen.read(0x400)
    await cacheAhbGen.read(0x800)
    await cacheAhbGen.read(0x400)
