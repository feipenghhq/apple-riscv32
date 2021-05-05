#!/usr/bin/python3
###################################################################
#
# Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
#
# Author: Heqing Huang
# Date Created: 04/27/2021
#
# ================== Description ==================
#
# Python program to download instructions into instruction rom
#
##################################################################

import sys
import serial

class UartDownload:

    def __init__(self, size, baudrate=115200):
        """
            @param size: instruction rom size in KB
            @param baudrate: uart baudrate
        """
        self.baudrate = baudrate
        self.port = '/dev/ttyUSB1'
        self.serPort = None
        self.size = size * 1024
        self.ram = []

    def readVerilogMem(self, file):
        """
            Function to read verilog memory file and convert it into actual instruction stream

            @param size: number of word in the final ram
            @return: return the ram data sequentially in byte
        """

        def readFromLine(line, ram, addr):
            data = line.split(" ")
            for byte in data:
                ram[addr] = int(byte ,16)
                addr += 1
            return addr

        self.ram = [ 0 for _ in range(self.size)]
        addr = 0

        f = open(file)
        for line in f.readlines():
            line = line.rstrip()
            if line.find('@') != -1: # this is address line
                addr = int(line[1:], 16)
            else:
                addr = readFromLine(line, self.ram, addr)
                if (addr == self.size):
                    f.close()
        f.close()

    def addStartStop(self):
        """ Add start and stop signal to the bit stream """
        self.ram.insert(0, 0xFF)
        self.ram.insert(0, 0xFF)
        self.ram.insert(0, 0xFF)
        self.ram.insert(0, 0xFF)
        self.ram.append(0xFE)
        self.ram.append(0xFF)
        self.ram.append(0xFF)
        self.ram.append(0xFF)

    def printRam(self):
        for byte in self.ram:
            print(byte, end='')

    def createData(self, file):
        """ create data stream from verilog memory file """
        self.readVerilogMem(file)
        self.addStartStop()
        #self.printRam()

    def setupUart(self):
        """ Setup uart port """
        self.serPort = serial.Serial(self.port, self.baudrate)

    def writeRam(self):
        num = self.serPort.write(self.ram)
        print(f"write {num} of bytes")



if __name__ == "__main__":

    uartDownload = UartDownload(64)
    uartDownload.createData(sys.argv[1])
    uartDownload.setupUart()
    uartDownload.writeRam()