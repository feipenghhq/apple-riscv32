##################################################################################################
##
## Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
##
## ~~~ Hardware in SpinalHDL ~~~
##
## Author: Heqing Huang
## Date Created: 03/30/2021
##
## ================== Description ==================
##
## Basic test suites for the cpu
##
##################################################################################################

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import FallingEdge, Timer

###############################
# Common function
###############################

async def reset(dut, time=20):
    """ Reset the design """
    dut.reset = 1
    await Timer(time, units="ns")
    await FallingEdge(dut.clk)
    dut.reset = 0

def clear_imem(dut, imem_size_to_clear):
    """ Clean the instruction ram """
    print("[TB-INFO]: Cleaning Instruction Memory")
    for i in range(imem_size_to_clear):
        dut.DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol0[i] = 0
        dut.DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol1[i] = 0
        dut.DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol2[i] = 0
        dut.DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol3[i] = 0
    print(f"[TB-INFO]: Cleaning the Instruction Memory done")

def load_imem(dut, imem):
    """ Load the instruction ram data """
    print("[TB-INFO]: Loading Instruction Memory")
    size = len(imem)
    for i in range(size):
        dut.DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol0[i] = imem[i] & 0xFF
        dut.DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol1[i] = (imem[i] >> 8) & 0xFF
        dut.DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol2[i] = (imem[i] >> 16) & 0xFF
        dut.DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol3[i] = (imem[i] >> 24) & 0xFF
    print(f"[TB-INFO]: Loading Instruction Memory done. Memory size is {size}")

def print_register(dut, size=32):
    """ Print the register value """
    for i in range(size):
        val = dut.DUT_AppleRISCVSoC.soc_cpu_core.core_regfile_inst.ram[i].value.integer
        print(f"Register {i}, {val}")

def check_register(dut, expected):
    """ Check the register file with the expected data `"""
    for key, value in expected.items():
        val = dut.DUT_AppleRISCVSoC.soc_cpu_core.core_regfile_inst.ram[key].value.integer
        assert value == val, f"RAM1: Register {key}, Expected: {value}, Actual: {val}"
        print(f"RAM1: Register {key}, Expected: {value}, Actual: {val}")

async def run_test(dut, imm_data, expected_register, runtime=400, imem_size_to_clear=100):
    clear_imem(dut, imem_size_to_clear)
    load_imem(dut, imm_data)
    clock = Clock(dut.clk, 10, units="ns")  # Create a 10us period clock on port clk
    cocotb.fork(clock.start())  # Start the clock
    await reset(dut)
    await Timer(runtime, units="ns")
    check_register(dut, expected_register)
    #print_register(dut, 17)

###############################
# Test suites
###############################

import or1
@cocotb.test()
async def test_or1(dut):
    """ Simple ORI/OR instruction test """
    await run_test(dut, or1.imem_data, or1.expected_register)

import logic_arithmetic1
@cocotb.test()
async def test_logic_arithmetic1(dut):
    await run_test(dut, logic_arithmetic1.imem_data, logic_arithmetic1.expected_register, 300)

import logic_arithmetic2
@cocotb.test()
async def test_logic_arithmetic2(dut):
    await run_test(dut, logic_arithmetic2.imem_data, logic_arithmetic2.expected_register, 300)

import forward_logic1
@cocotb.test()
async def test_forward_logic1(dut):
    """ Test the forward logic """
    await run_test(dut, forward_logic1.imem_data, forward_logic1.expected_register, 300)

import forward_logic2
@cocotb.test()
async def test_forward_logic2(dut):
    """ Test the forward logic """
    await run_test(dut, forward_logic2.imem_data, forward_logic2.expected_register, 200)

import forward_logic3
@cocotb.test()
async def test_forward_logic3(dut):
    """ Test the forward logic """
    await run_test(dut, forward_logic3.imem_data, forward_logic3.expected_register, 350)

import load_store1
@cocotb.test()
async def test_load_store1(dut):
    """ Test the lw/sw logic """
    await run_test(dut, load_store1.imem_data, load_store1.expected_register, 1000)

import load_store2
@cocotb.test()
async def test_load_store2(dut):
    """ Test the sb/sh logic """
    await run_test(dut, load_store2.imem_data, load_store2.expected_register, 1000)

import load_store3
@cocotb.test()
async def test_load_store3(dut):
    """ Test the lb/lh logic """
    await run_test(dut, load_store3.imem_data, load_store3.expected_register, 800)

import branch1
@cocotb.test()
async def test_branch1(dut):
    """ Test the beq/bne logic """
    await run_test(dut, branch1.imem_data, branch1.expected_register, 800)

import branch2
@cocotb.test()
async def test_branch2(dut):
    """ Test the blt/bge logic """
    await run_test(dut, branch2.imem_data, branch2.expected_register, 1000)

import branch3
@cocotb.test()
async def test_branch3(dut):
    """ Test the bltu/bgeu logic """
    await run_test(dut, branch3.imem_data, branch3.expected_register, 400)

import lui_auipc1
@cocotb.test()
async def test_lui_auipc1(dut):
    """ Test the lui/auipc logic """
    await run_test(dut, lui_auipc1.imem_data, lui_auipc1.expected_register, 120)

import jal1
@cocotb.test()
async def test_jal1(dut):
    """ Test the jal logic """
    await run_test(dut, jal1.imem_data, jal1.expected_register, 400)

import jalr1
@cocotb.test()
async def test_jalr1(dut):
    """ Test the jalr logic """
    await run_test(dut, jalr1.imem_data, jalr1.expected_register, 500)

import mul
@cocotb.test()
async def test_mul(dut):
    """ Test the jalr logic """
    await run_test(dut, mul.imem_data, mul.expected_register, 500)

import div
@cocotb.test()
async def test_div(dut):
    """ Test the jalr logic """
    await run_test(dut, div.imem_data, div.expected_register, 2000)

import bpu
@cocotb.test()
async def test_bpu(dut):
    """ Test the jalr logic """
    await run_test(dut, bpu.imem_data, bpu.expected_register, 2000)