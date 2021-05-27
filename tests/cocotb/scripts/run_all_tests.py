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
import argparse

from get_all_tests import *

#####################################
# Utility function
#####################################

def cmdParser():
    parser = argparse.ArgumentParser(description='Run all the tests')
    parser.add_argument('-soc', type=str, required=True, nargs='?', help='The FPGA board')
    parser.add_argument('-timeout', '-to', type=str, required=True, nargs='?', help='Timeout value')
    parser.add_argument('-test', '-t', type=str, required=True, nargs='?', help='The test you want to run')
    return parser.parse_args()

#####################################
# Main Class
#####################################

class AllTests:
    def __init__(self, soc, timeout, f_get_all_tests):
        self.soc = soc
        self.timeout = timeout
        self.cmds = {}
        self.results = {}
        self.failed_tests = []
        # a hash table containing all the test name and it's path
        self.tests = f_get_all_tests()

    def check_result(self):
        """ Check the test result """
        with open('results.xml') as file:
            contents = file.read()
            search_word = "<failure />"
            file.close()
            return not (search_word in contents)

    def run_test(self, test, path):
        """ invoke makefile to run a test """
        cmd = f'make TIMEOUT={self.timeout} TESTNAME={test} TESTPATH={path} SOC={self.soc} DUMP=0'
        self.cmds[test] = cmd
        os.system(cmd)
        self.results[test] = self.check_result()

    def run_all_tests(self):
        """ Run all the tests """
        print(self.tests)
        for test, path in self.tests.items():
            self.run_test(test, path)

    def print_result(self):
        translate = {
            True: "PASS",
            False: "FAIL"
        }
        final_pass = True
        print("=======================================")
        print("              Tests Result             ")
        print("=======================================")
        for test, result in self.results.items():
            pass_or_fail = translate[result]
            final_pass = final_pass & result
            print(test + ": " + str(pass_or_fail), end='')
            if pass_or_fail == 'PASS':
                print()
            else:
                print('    ---- ' + test + ' ----')
                self.failed_tests.append(test)
        if final_pass:
            print("Congratulation!! All tests PASS")
            os.system("touch .PASS")
        else:
            print("Some tests FAILED")
            os.system("touch .FAIL")

    def allTasks(self):
        """ Run all the Tasks """
        os.system("make clean_all")
        self.run_all_tests()
        self.print_result()
        os.system("rm -rf *.verilog")

if __name__ == '__main__':
    args = cmdParser()
    soc = args.soc
    to = args.timeout
    test = args.test
    run = AllTests(soc, to, get_all_tests(test))
    run.allTasks()