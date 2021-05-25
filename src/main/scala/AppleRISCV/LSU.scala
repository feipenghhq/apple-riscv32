///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: LSU
//
// Author: Heqing Huang
// Date Created: 03/31/2021
// Revision 1.0: 05/23/2021
//
// ================== Description ==================
//
// Load Store Unit
//
// - Logic to handle byte and half word access to memory
// - Because the underlining memory device may not byte-addressable,
//   We always read/write the entire bus width data.
//
// Revision 1.0:
//  - Renamed to IFU and Changed to AHB bus
//  - FIXME: What if we are in data phase and there is a stall request?
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.ahblite._
import spinal.lib.bus.amba3.ahblite.AhbLite3._


case class LSU() extends Component {

  val io = new Bundle {
    val stage_enable = in Bool
    val write = in Bool
    val read = in Bool
    val addr = in UInt(AppleRISCVCfg.XLEN bits)
    val wdata = in Bits(AppleRISCVCfg.XLEN bits)
    val rdata = out Bits(AppleRISCVCfg.XLEN bits)
    val rw_byte = in Bool
    val rw_half = in Bool
    val rd_unsigned = in Bool
    val lsu_stall_req = out Bool

    val dbus_ahb = master(AhbLite3Master(AppleRISCVCfg.dbusAhbCfg))

    // Exception
    val exc_ld_addr_ma = out Bool
    val exc_sd_addr_ma = out Bool
    val exc_ld_acc_flt = out Bool
    val exc_sd_acc_flt = out Bool
  }

  val addr_phase = Bool
  val data_phase = Bool

  // ========================================
  // Processing input
  // ========================================

  // Check wen and ren
  val wen = io.write & ~io.exc_sd_addr_ma
  val ren = io.read & ~io.exc_ld_addr_ma

  // stage the input for data phase
  val rd_unsigned_s1  = RegNextWhen(io.rd_unsigned, io.read & addr_phase)
  val rw_byte_s1      = RegNextWhen(io.rw_byte, io.read & addr_phase)
  val rw_half_s1      = RegNextWhen(io.rw_half, io.read & addr_phase)
  val byte_addr_s1    = RegNextWhen(io.addr(1 downto 0), io.read & addr_phase)
  val ren_ff          = RegNextWhen(ren, ~io.lsu_stall_req)
  val wen_ff          = RegNextWhen(wen, ~io.lsu_stall_req)

  // Check address alignment
  val half_addr_misalign = io.rw_half & io.addr(0)
  val word_addr_misalign = ~(io.rw_half | io.rw_byte) & (io.addr(1 downto 0) =/= 0)
  io.exc_ld_addr_ma := io.read & (half_addr_misalign | word_addr_misalign)
  io.exc_sd_addr_ma := io.write & (half_addr_misalign | word_addr_misalign)

  // Check if data phase can complete at one cycle
  addr_phase := (wen | ren)
  data_phase := RegNextWhen(addr_phase, ~io.lsu_stall_req)
  io.lsu_stall_req  := ~io.dbus_ahb.HREADY & data_phase

  // Check if we have access fault
  val acc_flt = data_phase & io.dbus_ahb.HREADY & io.dbus_ahb.HRESP
  io.exc_ld_acc_flt := ren_ff & acc_flt
  io.exc_sd_acc_flt := wen_ff & acc_flt

  // ========================================
  // Write Data Processing
  // ========================================
  val byte = io.wdata(7 downto 0)
  val half = io.wdata(15 downto 0).resized
  // wdata should be provided at data phase, so we need to delay it for one cycle
  val wdata = Reg(io.wdata.clone())
  // also do not update wdata when lsu is waiting for HREADY signal becasue
  // if we are using forwarded data, it will be gone.
  when(~io.lsu_stall_req) {
    when(io.rw_byte) {
      wdata := byte ## byte ## byte ## byte
    }.elsewhen(io.rw_half) {
      wdata := half ## half
    }.otherwise{
      wdata := io.wdata
    }
  }


  // ========================================
  // Read Data Processing
  // ========================================
  val rdata_byte0 = io.dbus_ahb.HRDATA(7 downto 0)
  val rdata_byte1 = io.dbus_ahb.HRDATA(15 downto 8)
  val rdata_byte2 = io.dbus_ahb.HRDATA(23 downto 16)
  val rdata_byte3 = io.dbus_ahb.HRDATA(31 downto 24)
  val rdata_hw0   = io.dbus_ahb.HRDATA(15 downto 0)
  val rdata_hw1   = io.dbus_ahb.HRDATA(31 downto 16)

  val rdata_byte0_unsign_ext = rdata_byte0.resize(AppleRISCVCfg.XLEN)
  val rdata_byte1_unsign_ext = rdata_byte1.resize(AppleRISCVCfg.XLEN)
  val rdata_byte2_unsign_ext = rdata_byte2.resize(AppleRISCVCfg.XLEN)
  val rdata_byte3_unsign_ext = rdata_byte3.resize(AppleRISCVCfg.XLEN)
  val rdata_hw0_unsign_ext   = rdata_hw0.resize(AppleRISCVCfg.XLEN)
  val rdata_hw1_unsign_ext   = rdata_hw1.resize(AppleRISCVCfg.XLEN)
  val rdata_byte0_sign_ext   = rdata_byte0.asSInt.resize(AppleRISCVCfg.XLEN).asBits
  val rdata_byte1_sign_ext   = rdata_byte1.asSInt.resize(AppleRISCVCfg.XLEN).asBits
  val rdata_byte2_sign_ext   = rdata_byte2.asSInt.resize(AppleRISCVCfg.XLEN).asBits
  val rdata_byte3_sign_ext   = rdata_byte3.asSInt.resize(AppleRISCVCfg.XLEN).asBits
  val rdata_hw0_sign_ext     = rdata_hw0.asSInt.resize(AppleRISCVCfg.XLEN).asBits
  val rdata_hw1_sign_ext     = rdata_hw1.asSInt.resize(AppleRISCVCfg.XLEN).asBits

  io.rdata := io.dbus_ahb.HRDATA
  when(rw_byte_s1) {
    switch(byte_addr_s1) {
      is(0) {io.rdata := Mux(rd_unsigned_s1, rdata_byte0_unsign_ext, rdata_byte0_sign_ext)}
      is(1) {io.rdata := Mux(rd_unsigned_s1, rdata_byte1_unsign_ext, rdata_byte1_sign_ext)}
      is(2) {io.rdata := Mux(rd_unsigned_s1, rdata_byte2_unsign_ext, rdata_byte2_sign_ext)}
      is(3) {io.rdata := Mux(rd_unsigned_s1, rdata_byte3_unsign_ext, rdata_byte3_sign_ext)}
    }
  }.elsewhen(rw_half_s1) {
    switch(byte_addr_s1) {
      is(0) {io.rdata := Mux(rd_unsigned_s1, rdata_hw0_unsign_ext, rdata_hw0_sign_ext)}
      is(2) {io.rdata := Mux(rd_unsigned_s1, rdata_hw1_unsign_ext, rdata_hw1_sign_ext)}
    }
  }

  // ========================================
  // APH Bus Control
  // ========================================
  io.dbus_ahb.HADDR     := io.addr
  io.dbus_ahb.HBURST    := B"3'b000"    // Single burst
  io.dbus_ahb.HMASTLOCK := False        // Not locked
  io.dbus_ahb.HPROT(0)  := True         // Data fetch
  io.dbus_ahb.HPROT(1)  := True         // Privileged access (We only have machine mode right now)
  io.dbus_ahb.HPROT(2)  := True         // Buffer-able
  io.dbus_ahb.HPROT(3)  := True         // Cache-able
  io.dbus_ahb.HSIZE     := io.rw_byte  ? B"3'b000" | (io.rw_half ? B"3'b01" | B"3'b010")
  io.dbus_ahb.HTRANS    := (wen | ren) ? NONSEQ | IDLE
  io.dbus_ahb.HWDATA    := wdata
  io.dbus_ahb.HWRITE    := wen
}
