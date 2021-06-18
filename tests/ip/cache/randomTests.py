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
from env import *

memDepth = 4096

def addrRandom():
    """ Constraint the address """
    return (random.randint(0, ((memDepth)<<2)-1) >> 2) << 2

def addrSameSet():
    """ Generate address with the same set """
    setIdx = 0
    setBitStart = 3
    setBitEnd = 9
    addrMax = memDepth >> setBitEnd - 1
    addr = random.randint(0, addrMax)
    addr = (addr << setBitEnd) + (setIdx << (setBitStart-1))
    return addr

@cocotb.coroutine
def cacheRandomRead(dut, iterNum, readOnly, addrGen, seed):
    """ Cache Ramdom Read/Write test
    """
    cacheAhbGen = setup(dut)
    random.seed(seed)
    print("Seed is " + str(seed))
    yield reset(dut)
    for i in range(iterNum):
        addr = addrGen()
        data = random.randint(0, 1000)
        if readOnly or random.randint(0, 1):
            yield cacheAhbGen.read(addr)
        else:
            yield cacheAhbGen.write(addr, data)

randomReadTf = cocotb.regression.TestFactory(cacheRandomRead)
randomReadTf.add_option("iterNum",  [100])
randomReadTf.add_option("readOnly", [0])
randomReadTf.add_option("addrGen",  [addrSameSet])
randomReadTf.add_option("seed",     [90])
randomReadTf.generate_tests()
