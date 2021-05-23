##################################################################################################
##
## Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
##
## ~~~ Hardware in SpinalHDL ~~~
##
## Author: Heqing Huang
## Date Created: 05/22/2021
##
## ================== Description ==================
##
## Test using riscv-arch-test
##
##################################################################################################

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import FallingEdge, Timer
import filecmp

import os
import subprocess

subprocess_run = subprocess.Popen("git rev-parse --show-toplevel", shell=True, stdout=subprocess.PIPE)
subprocess_return = subprocess_run.stdout.read()

RVARCH   = 'rv32i_m'
REPO_ROOT = subprocess_return.decode().rstrip()
SRC_PATH = REPO_ROOT + f'/tests/riscv-arch-test/riscv-arch-test/work/{RVARCH}'
REF_PATH = REPO_ROOT + f'/tests/riscv-arch-test/riscv-arch-test/riscv-test-suite/{RVARCH}'


# Process input variable
runtime   = int(os.getenv('RUN_TIME'))
isa       = os.getenv('RISCV_ISA')
name      = os.getenv('TEST_NAME')
signature = f'{name}.signature'
SoC       = os.getenv('SoC')
outdir    = f'output/{SoC}/{RVARCH}/{isa}'

def process_rom_file(isa, name):
    """ Split the text and data section for the generated verilog file """
    # Link the instruction rom file to the tb directory
    SRC_FILE = f'{SRC_PATH}/{isa}/{name}.elf.verilog'
    ROM_FILE = os.getcwd() + f'/{name}.verilog' # need to link the instruction ram file the the current directory
    if os.path.isfile(ROM_FILE):
        os.remove(ROM_FILE)
    os.symlink(SRC_FILE, ROM_FILE)
    os.system(f"ln -s {REPO_ROOT}/*.bin {os.getcwd()}/.")
    FP = open(f'{name}.verilog', "r")
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

def get_memory_data(dut, addr):
    """ Get the memory data for a specific address"""
    addr = addr >> 2 # From byte address to word address
    data = 0
    """Arty use Block RAM as Data memory"""
    if SoC == 'arty':
        memory_inst = dut.DUT_AppleRISCVSoC.soc_dmem_inst
        data = memory_inst.ram_symbol0[addr].value
        data = data | (memory_inst.ram_symbol1[addr].value << 8)
        data = data | (memory_inst.ram_symbol2[addr].value << 16)
        data = data | (memory_inst.ram_symbol3[addr].value << 24)
    """DE2 use SRAM as Data memory"""
    if SoC == 'de2':
        memory_inst = dut.IS61LV25616
        data = memory_inst.RAM_0[addr].value
        data = data | (memory_inst.RAM_1[addr].value << 8)
        data = data | (memory_inst.RAM_0[addr+1].value << 16)
        data = data | (memory_inst.RAM_1[addr+1].value << 24)
    return data

BEGIN_SIGNATURE_PTR = 0x0
END_SIGNATURE_PTR   = 0x4

def check_finish(dut):
    """ Check if the test has finished or not """
    begin_signature = get_memory_data(dut, BEGIN_SIGNATURE_PTR)
    end_signature = get_memory_data(dut, END_SIGNATURE_PTR)
    if begin_signature > 0x8000000F and end_signature > begin_signature:
        return True
    return False

def dump_signature(dut):
    """ Dump the signature to the output directory """
    begin_signature = get_memory_data(dut, BEGIN_SIGNATURE_PTR)
    end_signature = get_memory_data(dut, END_SIGNATURE_PTR)
    print(f"begin_signature: {hex(begin_signature)}, end_signature: {hex(end_signature)}")
    file_name = f'{outdir}/{name}/{name}.signature'
    os.system(f"mkdir -p {outdir}/{name}")
    FP = open(file_name, "w")
    for addr in range(begin_signature, end_signature, 4):
        addr = addr & 0x07FFFFFFF
        data = hex(get_memory_data(dut, addr))
        FP.write(data[2:].zfill(8) + '\n')
    FP.close()

def check_signature(dut):
    """ Check the signature file agains reference """
    file_name = f'{outdir}/{name}/{name}.signature'
    ref_name = f'{REF_PATH}/{isa}/references/{name}.reference_output'
    assert filecmp.cmp(file_name, ref_name), "Signature does not match with reference file"


###############################
# Test suites
###############################

async def reset(dut, time=20):
    """ Reset the design """
    dut.reset = 1
    await Timer(time, units="ns")
    await FallingEdge(dut.clk)
    dut.reset = 0

TIMER_DELTA = 2000
TIME_OUT = 500000

@cocotb.test()
def riscv_arch_test(dut):
    """ RISCV TEST """
    total_time = 0
    timeout = False
    # process input information
    process_rom_file(isa, name)

    # Test start
    clock = Clock(dut.clk, 10, units="ns")  # Create a 10us period clock on port clk
    cocotb.fork(clock.start())  # Start the clock
    yield reset(dut)
    yield Timer(runtime, units="ns")
    total_time += runtime
    while not check_finish(dut) and not timeout:
        yield Timer(TIMER_DELTA, units="ns")
        total_time += TIMER_DELTA
        if total_time > TIME_OUT:
            timeout = True

    # Check result
    dump_signature(dut)
    check_signature(dut)



