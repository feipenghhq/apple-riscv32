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
## AHB Bus Function
##
##################################################################################################

import cocotb
import cocotb_bus
from cocotb.clock import Clock
from cocotb.triggers import FallingEdge, RisingEdge, Timer
from cocotb_bus.drivers import BusDriver
from cocotb_bus.monitors import BusMonitor
from cocotb_bus.bus import Bus
from CacheScoreboard import *


#########################################################################

class AHB3Signal(object):
    """ Signal of AHB3 """
    signal = ['HSEL', 'HWRITE', 'HADDR', 'HWDATA', 'HRDATA', 'HSIZE',
              'HTRANS', 'HPROT', 'HBURST', 'HMASTLOCK', 'HREADY', 'HREADYOUT']

class AHB3Bus(Bus):
    def __init__(self, entity, name):
        super().__init__(entity, name, AHB3Signal.signal)

    def driveASignal(self, name, value, bus_separator = '_'):
        signalName = self._name + bus_separator + name
        setattr(self._entity, signalName, value)

class AHB3ReqTrans(object):
    """ AHB Lite 3 Request Transaction """
    NONSEQ = 2
    def __init__(self, active, addr, data, write, size=2, burst=0):
        self.HSEL   = True if active else False
        self.HWRITE = 1 if write else 0
        self.HADDR  = addr
        self.HWDATA = data
        self.HSIZE  = size
        self.HTRANS = AHB3ReqTrans.NONSEQ if active else 0
        self.HBURST = burst
        self.HPROT  = 0
        self.HREADY = 1
        self.HMASTLOCK = 0

class AHB3RespTrans(object):
    """ AHB Lite 3 Response Transaction """
    def __init__(self, addr, data):
        self.HADDR  = addr
        self.HRDATA = data

    def __str__(self):
        return f"data: {self.HRDATA}, addr: {self.HADDR}"

    def __eq__(self, other):
        if not isinstance(other, AHB3RespTrans):
            return False
        return (self.HADDR == other.HADDR) and (self.HRDATA == other.HRDATA)

class AHB3Generator(object):
    """ AHB Lite 3 Generator
        Only support single transcation right now.
    """
    def __init__(self, clk, driver, scoreboard):
        self.clk        = clk
        self.count      = 0
        self.readCount  = 0
        self.writeCount = 0
        self.driver     = driver
        self.scoreboard = scoreboard
        self.wait       = 100
        self.log        = cocotb.log

    def reset(self):
        self.driver.append(AHB3ReqTrans(False, 0x0, 0x0, False))

    async def read(self, addr):
        self.log.debug(f"Generate read request at addr {addr}")
        # Push the expected read resp transaction to scoreboard
        tr = AHB3RespTrans(addr, self.scoreboard.getData(addr))
        self.scoreboard.addExpected(tr)
        # Send the transaction
        addrPhase = AHB3ReqTrans(True, addr, 0x0, False)
        dataPhase = AHB3ReqTrans(False, addr, 0x0, False)
        await self.driver.send(addrPhase)
        await self.driver.send(dataPhase)
        # FIXME self.driver._wait_for_signal(self.driver.bus.HREADYOUT)
        await Timer(self.wait, "ns") # FIXME


    async def write(self, addr, data):
        self.log.info(f"Generate write request at addr {hex(addr)} with data {data}")
        # update data in coreboard
        self.scoreboard.updateMemory(addr, data)
        # Send the transaction
        addrPhase = AHB3ReqTrans(True, addr, 0x0, True)
        dataPhase = AHB3ReqTrans(False, 0x0, data, False)
        await self.driver.send(addrPhase)
        await self.driver.send(dataPhase)
        await Timer(self.wait, "ns") # FIXME


class AHB3Driver(BusDriver):
    """ AHB Lite3 Driver """
    _signals = AHB3Signal.signal

class AHB3Monitor(BusMonitor):
    """ AHB Lite3 BusMonitor """
    _signals = AHB3Signal.signal

    def readHSEL(self):
        try:
            return self.bus.HSEL.value.integer
        except ValueError:
            return 0

    async def _monitor_recv(self):
        hasReadReq  = False
        addr        = 0

        while True:
            await RisingEdge(self.clock)
            if self.bus.HREADYOUT.value and hasReadReq:
                hasReadReq = False
                data = self.bus.HRDATA.value.integer
                self.log.info(f"Captured read data: {data}, addr: {hex(addr)}")
                tr = AHB3RespTrans(addr, data)
                self._recv(tr)
            if (not self._reset.value) and self.readHSEL() and (not self.bus.HWRITE.value):
                hasReadReq = True
                addr = self.bus.HADDR.value.integer

#################################################################

async def reset(dut, time=20):
    """ Reset the design """
    dut.reset = 1
    await Timer(time, units="ns")
    await RisingEdge(dut.clk)
    dut.reset = 0
    await RisingEdge(dut.clk)

@cocotb.test()
async def AHBTest(dut):

    cacheAhbMon = AHB3Monitor(dut, 'io_cache_ahb', dut.clk, dut.reset)
    cacheSB     = CacheScoreboard(dut, [x for x in range(4098)], cacheAhbMon)
    cacheAhbDrv = AHB3Driver(dut, 'io_cache_ahb', dut.clk)
    cacheAhbGen = AHB3Generator(dut.clk, cacheAhbDrv, cacheSB)
    cacheAhbGen.reset()

    clock = Clock(dut.clk, 20, units="ns")  # Create a 20 ns period clock on port clk
    cocotb.fork(clock.start())
    await reset(dut)
    await cacheAhbGen.read(0)
    await cacheAhbGen.write(1, 1)
    await Timer(1000, "ns")
