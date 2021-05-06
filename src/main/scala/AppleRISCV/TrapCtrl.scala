///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: trap ctrl
//
// Author: Heqing Huang
// Date Created: 04/17/2021
//
// ================== Description ==================
//
// Trap controller.
//
// Process all the exception/interrupt at WB stage
//
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._
import spinal.lib.MuxOH

import scala.collection.mutable.ArrayBuffer

case class trap_ctrl_io() extends Bundle {

  // exception signal
  val exc_ld_addr_ma    = in Bool
  val exc_sd_addr_ma    = in Bool
  val exc_ill_instr     = in Bool
  val exc_instr_addr_ma = in Bool

  // interrupt signal
  val external_interrupt  = in Bool
  val timer_interrupt     = in Bool
  val software_interrupt  = in Bool
  val debug_interrupt     = in Bool

  // system input
  val mret                = in Bool
  val ecall               = in Bool

  // info
  val stage_valid    = in Bool
  val cur_pc         = in UInt(AppleRISCVCfg.XLEN bits)
  val cur_instr      = in Bits(AppleRISCVCfg.XLEN bits)
  val cur_dmem_addr  = in UInt(AppleRISCVCfg.XLEN bits)

  // mcsr input
  val mie_meie    = in Bool
  val mie_mtie    = in Bool
  val mie_msie    = in Bool
  val mstatus_mie = in Bool
  val mepc        = in Bits(AppleRISCVCfg.XLEN bits)
  val mtvec       = in  Bits(AppleRISCVCfg.XLEN bits)

  // mcsr output
  val mtrap_enter  = out Bool
  val mtrap_exit   = out Bool
  val mtrap_mepc   = out Bits(AppleRISCVCfg.XLEN bits)
  val mtrap_mcause = out Bits(AppleRISCVCfg.MXLEN bits)
  val mtrap_mtval  = out Bits(AppleRISCVCfg.MXLEN bits)

  // pc control
  val pc_trap      = out Bool
  val pc_value     = out UInt(AppleRISCVCfg.XLEN bits)

  // output to hdu for flushing
  val trap_flush   = out Bool
}

case class TrapCtrl() extends Component {

  noIoPrefix()

  val io = trap_ctrl_io()

  // == exception control == //
  val dmem_addr_exception = io.exc_ld_addr_ma | io.exc_sd_addr_ma
  val exception           = dmem_addr_exception | io.exc_ill_instr | io.exc_instr_addr_ma
  val dmem_addr_extended  = io.cur_dmem_addr.resize(AppleRISCVCfg.MXLEN)

  // == interrupt control == //
  val external_interrupt_masked = io.external_interrupt & io.mstatus_mie & io.mie_meie
  val timer_interrupt_masked    = io.timer_interrupt & io.mstatus_mie & io.mie_mtie
  val software_interrupt_masked = io.software_interrupt & io.mstatus_mie & io.mie_msie
  val debug_interrupt_masked    = io.debug_interrupt & io.mstatus_mie
  val interrupt = io.stage_valid & (
                  external_interrupt_masked | timer_interrupt_masked |
                  software_interrupt_masked | debug_interrupt_masked)
  val pc_plus_4 = io.cur_pc + 4

  // == mcause exception code == //
  // interrupt
  val interrupt_code_sel_in   = io.external_interrupt ## io.timer_interrupt ## io.software_interrupt
  val interrupt_code_sel_data = Array(
    B(ExcCode.EXC_CODE_M_SW_INT, AppleRISCVCfg.MXLEN-1 bits),   // This is the first entry
    B(ExcCode.EXC_CODE_M_TIMER_INT, AppleRISCVCfg.MXLEN-1 bits),
    B(ExcCode.EXC_CODE_M_EXT_INT, AppleRISCVCfg.MXLEN-1 bits))
  val interrupt_code = MuxOH(interrupt_code_sel_in, interrupt_code_sel_data)
  // exception
  val exceptions_code_sel_in = io.exc_ld_addr_ma ## io.exc_sd_addr_ma ## io.exc_ill_instr ## io.exc_instr_addr_ma ## io.ecall
  val exceptions_code_sel_data = Array(
    B(ExcCode.EXC_CODE_MECALL, AppleRISCVCfg.MXLEN-1 bits),       // This is the first entry
    B(ExcCode.EXC_CODE_INSTR_ADDR_MA, AppleRISCVCfg.MXLEN-1 bits),
    B(ExcCode.EXC_CODE_ILL_INSTR, AppleRISCVCfg.MXLEN-1 bits),
    B(ExcCode.EXC_CODE_SD_ADDR_MA, AppleRISCVCfg.MXLEN-1 bits),
    B(ExcCode.EXC_CODE_LD_ADDR_MA, AppleRISCVCfg.MXLEN-1 bits))
  val exception_code = MuxOH(exceptions_code_sel_in, exceptions_code_sel_data)
  val trap_code = Mux(interrupt, interrupt_code, exception_code)

  // mcsr
  io.mtrap_enter  := exception | interrupt | io.ecall
  io.mtrap_exit   := io.mret
  io.mtrap_mepc   := io.cur_pc.asBits
  io.mtrap_mcause := interrupt ## trap_code
  io.mtrap_mtval  := Mux(io.exc_ill_instr, io.cur_instr, dmem_addr_extended.asBits)

  // update pc
  io.pc_trap      := io.mtrap_enter | io.mtrap_exit
  val mtvec_base  =  io.mtvec(AppleRISCVCfg.MXLEN-1 downto 2)
  io.pc_value     := Mux(io.mret, io.mepc.asUInt, mtvec_base.asUInt.resized)

  // request to flush
  io.trap_flush   := io.mtrap_enter | io.mtrap_exit
}
