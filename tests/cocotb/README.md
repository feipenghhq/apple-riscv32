# Cocotb Tests

This folder contains all the tests using cocotb framework

## Sanity check (sanity_check)

Sanity checks contains some hand-written RISC-V ASM to test different instructions. The ASM code is in the comment session in each python test file.

Each python test file has the translated machine code for the instruction ram and the expected register value. The run script takes the instruction ram content, load it into the instruction ram and run the code. Once the run complete, it will check the register value against the expected value to see if the test passes or fails

Command to run the tests (inside sanity_check folder):

```bash
# To run all the sanity tests:
make

# To run individual test, using the following command

make TESTCASE=<test_name>

# Usage:
# Note: <test_name>  is the tests function name defined in sanity_check.py
# For example:
# make TESTCASE=test_logic_arithmetic1
```

## Riscv test simple (riscv-test-simple)

This folder contains the framework to run the modified RISC-V instruction set tests from riscv-test repo.The ASM programs tests/riscv-test-simple folder are compiled to create instruction ram file.

The run script will create symbolic link pointing to the instruction ram file and load it into the instruction ram. Then, it will run the program. The program will write specific value to registers if the test succeeds or fails. The run script will check the register value to determine if the test passes or fails.

Command to run the tests (inside riscv-test-simple folder):

```bash
# Run single tests through cocotb makefile flow

make TESTNAME=??? RISCVARCH=??? RUNTIME=???

# Usage:
#   TESTNAME: test instruction name. e.g. simple, xor, xori
#   RISCVARCH: riscv isa. e.g. rv32ui
#   RUNTIME: run time in ns. e.g. 3000. Notes: for some tests, runtime needs to be large enough
#            for the test to complete.
# For example:
#   make clean_run
#   make TESTNAME=simple RISCVARCH=rv32ui RUNTIME=3000
#   make TESTNAME=xor RISCVARCH=rv32ui RUNTIME=10000

# Run all the tests and check results
# A python script is written to run all the tests.
./run_all.py
```

Result and waveform dump will be placed in `output/<arch>/<test>`

## Test debug

This folder contains standalone tests for the debug feature. To run the test, go to each subdirectory and run `make`
