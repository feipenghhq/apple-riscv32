##################################################################################################
##
## Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
##
## ~~~ Hardware in SpinalHDL ~~~
##
## Author: Heqing Huang
## Date Created: 06/16/2021
##
## ================== Description ==================
##
## Test the cache module
##
##################################################################################################

import cocotb
import random
import sys
from env import *

memDepth = 4096

def addrRandom():
    """ random address """
    return (random.randint(0, ((memDepth)<<2)-1) >> 2) << 2

def addrSameSet():
    """ address within the same set """
    setIdx = 0
    setBitStart = 3
    setBitEnd = 11
    addrMax = memDepth >> setBitEnd - 1
    addr = random.randint(0, addrMax)
    addr = (addr << setBitEnd) + (setIdx << (setBitStart-1))
    return addr

@cocotb.coroutine
def cacheRandomRead(dut, iterNum, addrGen, seed):
    """ Cache Ramdom Read/Write test
    """
    cacheAhbGen = setup(dut)
    random.seed(seed)
    yield reset(dut)
    for i in range(iterNum):
        addr = addrGen()
        data = random.randint(0, 1000)
        if random.randint(0, 1):
            yield cacheAhbGen.read(addr)
        else:
            yield cacheAhbGen.write(addr, data)

seeds = [random.randint(0, sys.maxsize-1) for x in range(10)]
randomAddrTF = cocotb.regression.TestFactory(cacheRandomRead)
randomAddrTF.add_option("iterNum",  [10000])
randomAddrTF.add_option("addrGen",  [addrRandom])
randomAddrTF.add_option("seed",     seeds)
#randomAddrTF.add_option("addrGen",  [addrRandom])
#randomAddrTF.add_option("seed",     [118544370685217500])  # To debug a seed
randomAddrTF.generate_tests(prefix="randomAddr")

seeds = [random.randint(0, sys.maxsize-1) for x in range(5)]
sameSetAddrTF = cocotb.regression.TestFactory(cacheRandomRead)
sameSetAddrTF.add_option("iterNum",  [2000])
sameSetAddrTF.add_option("addrGen",  [addrSameSet])
sameSetAddrTF.add_option("seed",     seeds)
sameSetAddrTF.generate_tests(prefix="sameSetAddr")
