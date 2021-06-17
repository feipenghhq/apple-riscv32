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
## Test the cache module
##
##################################################################################################

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import FallingEdge, RisingEdge, Timer
import random

#########################################################################

WRITE = 1
READ  = 0

#########################################################################

# -------------------------------
# Cache AHB driver
# -------------------------------
class AHBLite3Driver:

    def __init__(self, dut, ahbName, scoreboard):
        self.ahbName = ahbName
        self.dut = dut
        self.scoreboard = scoreboard
        setattr(dut, self.ahbName + '_HSEL',      0)
        setattr(dut, self.ahbName + '_HWRITE',    0)
        setattr(dut, self.ahbName + '_HADDR',     0)
        setattr(dut, self.ahbName + '_HWDATA',    0)
        setattr(dut, self.ahbName + '_HSIZE',     0)
        setattr(dut, self.ahbName + '_HTRANS',    0)
        setattr(dut, self.ahbName + '_HPROT',     0)
        setattr(dut, self.ahbName + '_HBURST',    0)
        setattr(dut, self.ahbName + '_HMASTLOCK', 0)
        setattr(dut, self.ahbName + '_HREADY',    1)

    async def ahbNonSeq(self, addr, wdata, write, size=2):
        """ Read the cache """
        await FallingEdge(self.dut.clk)
        setattr(self.dut, self.ahbName + '_HSEL',    1)
        setattr(self.dut, self.ahbName + '_HTRANS',  2)
        setattr(self.dut, self.ahbName + '_HWRITE',  write)
        setattr(self.dut, self.ahbName + '_HADDR',   addr)
        setattr(self.dut, self.ahbName + '_HWDATA',  wdata)
        setattr(self.dut, self.ahbName + '_HSIZE',   size)
        if not write:
            self.scoreboard.addValue(addr, addr)
            print(f"Cache read - Addr: {addr}")
        await FallingEdge(self.dut.clk)
        setattr(self.dut, self.ahbName + '_HSEL',    0)
        setattr(self.dut, self.ahbName + '_HTRANS',  0)

    async def ahbMonitor(self):
        num = 0
        address_phase = []
        hsel   = 0
        htrans = 0
        hrdata = 0
        hreadyout = 0

        def readAHBData():
            nonlocal hreadyout, hrdata, address_phase
            if len(address_phase) > 0:
                hreadyout = getattr(self.dut, self.ahbName + '_HREADYOUT').value.integer
                try:
                    hrdata = getattr(self.dut, self.ahbName + '_HRDATA').value.integer
                except ValueError:
                    hrdata = 'X'
                if hreadyout:
                    address_phase.pop(0)
                    #print(f"Cache read - Read DATA: {hrdata}" )
                    self.scoreboard.checkValue(hrdata)

        def readAHB():
            nonlocal hsel, htrans, num, address_phase
            hsel   = getattr(self.dut, self.ahbName + '_HSEL').value.integer
            htrans = getattr(self.dut, self.ahbName + '_HTRANS').value.integer
            if hsel and htrans > 1:
                address_phase.append(num)
                num += 1

        while True:
            await FallingEdge(self.dut.clk)
            readAHBData()
            await RisingEdge(self.dut.clk)
            readAHB()

# -------------------------------
# Main memory Model
# -------------------------------
class MemoryModel:
    def __init__(self, dut, depth, width, ahbName):
        self.depth = depth
        self.width = width
        self.ahbName = ahbName
        self.dut = dut
        self.memory = {}

    def initMem(self):
        for i in range(self.depth):
            self.memory[i] = i

    async def start(self):
        self.initMem()
        setattr(self.dut, self.ahbName + '_HRESP',  0)
        setattr(self.dut, self.ahbName + '_HREADYOUT',  1)

        hsel   = 0
        htrans = 0
        haddr  = 0
        hwrite = 0
        hwdata = 0

        def readAHB():
            nonlocal hsel, htrans, haddr, hwrite, hwdata
            hsel   = getattr(self.dut, self.ahbName + '_HSEL').value.integer
            htrans = getattr(self.dut, self.ahbName + '_HTRANS').value.integer
            haddr  = getattr(self.dut, self.ahbName + '_HADDR').value.integer
            hwrite = getattr(self.dut, self.ahbName + '_HWRITE').value.integer
            hwdata = getattr(self.dut, self.ahbName + '_HWDATA').value.integer

        def accMem():
            if hsel and (htrans > 1) and hwrite:
                print(f"Main Memory Write - Addr: {haddr}, WDATA: {hwdata}" )
                self.memory[haddr] = hwdata
            if hsel and (htrans > 1) and not hwrite:
                print(f"Main Memory Read - Addr: {haddr}" )
                setattr(self.dut, self.ahbName + '_HRDATA', self.memory[haddr])

        while True:
            await FallingEdge(self.dut.clk)
            accMem()    # process address phase from previous clock
            readAHB()   # get the address phase information

# -------------------------------
# Score Board
# -------------------------------

class ScoreBoard:
    def __init__(self):
        self.expectedValue = []

    def addValue(self, addr, value):
        self.expectedValue.append((addr, value))

    def checkValue(self, value):
        addr, expected = self.expectedValue.pop(0)
        assert value == expected, f"[ERROR] Get wrong read data value at address: {addr}. Expected: {expected}, Actual: {value}"
        print(f"[INFO] Get correct read data value at address: {addr}, data: {value}")

#########################################################################


async def reset(dut, time=20):
    """ Reset the design """
    dut.reset = 1
    await Timer(time, units="ns")
    await FallingEdge(dut.clk)
    dut.reset = 0

@cocotb.test()
def cacheReadBasic(dut):
    """ Cache Read Test 1
        - Start with empty cache.
        - Test read miss cases
        - Test read hit
    """

    clock = Clock(dut.clk, 20, units="ns")  # Create a 20 ns period clock on port clk
    cocotb.fork(clock.start())  # Start the clock

    scoreboard = ScoreBoard()
    cache_ahb = AHBLite3Driver(dut, 'io_cache_ahb', scoreboard)
    memory = MemoryModel(dut, 256, 32, 'io_mem_ahb')

    cocotb.fork(memory.start())
    cocotb.fork(cache_ahb.ahbMonitor())

    yield reset(dut)
    yield Timer(100, units="ns")
    yield cache_ahb.ahbNonSeq(0x0, 0, READ)
    yield Timer(100, units="ns")
    yield cache_ahb.ahbNonSeq(0x8, 0, READ)
    yield Timer(100, units="ns")

    yield cache_ahb.ahbNonSeq(0x0, 0, READ)
    yield Timer(100, units="ns")
    yield cache_ahb.ahbNonSeq(0x4, 0, READ)
    yield Timer(100, units="ns")
    yield cache_ahb.ahbNonSeq(0x8, 0, READ)
    yield Timer(100, units="ns")
    yield cache_ahb.ahbNonSeq(0xC, 0, READ)

    yield Timer(1000, units="ns")

