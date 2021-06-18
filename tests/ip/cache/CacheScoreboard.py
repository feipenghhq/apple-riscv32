from cocotb_bus.scoreboard import Scoreboard

class CacheScoreboard(Scoreboard):
    """ Cache Scoreboard """
    def __init__(self, dut, ram, monitor):
        super().__init__(dut)
        self.ram = ram
        self.expected_output = []
        self.dut = dut
        self.add_interface(monitor, self.expected_output)

    def getData(self, addr):
        return self.ram[addr>>2]

    def updateMemory(self, addr, value):
        self.ram[addr>>2] = value
        self.log.debug(f"Updated memory in scoreboard. Addr: {addr}, DATA: {value}")

    def addExpected(self, tr):
        self.log.debug(f"Added Expected Transaction: {tr}")
        self.expected_output.append(tr)