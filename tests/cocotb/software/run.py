##################################################################################################
##
## Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
##
## ~~~ Hardware in SpinalHDL ~~~
##
## Author: Heqing Huang
## Date Created: 04/06/2021
##
## ================== Description ==================
##
## Test using riscv-test
##
##################################################################################################

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import FallingEdge, Timer

import os
import subprocess

subprocess_run = subprocess.Popen("git rev-parse --show-toplevel", shell=True, stdout=subprocess.PIPE)
subprocess_return = subprocess_run.stdout.read()
REPO_ROOT = subprocess_return.decode().rstrip()

###############################
# Common function
###############################

def process_rom_file(name):
    """ Split the text and data section for the generated verilog file """

    FP = open('verilog.rom', "r")
    IRAM_FP = open('instr_ram.rom', "w")
    DRAM_FP = open('data_ram.rom', "w")

    iram = True
    #FP.readline() # get ride of the first address line
    for line in FP.readlines():
        if line.rstrip() == "@80000000":
            iram = False
            continue
        if iram:
            line = line.replace("@2", "@0")
            IRAM_FP.write(line)
        else:
            DRAM_FP.write(line)

    FP.close()
    IRAM_FP.close()
    DRAM_FP.close()

async def reset(dut, time=20):
    """ Reset the design """
    dut.reset = 1
    await Timer(time, units="ns")
    await FallingEdge(dut.clk)
    dut.reset = 0




###############################
# Test suites
###############################

@cocotb.test()
async def run_test(dut):
    runtime = int(os.getenv('RUN_TIME'))
    test = os.getenv('RISCV_ARCH') + '-' + os.getenv('TEST_NAME')
    process_rom_file(test)
    clock = Clock(dut.clk, 10, units="ns")  # Create a 10us period clock on port clk
    cocotb.fork(clock.start())  # Start the clock
    await reset(dut)
    await Timer(runtime, units="ns")
