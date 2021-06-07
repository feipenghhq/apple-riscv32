##################################################################################################
##
## Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
##
## ~~~ Hardware in SpinalHDL ~~~
##
## Author: Heqing Huang
## Date Created: 06/06/2021
##
## ================== Description ==================
##
## Test the uart debug function
##
##################################################################################################

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import FallingEdge, Timer
from cocotbext.uart import UartSource, UartSink
import random

def to4Bytes(num):
    arr = []
    for i in range(4):
        arr.append(num & 0xFF)
        num = num >> 8
    return bytearray(arr)

###############################
# Test suites
###############################

async def reset(dut, time=20):
    """ Reset the design """
    dut.io_reset = 1
    await Timer(time, units="ns")
    await FallingEdge(dut.io_clk)
    dut.io_reset = 0

@cocotb.test()
def uartDbg_test(dut):
    """ RISCV TEST """
    # Test start
    clock = Clock(dut.io_clk, 20, units="ns")  # Create a 20 ns period clock on port clk
    cocotb.fork(clock.start())  # Start the clock
    yield reset(dut)

    uart_source = UartSource(dut.io_uart0_rxd, baud=115200, bits=8)
    num = 2
    golden = []
    # start
    dut.DUT_AppleRISCVSoC.io_load_imem = 1
    yield uart_source.write(to4Bytes(0xFFFFFFFF))
    yield uart_source.wait()
    # instruction
    for _ in range(num):
        data = random.randint(0, 0xFFFFFFFF)
        print(f"Sending data {hex(data)}")
        golden.append(data)
        yield uart_source.write(to4Bytes(data))
        yield uart_source.wait()
    # end
    yield uart_source.write(to4Bytes(0xFFFFFFFE))
    yield uart_source.wait()

    # Check result
    print("Check result")
    for i in range(num):
        instr = dut.DUT_AppleRISCVSoC.soc_imem.ram_symbol0[i].value
        instr = instr + (dut.DUT_AppleRISCVSoC.soc_imem.ram_symbol1[i].value << 8)
        instr = instr + (dut.DUT_AppleRISCVSoC.soc_imem.ram_symbol2[i].value << 16)
        instr = instr + (dut.DUT_AppleRISCVSoC.soc_imem.ram_symbol3[i].value << 24)
        print(hex(instr))