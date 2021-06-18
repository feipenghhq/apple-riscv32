##################################################################################################
##
## Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
##
## ~~~ Hardware in SpinalHDL ~~~
##
## Author: Heqing Huang
## Date Created: 06/167/2021
##
## ================== Description ==================
##
## Scoreboard for the cache
##
##################################################################################################

from cocotb_bus.scoreboard import Scoreboard

class CacheScoreboard(Scoreboard):
    """ Cache Scoreboard """
    def __init__(self, dut, ram, monitor, debug=False):
        super().__init__(dut)
        self.ram = ram
        self.expected_output = []
        self.dut = dut
        self.add_interface(monitor, self.expected_output)
        self.debug = debug

    def getData(self, addr):
        return self.ram[addr>>2]

    def updateMemory(self, addr, value):
        self.ram[addr>>2] = value
        if self.debug:
            self.log.info(f"Updated memory in scoreboard. Addr: {addr}, DATA: {value}")

    def addExpected(self, tr):
        if self.debug:
            self.log.info(f"Added Expected Transaction: {tr}")
        self.expected_output.append(tr)