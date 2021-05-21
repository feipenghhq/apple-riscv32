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
# Revsion 1: 05/18/2021
#   Added command line parser
#
##################################################################

import sys
import argparse
import serial
from serial.tools.list_ports import comports

PORT_NAME = {
    "arty": "Digilent USB Device",
    "de2" : "USB-Serial Controller"
}


class UartDownload:

    def __init__(self, size, port, file, baudrate=115200):
        """
            @param size: instruction rom size in KB
            @param baudrate: uart baudrate
        """
        self.baudrate = baudrate
        self.port = port
        self.file = file
        self.size = size * 1024
        self.base = 0x20000000
        self.ram = []

    def readVerilogMem(self, base):
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

        f = open(self.file)
        for line in f.readlines():
            line = line.rstrip()
            if line.find('@') != -1: # this is address line
                addr = int(line[1:], 16) - base
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

    def createData(self):
        """ create data stream from verilog memory file """
        self.readVerilogMem(self.base)
        self.addStartStop()
        #self.printRam()

    def setupUart(self):
        """ Setup uart port """
        self.serPort = serial.Serial(self.port, self.baudrate)

    def writeRam(self):
        num = self.serPort.write(self.ram)
        print(f"write {num} of bytes")

    def printRam(self, size):
        for i in range(size):
            print(hex(self.ram[i]))

    def all(self):
        self.createData()
        self.setupUart()
        self.writeRam()

def cmdParser():
    parser = argparse.ArgumentParser(description='Upload Instruction ROM through Uart')
    parser.add_argument('-size', '-s', type=int, required=True, nargs='?', help='Size of the Instruction ROM in KByte')
    parser.add_argument('-file', '-f', type=str, required=True, nargs='?', help='The Instruction ROM file')
    parser.add_argument('-board', '-b',  type=str, required=True, nargs='?', help='The FPGA board')
    return parser.parse_args()

def getComport(board):
    all_port_info = comports()
    for p, des, _ in all_port_info:
        if PORT_NAME[board] in des:
            print("Found Com Ports: " + p)
            print(des)
            return p

if __name__ == "__main__":
    args = cmdParser()
    size = args.size
    file = args.file
    board = args.board
    port = getComport(board)
    uartDownload = UartDownload(size, port, file)
    uartDownload.all()