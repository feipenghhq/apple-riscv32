///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: DmemCtrl
//
// Author: Heqing Huang
// Date Created: 03/31/2021
//
// ================== Description ==================
//
// Data Memory Controller
//
// - Logic to handle byte and half word access to memory
// - We always read/write the whole word from/to memory so the address to the memory is aligned
//   to word address (lower 2 bits is always 0)
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import AppleRISCVSoC.bus._
import spinal.core._
import spinal.lib.master

case class DmemCtrl() extends Component {

  val io = new Bundle {
    // CPU side
    val cpu2mc_wr              = in Bool
    val cpu2mc_rd              = in Bool
    val cpu2mc_addr            = in UInt(AppleRISCVCfg.XLEN bits)
    val cpu2mc_data            = in Bits(AppleRISCVCfg.XLEN bits)
    val mc2cpu_data            = out Bits(AppleRISCVCfg.XLEN bits)
    val cpu2mc_mem_LS_byte     = in Bool
    val cpu2mc_mem_LS_halfword = in Bool
    val cpu2mc_mem_LW_unsigned = in Bool
    val dmem_stall_req         = out Bool
    // Exception
    val exc_ld_addr_ma = out Bool
    val exc_sd_addr_ma = out Bool
  }
  noIoPrefix()

  val dmem_sib = master(Sib(AppleRISCVCfg.sibCfg))

  // == Store the information for read data process == //
  val mem_byte_addr   = io.cpu2mc_addr(1 downto 0)
  val LW_unsigned_s1  = RegNextWhen(io.cpu2mc_mem_LW_unsigned, io.cpu2mc_rd)
  val LS_byte_s1      = RegNextWhen(io.cpu2mc_mem_LS_byte, io.cpu2mc_rd)
  val LS_halfword_s1  = RegNextWhen(io.cpu2mc_mem_LS_halfword, io.cpu2mc_rd)
  val mem_byte_addr_s1 = RegNextWhen(mem_byte_addr, io.cpu2mc_rd)

  // == Extract the data field == //
  val cpu2mc_data_7_0  = io.cpu2mc_data(7 downto 0)
  val cpu2mc_data_15_0 = io.cpu2mc_data(15 downto 0)

  val mem2mc_data_byte0 = dmem_sib.rdata(7 downto 0)
  val mem2mc_data_byte1 = dmem_sib.rdata(15 downto 8)
  val mem2mc_data_byte2 = dmem_sib.rdata(23 downto 16)
  val mem2mc_data_byte3 = dmem_sib.rdata(31 downto 24)
  val mem2mc_data_hw0   = dmem_sib.rdata(15 downto 0)
  val mem2mc_data_hw1   = dmem_sib.rdata(31 downto 16)

  // == extend the data in advance == //
  val mem2mc_data_byte0_unsign_ext = mem2mc_data_byte0.resize(AppleRISCVCfg.XLEN)
  val mem2mc_data_byte1_unsign_ext = mem2mc_data_byte1.resize(AppleRISCVCfg.XLEN)
  val mem2mc_data_byte2_unsign_ext = mem2mc_data_byte2.resize(AppleRISCVCfg.XLEN)
  val mem2mc_data_byte3_unsign_ext = mem2mc_data_byte3.resize(AppleRISCVCfg.XLEN)
  val mem2mc_data_hw0_unsign_ext   = mem2mc_data_hw0.resize(AppleRISCVCfg.XLEN)
  val mem2mc_data_hw1_unsign_ext   = mem2mc_data_hw1.resize(AppleRISCVCfg.XLEN)

  val mem2mc_data_byte0_sign_ext = mem2mc_data_byte0.asSInt.resize(AppleRISCVCfg.XLEN).asBits
  val mem2mc_data_byte1_sign_ext = mem2mc_data_byte1.asSInt.resize(AppleRISCVCfg.XLEN).asBits
  val mem2mc_data_byte2_sign_ext = mem2mc_data_byte2.asSInt.resize(AppleRISCVCfg.XLEN).asBits
  val mem2mc_data_byte3_sign_ext = mem2mc_data_byte3.asSInt.resize(AppleRISCVCfg.XLEN).asBits
  val mem2mc_data_hw0_sign_ext   = mem2mc_data_hw0.asSInt.resize(AppleRISCVCfg.XLEN).asBits
  val mem2mc_data_hw1_sign_ext   = mem2mc_data_hw1.asSInt.resize(AppleRISCVCfg.XLEN).asBits

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
  dmem_sib.wdata := io.cpu2mc_data
  when(io.cpu2mc_mem_LS_byte) {
    dmem_sib.wdata := cpu2mc_data_7_0 ## cpu2mc_data_7_0 ## cpu2mc_data_7_0 ## cpu2mc_data_7_0
  }.elsewhen(io.cpu2mc_mem_LS_halfword) {

    dmem_sib.wdata := cpu2mc_data_15_0 ## cpu2mc_data_15_0
  }

  // Read Data
  io.mc2cpu_data := dmem_sib.rdata // Default value
  when(LS_byte_s1) {
    switch(mem_byte_addr_s1) {
      is(0) {io.mc2cpu_data := Mux(LW_unsigned_s1, mem2mc_data_byte0_unsign_ext, mem2mc_data_byte0_sign_ext)}
      is(1) {io.mc2cpu_data := Mux(LW_unsigned_s1, mem2mc_data_byte1_unsign_ext, mem2mc_data_byte1_sign_ext)}
      is(2) {io.mc2cpu_data := Mux(LW_unsigned_s1, mem2mc_data_byte2_unsign_ext, mem2mc_data_byte2_sign_ext)}
      is(3) {io.mc2cpu_data := Mux(LW_unsigned_s1, mem2mc_data_byte3_unsign_ext, mem2mc_data_byte3_sign_ext)}
    }
  }.elsewhen(LS_halfword_s1) {
    switch(mem_byte_addr_s1) {
      is(0) {io.mc2cpu_data := Mux(LW_unsigned_s1, mem2mc_data_hw0_unsign_ext, mem2mc_data_hw0_sign_ext)}
      is(2) {io.mc2cpu_data := Mux(LW_unsigned_s1, mem2mc_data_hw1_unsign_ext, mem2mc_data_hw1_sign_ext)}
    }
  }

  // == check address alignment == //
  val halfword_addr_misalign = io.cpu2mc_mem_LS_halfword & io.cpu2mc_addr(0)
  val word_address_misalign  = ~(io.cpu2mc_mem_LS_byte | io.cpu2mc_mem_LS_halfword) & (io.cpu2mc_addr(1 downto 0) =/= 0)
  io.exc_ld_addr_ma  := io.cpu2mc_rd & (halfword_addr_misalign | word_address_misalign)
  io.exc_sd_addr_ma := io.cpu2mc_wr & (halfword_addr_misalign | word_address_misalign)

  // == Control Signal == //
  val wen = io.cpu2mc_wr & ~io.exc_sd_addr_ma
  val ren = io.cpu2mc_rd & ~io.exc_ld_addr_ma
  dmem_sib.sel       := wen | ren
  dmem_sib.enable    := dmem_sib.sel
  dmem_sib.write     := wen
  // The address should align to work boundary,because we always read a word and process the byte here.
  dmem_sib.addr      := io.cpu2mc_addr(AppleRISCVCfg.XLEN -1 downto 2) @@ False @@ False

  // Decode logic for write byte enable
  val byte_mask     = B"0001" |<< io.cpu2mc_addr(1 downto 0)
  val halfword_mask = Mux(io.cpu2mc_addr(1), B"1100", B"0011")
  dmem_sib.mask     := Mux(io.cpu2mc_mem_LS_byte, byte_mask, Mux(io.cpu2mc_mem_LS_halfword, halfword_mask, B"1111"))

  val dmem_ready     = dmem_sib.ready
  val dmem_resp      = dmem_sib.resp     // This should be 1 ideally
  io.dmem_stall_req  := ~dmem_ready & dmem_sib.sel
}
