///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: mcsr
//
// Author: Heqing Huang
// Date Created: 04/17/2021
//
// ================== Description ==================
//
// Machine Level CSR module.
//
// For simplicity, for RW register, we always assign MXLEN bit flip flop even some of the bits
// are reserved or unimplemented.
//
// Some notes about interrupt.
// 1. mstatus:mpp is always set to 2'b11 since we only support machine mode
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._

case class MCSRIO() extends Bundle {
  // mcsr sw access
  val mcsr_addr = in Bits(AppleRISCVCfg.CSR_ADDR_WIDTH bits)
  val mcsr_din  = in Bits(AppleRISCVCfg.MXLEN bits)
  val mcsr_wen  = in Bool
  val mcsr_dout = out Bits(AppleRISCVCfg.MXLEN bits)

  // trap related input
  val mtrap_enter  = in Bool
  val mtrap_exit   = in Bool
  val mtrap_mepc   = in Bits(AppleRISCVCfg.XLEN bits)
  val mtrap_mcause = in Bits(AppleRISCVCfg.MXLEN bits)
  val mtrap_mtval  = in Bits(AppleRISCVCfg.MXLEN bits)
  val external_interrupt  = in Bool
  val timer_interrupt     = in Bool
  val software_interrupt  = in Bool


  // trap related output
  val mtvec        = out Bits(AppleRISCVCfg.XLEN bits)
  val mie_meie     = out Bool
  val mie_mtie     = out Bool
  val mie_msie     = out Bool
  val mstatus_mie  = out Bool
  val mepc         = out Bits(AppleRISCVCfg.MXLEN bits)

  // other
  val hartId       = in Bits(AppleRISCVCfg.MXLEN bits)
}

