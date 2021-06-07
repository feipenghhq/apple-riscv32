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

def process_rom_file(file_name, file_path):
    """ Split the text and data section for the generated verilog file """

    # Link the instruction rom file to the tb directory
    SRC_FILE = f'/{file_path}/{file_name}.verilog'
    ROM_FILE = os.getcwd() + f'/{file_name}.verilog' # need to link the instruction ram file the the current directory
    if os.path.isfile(ROM_FILE):
        os.remove(ROM_FILE)
    os.symlink(SRC_FILE, ROM_FILE)
    os.system(f"ln -s {REPO_ROOT}/*.bin {os.getcwd()}/.")
    FP = open(f'{file_name}.verilog', "r")
    IRAM_FP = open('instr_ram.rom', "w")
    DRAM_FP = open('data_ram.rom', "w")
    iram = True
    FP.readline() # get ride of the first address line
    for line in FP.readlines():
        if line.rstrip() == "@80000000":
            iram = False
            continue
        if iram:
            IRAM_FP.write(line)
        else:
            DRAM_FP.write(line)
    FP.close()
    IRAM_FP.close()
    DRAM_FP.close()
    os.system("sed -i 's/@2/@0/' instr_ram.rom")

def check_finish(dut):
    """ Check if the rest is finished or not """
    try:
        reg1 = dut.DUT_AppleRISCVSoC.core.regfile_inst.ram[1].value.integer
        reg2 = dut.DUT_AppleRISCVSoC.core.regfile_inst.ram[2].value.integer
        reg3 = dut.DUT_AppleRISCVSoC.core.regfile_inst.ram[3].value.integer
    except ValueError:
        reg1 = 'X'
        reg2 = 'X'
        reg3 = 'X'
    if reg1 == 1 and reg2 == 2 and reg3 == 3:
        return True, True
    if reg1 == 0xf and reg2 == 0xf and reg3 == 0xf:
        return True, False
    return False, False


###############################
# Test suites
###############################

TIMER_DELTA = 1000
TIME_OUT = int(os.getenv('TIME_OUT'))

async def reset(dut, time=20):
    """ Reset the design """
    dut.io_reset = 1
    await Timer(time, units="ns")
    await FallingEdge(dut.io_clk)
    dut.io_reset = 0

@cocotb.test()
def test(dut):
    """ RISCV TEST """
    runtime = 2000
    file_name = os.getenv('TEST_NAME')
    file_path = os.getenv('TEST_PATH')
    total_time = 0
    timeout = False
    process_rom_file(file_name, file_path)

    # Test start
    clock = Clock(dut.io_clk, 10, units="ns")  # Create a 10us period clock on port clk
    cocotb.fork(clock.start())  # Start the clock
    yield reset(dut)
    yield Timer(runtime, units="ns")
    finished, passed = False, False
    while not finished and not timeout:
        yield Timer(TIMER_DELTA, units="ns")
        finished, passed = check_finish(dut)
        total_time += TIMER_DELTA
        if total_time > TIME_OUT:
            timeout = True
            assert False, "Time out"

    # check result
    assert passed, "Test Failed"
