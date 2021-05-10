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

    # Link the instruction rom file to the tb directory
    SRC_FILE = REPO_ROOT + '/tests/riscv-tests-simple/generated/' + name + ".verilog"
    ROM_FILE = os.getcwd() + '/verilog.rom' # need to link the instruction ram file the the current directory
    if os.path.isfile(ROM_FILE):
        os.remove(ROM_FILE)
    os.symlink(SRC_FILE, ROM_FILE)
    os.system(f"ln -s {REPO_ROOT}/*.bin {os.getcwd()}/.")

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
            val = dut.DUT_AppleRISCVSoC.soc_cpu_core.core_regfile_inst.ram[i].value.integer
            print(f"Register {i}, {hex(val)}")
        except ValueError:
            print(f"Register {i}, XXXX")

def check_register(dut, expected):
    """ Check the register file with the expected data """
    for key, value in expected.items():
        val = dut.DUT_AppleRISCVSoC.soc_cpu_core.core_regfile_inst.ram[key].value.integer
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
    test = os.getenv('RISCV_ARCH') + '-' + os.getenv('TEST_NAME')
    process_rom_file(test)
    clock = Clock(dut.clk, 10, units="ns")  # Create a 10us period clock on port clk
    cocotb.fork(clock.start())  # Start the clock
    await reset(dut)
    await Timer(runtime, units="ns")
    # This pattern is defined in the TEST_PASS macro
    expected_register = {
        1 : 1,
        2 : 2,
        3 : 3,
    }
    print_register(dut, 32)
    check_register(dut, expected_register)
