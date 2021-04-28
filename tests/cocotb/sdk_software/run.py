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

def process_rom_file(name, main):
    """ Split the text and data section for the generated verilog file """

    # Link the instruction rom file to the tb directory
    SRC_FILE = REPO_ROOT + '/sdk/software/' + name + '/' + main + ".verilog"
    ROM_FILE = os.getcwd() + '/verilog.rom' # need to link the instruction ram file the the current directory
    if os.path.isfile(ROM_FILE):
        os.remove(ROM_FILE)
    os.symlink(SRC_FILE, ROM_FILE)

    FP = open('verilog.rom', "r")
    IRAM_FP = open('instr_ram.rom', "w")
    DRAM_FP = open('data_ram.rom', "w")

    iram = True
    for line in FP.readlines():
        if line.rstrip() == "@01000000":
            iram = False
            continue
        if iram:
            IRAM_FP.write(line)
        else:
            DRAM_FP.write(line)


    FP.close()
    IRAM_FP.close()
    DRAM_FP.close()

def print_register(dut, size=32):
    """ Print the register value """
    for i in range(size):
        try:
            val = dut.DUT_apple_riscv_soc.cpu_core.regfile_inst.ram[i].value.integer
        except ValueError:
            val = 'X'
        print(f"Register {i}, {hex(val)}")

def check_register(dut, expected):
    """ Check the register file with the expected data """
    for key, value in expected.items():
        val = dut.DUT_apple_riscv_soc.cpu_core.regfile_inst.ram[key].value.integer
        assert value == val, f"RAM1: Register {key}, Expected: {value}, Actual: {val}"
        print(f"RAM1: Register {key}, Expected: {value}, Actual: {val}")

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
    name = os.getenv('PROGRAM')
    top = os.getenv('PROGRAM_TOP')
    process_rom_file(name, top)
    clock = Clock(dut.clk, 10, units="ns")  # Create a 10us period clock on port clk
    cocotb.fork(clock.start())  # Start the clock
    await reset(dut)
    await Timer(runtime, units="ns")