case class MCSR() extends Component {

  val io = MCSRIO()
  noIoPrefix()

  // ============================================
  // Defining Register
  // ============================================

  // Machine Information Registers
  val mvendorid = B(0, AppleRISCVCfg.MXLEN bits) // RO
  val marchid   = B(0, AppleRISCVCfg.MXLEN bits) // RO
  val mimpid    = B(0, AppleRISCVCfg.MXLEN bits) // RO
  val mhartid   = io.hartId                      // RO

  // Machine Trap Setup
  val mstatus   = Reg(Bits(AppleRISCVCfg.MXLEN bits)) init 0  // RW
  val misa      = B(0, AppleRISCVCfg.MXLEN bits)  // RW
  val mie       = Reg(Bits(AppleRISCVCfg.MXLEN bits)) init 0  // RW
  val mtvec     = Reg(Bits(AppleRISCVCfg.MXLEN bits)) init 0  // RW

  // Machine Trap Handling
  val mscratch  = Reg(Bits(AppleRISCVCfg.MXLEN bits)) init 0  // RW
  val mepc      = Reg(Bits(AppleRISCVCfg.MXLEN bits)) init 0  // RW
  val mcause    = Reg(Bits(AppleRISCVCfg.MXLEN bits)) init 0  // RW
  val mtval     = Reg(Bits(AppleRISCVCfg.MXLEN bits)) init 0  // RW
  val mip       = Reg(Bits(AppleRISCVCfg.MXLEN bits)) init 0  // RW

  // ============================================
  // SW access
  // ============================================

  // Read Logic
  switch (io.mcsr_addr) {
    is(B"hF11") {io.mcsr_dout := mvendorid}
    is(B"hF12") {io.mcsr_dout := marchid}
    is(B"hF13") {io.mcsr_dout := mimpid}
    is(B"hF14") {io.mcsr_dout := mhartid}

    is(B"h300") {io.mcsr_dout := mstatus}
    is(B"h301") {io.mcsr_dout := misa}
    is(B"h304") {io.mcsr_dout := mie}
    is(B"h305") {io.mcsr_dout := mtvec}

    is(B"h340") {io.mcsr_dout := mscratch}
    is(B"h341") {io.mcsr_dout := mepc}
    is(B"h342") {io.mcsr_dout := mcause}
    is(B"h343") {io.mcsr_dout := mtval}
    is(B"h344") {io.mcsr_dout := mip}

    default {io.mcsr_dout := mvendorid}
  }

  // Write Logic
  // Write logic only does write enable decode, the actual write is in HW access section
  val mstatus_wen = (io.mcsr_addr === B"h300") & io.mcsr_wen
  val mie_wen     = (io.mcsr_addr === B"h304") & io.mcsr_wen
  val mtvec_wen   = (io.mcsr_addr === B"h305") & io.mcsr_wen

  val mscratch_wen = (io.mcsr_addr === B"h340") & io.mcsr_wen
  val mepc_wen    = (io.mcsr_addr === B"h341") & io.mcsr_wen
  val mcause_wen  = (io.mcsr_addr === B"h342") & io.mcsr_wen
  val mtval_wen   = (io.mcsr_addr === B"h343") & io.mcsr_wen

  // ============================================
  // HW access
  // ============================================

  // == Machine Trap Setup == //

  // mstatus register
  def mstatus_mie:  Bool = mstatus(3)
  def mstatus_mpie: Bool = mstatus(7)
  def mstatus_mpp:  Bits = mstatus(12 downto 11)
  mstatus_mpp := B"2'b11" // Since we only support Machine mode, we always set it to 11
  when(io.mtrap_enter) {
    mstatus_mie   := False
    mstatus_mpie  := mstatus_mie
  }.elsewhen(io.mtrap_exit) {
    mstatus_mie   := mstatus_mpie
    mstatus_mpie  := True
  }.elsewhen(mstatus_wen) {
    mstatus       := io.mcsr_din
  }

  // misa register
  misa(AppleRISCVCfg.MXLEN-1 downto AppleRISCVCfg.MXLEN-2) := 1

  // mie register
  def mie_meie: Bool = mie(11)
  def mie_mtie: Bool = mie(7)
  def mie_msie: Bool = mie(3)
  when(mie_wen) {
    mie := io.mcsr_din
  }

  // mtvec register
  def mtvec_base: Bits = mtvec(AppleRISCVCfg.MXLEN-1 downto 2)
  def mtvec_mode: Bits = mtvec(1 downto 0)
  when(mtvec_wen) {
    mtvec := io.mcsr_din
  }

  // == Machine Trap Handling == //

  // mscratch
  when(mscratch_wen) {
    mscratch := io.mcsr_din
  }

  // mepc
  def mepc_base: Bits = mepc(AppleRISCVCfg.MXLEN-1 downto 2)
  def mepc_mode: Bits = mepc(1 downto 0)
  when(io.mtrap_enter) {
    mepc  := io.mtrap_mepc
  }.elsewhen(mepc_wen) {
    mepc  := io.mcsr_din
  }

  // mcause
  def mcause_ec:        Bits = mcause(AppleRISCVCfg.MXLEN-2 downto 0)
  def mcause_interrupt: Bool = mcause(AppleRISCVCfg.MXLEN-1)
  when(io.mtrap_enter) {
    mcause  := io.mtrap_mcause
  }elsewhen(mcause_wen) {
    mcause  := io.mcsr_din
  }

  // mtval
  when(io.mtrap_enter) {
    mtval  := io.mtrap_mtval
  }.elsewhen(mtval_wen) {
    mtval  := io.mcsr_din
  }

  // mip register
  def mip_meip: Bool = mip(11)
  def mip_mtip: Bool = mip(7)
  def mip_msip: Bool = mip(3)
  mip_meip := io.external_interrupt &  mie_meie
  mip_mtip := io.timer_interrupt    &  mie_mtie
  mip_msip := io.software_interrupt &  mie_msie

  // ============================================
  // Trap related
  // ============================================
  io.mtvec       := mtvec
  io.mie_meie    := mie_meie
  io.mie_mtie    := mie_mtie
  io.mie_msie    := mie_msie
  io.mstatus_mie := mstatus_mie
  io.mepc        := mepc
}
