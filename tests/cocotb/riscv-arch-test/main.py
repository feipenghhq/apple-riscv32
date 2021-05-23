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

import os
import sys
import subprocess
import shutil
import argparse

#####################################
# Common variable
#####################################

OUTPUT_DIR  = 'output'
RESULT      = 'results.xml'
SOC         = ['arty', 'de2']

subprocess_run = subprocess.Popen("git rev-parse --show-toplevel", shell=True, stdout=subprocess.PIPE)
subprocess_return = subprocess_run.stdout.read()
REPO_ROOT = subprocess_return.decode().rstrip()
TEST_SUITE_PATH = REPO_ROOT + f'/tests/riscv-arch-test/riscv-arch-test/riscv-test-suite/'

#####################################
# Utility function
#####################################

def cmdParser():
    parser = argparse.ArgumentParser(description='Run all the rv32m_i tests')
    parser.add_argument('-dump' , '-d', action='store_true', help='Dump waveform')
    parser.add_argument('-soc', type=str, required=True, nargs='?', help='The FPGA board')
    return parser.parse_args()

#####################################
# Main Class
#####################################

class Run:
    def __init__(self, soc, dump):
        self.soc = soc
        self.FILES = ['results.xml']
        self.cmds = []
        self.arch = 'rv32i_m'
        self.isa = ['I', 'M', 'privilege']
        self.tests = {}
        if dump:
            self.dump = 1
        else:
            self.dump = 0
        if soc == 'arty' and dump:
            self.FILES.append('DUT_arty.vcd')
        if soc == 'de2' and dump:
            self.FILES.append('DUT_de2.vcd')

    def clear_all(self):
        """ Clear all the output """
        os.system(f"make clean_all; rm -rf {OUTPUT_DIR}")

    def move_result(self, test, isa):
        """ Move the test result to its own directory """
        path = f'{OUTPUT_DIR}/{self.soc}/{self.arch}/{isa}/{test}'
        for file in self.FILES:
            target = f'{path}/{file}'
            shutil.move(file, target)

    def get_all_tests(self):
        """ Get all the tests for 1 isa """
        for isa in self.isa:
            path = f'{TEST_SUITE_PATH}/{self.arch}/{isa}/src'
            files = os.listdir(path)
            tests = list(map(lambda x:x[:-2], files))
            self.tests[isa] = tests

    def run_test(self, test, isa):
        """ invoke makefile to run a test """
        cmd = f'make RISCVISA={isa} TESTNAME={test} SOC={self.soc} DUMP={self.dump} '
        self.cmds.append(cmd)
        os.system(cmd)

    def check_result(self):
        """ Check the test result """
        with open(RESULT) as file:
            contents = file.read()
            search_word = "<failure />"
            file.close()
            return not (search_word in contents)

    def one_test(self, test, isa):
        """ run all the process for one tests """
        self.run_test(test, isa)
        result = self.check_result()
        try:
            self.move_result(test, isa)
        except FileNotFoundError:
            pass
        return result

    def one_isa_tests(self, isa):
        """ run all tests in an isa """
        tests = self.tests[isa]
        results = {}
        for test in tests:
            result = self.one_test(test, isa)
            results[test] = result
        return results

    def all_isa_tests(self):
        """ run all isa tests """
        results = {}
        for isa in self.tests.keys():
            result = self.one_isa_tests(isa)
            results[isa] = result
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
        test_num = 0
        for isa in results.keys():
            print("## ISA: " + isa + " ##")
            tests = results[isa]
            for test in tests.keys():
                rst = translate[tests[test]]
                final_pass = final_pass & tests[test]
                print(test + ": " + str(rst), end='')
                if rst == 'PASS':
                    print()
                else:
                    print('    <---- ' + test + ' ---->')
                test_num += 1
        if final_pass:
            print("Congratulation!! All tests PASS")
        else:
            print("Some tests FAILED")

    def clean_up(self):
        """ Clean up files """
        os.system("rm *.verilog")

    def all(self):
        """ Run all the test """
        self.clear_all()
        self.get_all_tests()
        results = self.all_isa_tests()
        for cmd in self.cmds:
            print(cmd)
        self.print_result(results)
        self.clean_up()


if __name__ == '__main__':
    args = cmdParser()
    soc = args.soc
    dump = args.dump
    if not soc in SOC:
        print(f"'{sys.argv[1]}' is not supported")
        print(f"Supported SOC: {SOC}")
    run = Run(soc, dump)
    run.all()