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
## Memory BFM
##
##################################################################################################

from cocotb.triggers import FallingEdge, RisingEdge, Timer
import random
import copy

class MemoryModel:
    """ Main memory Model """
    def __init__(self, dut, depth, width, bus, debug=False):
        self.dut     = dut
        self.depth   = depth
        self.width   = width
        self.bus     = bus
        self.memory  = {}
        self.debug   = debug
        self.initMem()

    def initMem(self):
        for i in range(self.depth):
            self.memory[i] = i << 2

    def getMemory(self):
        return copy.deepcopy(self.memory)

    async def start(self):
        hsel   = 0
        htrans = 0
        haddr  = 0
        hwrite = 0
        hwdata = 0
        self.bus.driveASignal('HRESP',  0)
        self.bus.driveASignal('HREADY', 1)
        self.bus.driveASignal('HRDATA', 0)
        while True:
            await RisingEdge(self.dut.clk)
            await Timer(1, "ns")
            # process address phase from previous clock
            if (htrans > 1) and hwrite:
                hwdata = self.bus.HWDATA.value.integer
                self.memory[haddr>>2] = hwdata
                if self.debug:
                    self.dut._log.info(f"Main Memory Write - addr: {hex(haddr)}, data: {hwdata}" )
            if (htrans > 1) and not hwrite:
                self.bus.driveASignal('HRDATA', self.memory[haddr>>2])
                if self.debug:
                    self.dut._log.info(f"Main Memory Read - addr: {hex(haddr)}, data: {self.memory[haddr>>2]}" )

            # get the address phase
            htrans = self.bus.HTRANS.value.integer
            haddr  = self.bus.HADDR.value.integer
            hwrite = self.bus.HWRITE.value.integer
