#!/usr/bin/python3
##################################################################################################
##
## Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
##
## ~~~ Hardware in SpinalHDL ~~~
##
## Author: Heqing Huang
## Date Created: 06/06/2021
##
## ================== Description ==================
##
## script to run software tests
##
##################################################################################################

import os
import sys
import argparse
import subprocess

#####################################
# Utility function
#####################################

subprocess_run = subprocess.Popen("git rev-parse --show-toplevel", shell=True, stdout=subprocess.PIPE)
subprocess_return = subprocess_run.stdout.read()
REPO_ROOT = subprocess_return.decode().rstrip()

SEARCH_PATH = [
    f"{REPO_ROOT}/sdk/demo",
]

def cmdParser():
    parser = argparse.ArgumentParser(description='Run all the tests')
    parser.add_argument('-soc', type=str, required=True, nargs='?', help='The FPGA board')
    parser.add_argument('-timeout', '-to', type=str, required=True, nargs='?', help='Timeout value')
    parser.add_argument('-test', '-t', type=str, required=True, nargs='?', help='The test you want to run')
    parser.add_argument('-dump', '-d', type=str, required=True, nargs='?', help='Dump the waveform?')
    return parser.parse_args()

def find_test(name):
    for start in SEARCH_PATH:
        for relpath, dirs, files in os.walk(start):
            test_name = f"{name}.verilog"
            if test_name in files:
                full_path = os.path.join(start, relpath, test_name)
                path = os.path.join(start, relpath)
                print(f"Find test: {full_path}")
                return path, name

#####################################
# Main Program
#####################################

if __name__ == '__main__':
    args = cmdParser()
    soc = args.soc
    to = args.timeout
    test = args.test
    dump = args.dump
    path, test = find_test(test)
    cmd = f"make TESTNAME={test} TESTPATH={path} TIMEOUT={to} DUMP={dump}"
    os.system(cmd)