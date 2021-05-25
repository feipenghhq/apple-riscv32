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
import sys
import subprocess
import shutil
import argparse

#####################################
# ISA
#####################################

# isa and its run time
rv32ui_isa = [
    'add', 'addi', 'and', 'andi', 'auipc', 'beq', 'bge', 'bgeu', 'blt', 'bltu', 'bne', 'jal', 'jalr',
    'lb', 'lbu', 'lh', 'lhu', 'lui', 'lw', 'or', 'ori', 'sb', 'sh', 'simple', 'sll', 'slli', 'slt',
    'slti', 'sltu', 'sltiu', 'sra', 'srai', 'srl', 'srli', 'sub', 'sw', 'xor', 'xori']
rv32mi_isa = ['mcsr', 'ma_addr', 'illegal',]
rv32si_isa = ['csr', 'scall',]
rv32um_isa = ['mul', 'mulh', 'mulhsu', 'mulhu', 'div', 'divu', 'rem', 'remu',]
soc_isa = ['software_interrupt', 'timer_interrupt',]

# architecture
ARCH = {
    'rv32ui': rv32ui_isa,
    'rv32mi': rv32mi_isa,
    'rv32si': rv32si_isa,
    'rv32um': rv32um_isa,
    'soc'   : soc_isa,
}

#####################################
# Common variable
#####################################

OUTPUT_DIR  = 'output'
RESULT      = 'results.xml'
SOC         = ['arty', 'de2']

#####################################
# Utility function
#####################################

def cmdParser():
    parser = argparse.ArgumentParser(description='Run all the tests')
    parser.add_argument('-dump' , '-d', action='store_true', help='Dump waveform')
    parser.add_argument('-soc', type=str, required=True, nargs='?', help='The FPGA board')
    return parser.parse_args()

#####################################
# Main Class
#####################################

class Run:
    def __init__(self, soc, dump):
        self.soc = soc
        if dump:
            self.dump = 1
        else:
            self.dump = 0
        self.FILES  = ['results.xml']
        if soc == 'arty' and dump:
            self.FILES.append('DUT_arty.vcd')
        if soc == 'de2' and dump:
            self.FILES.append('DUT_de2.vcd')
        self.cmds = []

    def clear_all(self):
        """ Clear all the output """
        if os.path.isdir(OUTPUT_DIR):
            shutil.rmtree(OUTPUT_DIR)
        os.system("make clean_all")

    def move_result(self, test, arch):
        """ Move the test result to its own directory """
        path = OUTPUT_DIR + '/' + self.soc + '/' + arch + '/' + test
        if not os.path.isdir(path):
            os.makedirs(path)
        for file in self.FILES:
            tgt = path + '/' + file
            shutil.move(file, tgt)

    def run_test(self, test, arch):
        """ invoke makefile to run a test """
        cmd = f'make TESTNAME={test} RISCVARCH={arch} SOC={self.soc} DUMP={self.dump}'
        self.cmds.append(cmd)
        os.system(cmd)

    def check_result(self):
        """ Check the test result """
        with open(RESULT) as file:
            contents = file.read()
            search_word = "<failure />"
            file.close()
            return not (search_word in contents)

    def one_test(self, test, arch):
        """ run all the process for one tests """
        self.run_test(test, arch)
        result = self.check_result()
        self.move_result(test, arch)
        return result

    def one_arch_tests(self, arch):
        """ run all tests in an arch """
        tests = ARCH[arch]
        results = {}
        for test in tests:
            result = self.one_test(test, arch)
            results[test] = result
        return results

    def all_arch_tests(self, archs):
        results = {}
        for arch in archs.keys():
            result = self.one_arch_tests(arch)
            results[arch] = result
        return results

    def print_result(self, results):
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

    def all(self):
        """ Run all the test """
        self.clear_all()
        results = self.all_arch_tests(ARCH)
        for cmd in self.cmds:
            print(cmd)
        self.print_result(results)


if __name__ == '__main__':
    args = cmdParser()
    soc = args.soc
    dump = args.dump
    if not soc in SOC:
        print(f"'{sys.argv[1]}' is not supported")
        print(f"Supported SOC: {SOC}")
    run = Run(soc, dump)
    run.all()