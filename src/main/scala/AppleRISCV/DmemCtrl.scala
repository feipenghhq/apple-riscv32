///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: DmemCtrl
//
// Author: Heqing Huang
// Date Created: 05/01/2021
//
// ================== Description ==================
//
// Data Memory Control
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._
import spinal.lib._
import AppleRISCVSoC.Bus._

case class DmemCtrl() extends Component {

  val io = new Bundle {
    val dmemCtrl = slave(DmemCtrlStage())
    val cpu2mcData = in Bits(AppleRISCVCfg.xlen bits)
    val cpu2mcAddr = in UInt(AppleRISCVCfg.xlen bits)
    val mc2cpuData = out Bits(AppleRISCVCfg.xlen bits)
    val dmemSib = master(Sib(AppleRISCVCfg.ImemSibCfg))
    // Exception
    val excLoadAddrMisalign     = out Bool
    val excStoreAddrMisalign    = out Bool
  }
  noIoPrefix()

  // == Store the information for read data process == //
  val accByte     = io.dmemCtrl.types === DmemTypeEnum.BY
  val accHalfWord = io.dmemCtrl.types === DmemTypeEnum.HW
  val memByteAddr   = io.cpu2mcAddr(1 downto 0)
  val unsigned_s1  = RegNextWhen(io.dmemCtrl.unsigned, io.dmemCtrl.read)
  val accByte_s1      = RegNextWhen(accByte, io.dmemCtrl.read)
  val accHalfWord_s1  = RegNextWhen(accHalfWord, io.dmemCtrl.read)
  val memByteAddr_s1 = RegNextWhen(memByteAddr, io.dmemCtrl.read)

  // == Extract the data field == //
  val cpu2mcData_7_0  = io.cpu2mcData(7 downto 0)
  val cpu2mcData_15_0 = io.cpu2mcData(15 downto 0)

  val rdataByte0 = io.dmemSib.rdata(7 downto 0)
  val rdataByte1 = io.dmemSib.rdata(15 downto 8)
  val rdataByte2 = io.dmemSib.rdata(23 downto 16)
  val rdataByte3 = io.dmemSib.rdata(31 downto 24)
  val rdataHw0   = io.dmemSib.rdata(15 downto 0)
  val rdataHw1   = io.dmemSib.rdata(31 downto 16)

  // == extend the data in advance == //
  val rdataByte0UnsignExt = rdataByte0.resize(AppleRISCVCfg.xlen)
  val rdataByte1UnsignExt = rdataByte1.resize(AppleRISCVCfg.xlen)
  val rdataByte2UnsignExt = rdataByte2.resize(AppleRISCVCfg.xlen)
  val rdataByte3UnsignExt = rdataByte3.resize(AppleRISCVCfg.xlen)
  val rdataHw0UnsignExt   = rdataHw0.resize(AppleRISCVCfg.xlen)
  val rdataHw1UnsignExt   = rdataHw1.resize(AppleRISCVCfg.xlen)

  val rdataByte0SignExt = rdataByte0.asSInt.resize(AppleRISCVCfg.xlen).asBits
  val rdataByte1SignExt = rdataByte1.asSInt.resize(AppleRISCVCfg.xlen).asBits
  val rdataByte2SignExt = rdataByte2.asSInt.resize(AppleRISCVCfg.xlen).asBits
  val rdataByte3SignExt = rdataByte3.asSInt.resize(AppleRISCVCfg.xlen).asBits
  val rdataHw0SignExt   = rdataHw0.asSInt.resize(AppleRISCVCfg.xlen).asBits
  val rdataHw1SignExt   = rdataHw1.asSInt.resize(AppleRISCVCfg.xlen).asBits

  // == Process the write/read data == //
  // From RISC-V Spec:
  //  The LW instruction loads a 32-bit value from memory into rd. LH loads a 16-bit value from memory,
  //  then sign-extends to 32-bits before storing in rd. LHU loads a 16-bit value from memory but then
  //  zero extends to 32-bits before storing in rd. LB and LBU are defined analogously for 8-bit values.
  //  The SW, SH, and SB instructions store 32-bit, 16-bit, and 8-bit values from the low bits of register
  //  rs2 to memory.
  //
  // Write: Since we always access a word in the memory, we need to place the data from the
  //        low bits of rs2 to its corresponding position for byte/half-word access.
  // READ: Since we always access the entire word in the memory, the data returned from the memory
  //       is the entire word, we need to extract the corresponding byte from the word and extends it.

  // Write Data
  io.dmemSib.wdata := io.cpu2mcData
  when(accByte) {
    // For simplicity, just set all the byte to the same data
    io.dmemSib.wdata := cpu2mcData_7_0 ## cpu2mcData_7_0 ## cpu2mcData_7_0 ## cpu2mcData_7_0
  }.elsewhen(accHalfWord) {
    // For simplicity, just set all the byte to the same data
    io.dmemSib.wdata := cpu2mcData_15_0 ## cpu2mcData_15_0
  }

  // Read Data
  io.mc2cpuData := io.dmemSib.rdata // Default value
  when(accByte_s1) {
    switch(memByteAddr_s1) {
      is(0) {io.mc2cpuData := Mux(unsigned_s1, rdataByte0UnsignExt, rdataByte0SignExt)}
      is(1) {io.mc2cpuData := Mux(unsigned_s1, rdataByte1UnsignExt, rdataByte1SignExt)}
      is(2) {io.mc2cpuData := Mux(unsigned_s1, rdataByte2UnsignExt, rdataByte2SignExt)}
      is(3) {io.mc2cpuData := Mux(unsigned_s1, rdataByte3UnsignExt, rdataByte3SignExt)}
    }
  }.elsewhen(accHalfWord_s1) {
    switch(memByteAddr_s1) {
      is(0) {io.mc2cpuData := Mux(unsigned_s1, rdataHw0UnsignExt, rdataHw0SignExt)}
      is(2) {io.mc2cpuData := Mux(unsigned_s1, rdataHw1UnsignExt, rdataHw1SignExt)}
    }
  }

  // == check address alignment == //
  val halfword_addr_misalign = accHalfWord & io.cpu2mcAddr(0)
  val word_address_misalign  = ~(accByte | accHalfWord) & (io.cpu2mcAddr(1 downto 0) =/= 0)
  io.excLoadAddrMisalign  := io.dmemCtrl.read & (halfword_addr_misalign | word_address_misalign)
  io.excStoreAddrMisalign := io.dmemCtrl.write & (halfword_addr_misalign | word_address_misalign)

  // == Control Signal == //
  val wen = io.dmemCtrl.write & ~io.excStoreAddrMisalign
  val ren = io.dmemCtrl.read & ~io.excLoadAddrMisalign
  io.dmemSib.sel       := wen | ren
  io.dmemSib.enable    := io.dmemSib.sel
  io.dmemSib.addr      := io.cpu2mcAddr
  io.dmemSib.write     := wen

  // Decode logic for write byte enable
  val byte_mask     = B"0001" |<< io.cpu2mcAddr(1 downto 0)
  val halfword_mask = Mux(io.cpu2mcAddr(1), B"1100", B"0011")
  io.dmemSib.mask     := Mux(accByte, byte_mask, Mux(accHalfWord, halfword_mask, B"1111"))

  //val dmem_ready     = io.dmemSib.ready    // This should be 1
  //val dmem_resp      = io.dmemSib.resp     // This should be 1
  //val dmem_data_vld  = dmem_ready & dmem_resp
}
