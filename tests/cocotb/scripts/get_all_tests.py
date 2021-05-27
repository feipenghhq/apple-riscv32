

import os
import subprocess

subprocess_run = subprocess.Popen("git rev-parse --show-toplevel", shell=True, stdout=subprocess.PIPE)
subprocess_return = subprocess_run.stdout.read()
REPO_ROOT = subprocess_return.decode().rstrip()

def riscv_test():
    pass

def dedicated_tests():
    """ Get all the test for dedicated test """
    path = "tests/dedicated-tests/generated"
    names = ["dedicated-access_imem"]
    tests = {}
    for name in names:
        tests[name] = f"{path}"
    return tests

def riscv_tests():
    """ Get all the test for riscv test """
    path = "tests/riscv-tests/generated"
    abs_path = f"{REPO_ROOT}/{path}"
    files = os.listdir(abs_path)
    tests = {}
    for test in files:
        if 'verilog' in test:
            tests[test.replace(".verilog", "")] = path
    return tests


def riscv_arch_tests():
    """ Get all the test for riscv test """
    path = "tests/riscv-arch-test/riscv-arch-test/work/rv32i_m"
    tests = {}
    for arch in ['I', 'M', 'privilege']:
        path1 = path + f'/{arch}'
        abs_path = f"{REPO_ROOT}/{path1}"
        files = os.listdir(abs_path)
        for test in files:
            if 'verilog' in test:
                tests[test.replace(".verilog", "")] = path1
    return tests

def get_all_tests(name):
    if name == 'dedicated_tests':
        return dedicated_tests
    if name == 'riscv_tests':
        return riscv_tests
    if name == 'riscv_arch_tests':
        return riscv_arch_tests
    raise FileNotFoundError("Test does not exist: " + str(name))