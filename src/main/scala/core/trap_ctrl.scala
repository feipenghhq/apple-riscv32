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

package core

import spinal.core._

case class trap_ctrl_io(param: CPU_PARAM) extends Bundle {

  // exception signal
  val load_addr_misalign                = in Bool
  val store_addr_misalign               = in Bool
  val illegal_instr_exception           = in Bool
  val instr_addr_misalign_exception     = in Bool

  // interrupt signal
  val external_interrupt  = in Bool
  val timer_interrupt     = in Bool
  val software_interrupt  = in Bool
  val debug_interrupt     = in Bool

  // system input
  val mret                = in Bool
  val ecall               = in Bool

  // info
  val wb_pc         = in UInt(param.PC_WIDTH bits)
  val wb_instr      = in Bits(param.XLEN bits)
  val wb_dmem_addr  = in UInt(param.XLEN bits)

  // mcsr input
  val mie_meie    = in Bool
  val mie_mtie    = in Bool
  val mie_msie    = in Bool
  val mstatus_mie = in Bool
  val mepc        = in Bits(param.PC_WIDTH bits)
  val mtvec       = in  Bits(param.PC_WIDTH bits)

  // mcsr output
  val mtrap_enter  = out Bool
  val mtrap_exit   = out Bool
  val mtrap_mepc   = out Bits(param.PC_WIDTH bits)
  val mtrap_mcause = out Bits(param.MXLEN bits)
  val mtrap_mtval  = out Bits(param.MXLEN bits)

  // pc control
  val pc_trap     = out Bool
  val pc_value    = out UInt(param.PC_WIDTH bits)

  // output to hdu for flushing
  val int_flush   = out Bool
}

case class trap_ctrl(param: CPU_PARAM) extends Component {

  noIoPrefix()

  val io = trap_ctrl_io(param)

  // == exception control == //
  val dmem_addr_exception = io.load_addr_misalign | io.store_addr_misalign
  val exception           = dmem_addr_exception | io.illegal_instr_exception | io.instr_addr_misalign_exception
  val dmem_addr_extended  = io.wb_dmem_addr.resize(param.MXLEN)

  // == interrupt control == //
  val external_interrupt_masked = io.external_interrupt & io.mstatus_mie & io.mie_meie
  val timer_interrupt_masked    = io.timer_interrupt & io.mstatus_mie & io.mie_mtie
  val software_interrupt_masked = io.software_interrupt & io.mstatus_mie & io.mie_msie
  val debug_interrupt_masked    = io.debug_interrupt & io.mstatus_mie
  val interrupt = external_interrupt_masked | timer_interrupt_masked |
                  software_interrupt_masked | debug_interrupt_masked
  val pc_plus_4 = io.wb_pc + 4

  // == mcause exception code == //

  // interrupt
  val external_interrupt_mask  = Bits(param.MXLEN - 1 bits)
  val timer_interrupt_mask     = Bits(param.MXLEN - 1 bits)
  val software_interrupt_mask  = Bits(param.MXLEN - 1 bits)
  external_interrupt_mask.setAllTo(external_interrupt_masked)
  timer_interrupt_mask.setAllTo(timer_interrupt_masked)
  software_interrupt_mask.setAllTo(software_interrupt_masked)

  val interrupt_code = external_interrupt_mask & param.EXCEP_CODE_M_external_interrupt |
                       timer_interrupt_mask    & param.EXCEP_CODE_M_timer_interrupt    |
                       software_interrupt_mask & param.EXCEP_CODE_M_software_interrupt

  // exception
  val load_addr_misalign_mask   = Bits(param.MXLEN - 1 bits)
  val store_addr_misalign_mask  = Bits(param.MXLEN - 1 bits)
  val illegal_instr_mask        = Bits(param.MXLEN - 1 bits)
  val instr_addr_misalign_mask  = Bits(param.MXLEN - 1 bits)
  val ecall_mask                = Bits(param.MXLEN - 1 bits)
  load_addr_misalign_mask.setAllTo(io.load_addr_misalign)
  store_addr_misalign_mask.setAllTo(io.store_addr_misalign)
  illegal_instr_mask.setAllTo(io.instr_addr_misalign_exception)
  instr_addr_misalign_mask.setAllTo(io.instr_addr_misalign_exception)
  ecall_mask.setAllTo(io.ecall)

  val exceptions_code = load_addr_misalign_mask  & param.EXCEP_CODE_load_addr_misalign  |
                        store_addr_misalign_mask & param.EXCEP_CODE_store_addr_misalign |
                        illegal_instr_mask       & param.EXCEP_CODE_illegal_instr       |
                        instr_addr_misalign_mask & param.EXCEP_CODE_instr_addr_misalign |
                        ecall_mask               & param.EXCEP_CODE_mecall // only support Machine ecall

  val exception_code = interrupt_code | exceptions_code

  // == mcsr == //
  io.mtrap_enter  := exception | interrupt | io.ecall
  io.mtrap_exit   := io.mret
  io.mtrap_mepc   := Mux(exception | io.ecall, io.wb_pc.asBits, pc_plus_4.asBits)
  io.mtrap_mcause := interrupt ## exception_code
  io.mtrap_mtval  := Mux(io.illegal_instr_exception, io.wb_instr, dmem_addr_extended.asBits)

  io.pc_trap      := io.mtrap_enter | io.mtrap_exit
  val mtvec_base  =  io.mtvec(param.MXLEN-1 downto 2)
  io.pc_value     := Mux(io.mret, io.mepc.asUInt, mtvec_base.asUInt.resized)
  io.int_flush    := interrupt
}
