# Tests

- [Tests](#tests)
  - [Introduction](#introduction)
  - [Directory Structures](#directory-structures)
  - [How to run tests](#how-to-run-tests)

## Introduction

This directory contains tests suites for the cpu core and cpu soc

## Directory Structures

### Main structure

```text
├── cocotb
│   ├── riscv-test-simple
│   └── sanity_check
├── riscv-tests-simple
└── verilog
```

### cocotb

- riscv-test-simple: Using the modified riscv-test code to test each cpu instruction
- sanity_check: Some simple sanity checks written in riscv asm and translated using the [venus](https://www.kvakil.me/venus/). Check each python file for the asm code.

### riscv-tests-simple

The modified source code from the riscv-test repo. See [README.md](./riscv-tests-simple/README.md) in it for more details.

### verilog

Testbench related verilog files

## How to run tests

See README.md under cocotb folder
