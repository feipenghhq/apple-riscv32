#!/usr/bin/python3
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
## script to run all the instructions
##
##################################################################################################

import os
import subprocess
import shutil

#####################################
# ISA
#####################################

# isa and its run time
rv32ui_isa = [
    ['add'   , 5000],
    ['addi'  , 3000 ],
    ['and'   , 10000],
    ['andi'  , 3000 ],
    ['auipc'  , 3000 ],
    ['beq'  , 5000 ],
    ['bge'  , 5000 ],
    ['bgeu'  , 5000 ],
    ['blt'  , 5000 ],
    ['bltu'  , 5000 ],
    ['bne'  , 5000 ],
    #['fence_i'  , 3000 ],
    ['jal'  , 3000 ],
    ['jalr'  , 3000 ],
    ['lb'   , 3000],
    ['lbu'  , 3000 ],
    ['lh'   , 3000],
    ['lhu'  , 3000 ],
    ['lui'  , 3000 ],
    ['lw'  , 3000 ],
    ['or'   , 10000],
    ['ori'  , 3000 ],
    ['sb'   , 5000],
    ['sh'  , 10000 ],
    ['simple', 2000 ],
    ['sll'   , 10000],
    ['slli'  , 3000 ],
    ['slt'   , 5000],
    ['slti'  , 3000 ],
    ['sltu'   , 5000],
    ['sltiu'  , 3000 ],
    ['sra'   , 10000],
    ['srai'  , 3000 ],
    ['srl'   , 10000],
    ['srli'  , 10000 ],
    ['sub'   , 5000],
    ['sw'  , 10000 ],
    ['xor'   , 10000],
    ['xori'  , 3000 ],
]

rv32mi_isa = [
    ['mcsr', 3000],
    ['ma_addr', 10000],
	['illegal', 10000]
]

rv32si_isa = [
    ['csr', 6000],
    ['scall', 6000],
]

soc_isa = [
    ['software_interrupt', 6000],
    ['timer_interrupt', 6000],
]

rv32um_isa = [
    ['mul', 10000],
    ['mulh', 10000],
    ['mulhsu', 10000],
    ['mulhu', 10000],
]

# architecture
ARCH = {
    'rv32ui': rv32ui_isa,
    'rv32mi': rv32mi_isa,
    'rv32si': rv32si_isa,
    'soc'   : soc_isa,
    'rv32um': rv32um_isa
}

#####################################
# Common variable
#####################################

OUTPUT_DIR  = 'output'

FILES  = [
    'DUT_AppleRISCVSoC.vcd',
    'results.xml',
]

RESULT = 'results.xml'

#####################################
# Utility function
#####################################

def clear_all():
    """ Clear all the output """
    if os.path.isdir(OUTPUT_DIR):
        shutil.rmtree(OUTPUT_DIR)
    os.system("make clean_all")

def move_result(test, arch):
    """ Move the test result to its own directory """
    path = OUTPUT_DIR + '/' + arch + '/' + test
    if not os.path.isdir(path):
        os.makedirs(path)
    for file in FILES:
        tgt = path + '/' + file
        shutil.move(file, tgt)

def run_test(test, arch, runtime):
    """ invoke makefile to run a test """
    cmd = f'make TESTNAME={test} RISCVARCH={arch} RUNTIME={runtime}'
    os.system(cmd)

def check_result():
    """ Check the test result """
    with open(RESULT) as file:
        contents = file.read()
        search_word = "<failure />"
        file.close()
        return not (search_word in contents)

def one_test(test, arch, runtime):
    """ run all the process for one tests """
    os.system("make clean_rom")
    run_test(test, arch, runtime)
    result = check_result()
    move_result(test, arch)
    return result

def one_arch_tests(arch):
    """ run all tests in an arch """
    tests = ARCH[arch]
    results = {}
    for test, runtime in tests:
        result = one_test(test, arch, runtime)
        results[test] = result
    return results

def all_arch_tests(archs):
    results = {}
    for arch in archs.keys():
        result = one_arch_tests(arch)
        results[arch] = result
    return results

def print_result(results):
    translate = {
        True: "PASS",
        False: "FAIL"
    }
    final_pass = True
    print("=======================================")
    print("              Tests Result             ")
    print("=======================================")
    for arch in results.keys():
        print("## ISA: " + arch + " ##")
        tests = results[arch]
        for test in tests.keys():
            rst = translate[tests[test]]
            final_pass = final_pass & tests[test]
            print(test + ": " + str(rst), end='')
            if rst == 'PASS':
                print()
            else:
                print('    <---- ' + test + ' ---->')
    if final_pass:
        print("Congratulation!! All tests PASS")
    else:
        print("Some tests FAILED")

def main():
    """ Run all the test """
    clear_all()
    results = all_arch_tests(ARCH)
    print_result(results)

main()