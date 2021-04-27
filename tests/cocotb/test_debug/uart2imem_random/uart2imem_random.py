##################################################################################################
##
## Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
##
## ~~~ Hardware in SpinalHDL ~~~
##
## Author: Heqing Huang
## Date Created: 04/27/2021
##
## ================== Description ==================
##
## Test the uart2imem function with sending random data into instruction
##
## This test uses the cocotbext-uart from alexforencich
## https://github.com/alexforencich/cocotbext-uart
##
##################################################################################################

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import FallingEdge, Timer
from cocotbext.uart import UartSource, UartSink
import random

import os
import subprocess

subprocess_run = subprocess.Popen("git rev-parse --show-toplevel", shell=True, stdout=subprocess.PIPE)
subprocess_return = subprocess_run.stdout.read()
REPO_ROOT = subprocess_return.decode().rstrip()

###############################
# Common function
###############################

async def reset(dut, time=20):
    """ Reset the design """
    dut.reset = 1
    await Timer(time, units="ns")
    await FallingEdge(dut.clk)
    dut.reset = 0

async def sentRandom4byte(dut, uart, refQue):
    """ Send random data to instruction rom"""
    word = []
    for idx in range(0, 4):
        word.append(random.randint(0, 0xFF))
    word1 = word[0] + (word[1] << 8) + (word[2] << 16) + (word[3] << 24)
    refQue.append(hex(word1))
    await uart.write(bytes(word))

def checkImem(dut, refQue):
    """ Check the data in imem rom with the data in refQue """
    for i in range(len(refQue)):
        data = hex(dut.DUT_apple_riscv_soc.soc_imem_inst.ram[i].value.integer)
        assert(data == refQue[i])
        print(f"Data at location {i}: {data}")


###############################
# Test suites
###############################

@cocotb.test()
async def uart2imem_random(dut):
    """
        Test the uart2imem function by sending random data into instruction ram through uart
        and read it back to check the result
    """
    runtime = int(os.getenv('RUN_TIME'))
    clock = Clock(dut.clk, 20, units="ns")  # Create a 20 ns period clock on port clk

    cocotb.fork(clock.start())  # Start the clock
    await reset(dut)            # wait for reset complete
    dut.load_imem = 1           # enable load_imem

    # Uart TX env
    uart_source = UartSource(dut.uart_port_rxd, baud=115200, bits=8)

    # Sent Push data into instruction rom
    refQue = []
    for i in range(10):
        await sentRandom4byte(dut, uart_source, refQue)
    await uart_source.wait()

    # Check result
    checkImem(dut, refQue)

    await Timer(runtime, units="ns")

