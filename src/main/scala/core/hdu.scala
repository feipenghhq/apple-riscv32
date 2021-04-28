///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: hazard detection unit
//
// Author: Heqing Huang
// Date Created: 04/08/2021
//
// ================== Description ==================
//
// Hazard Detection Unit
//
// Pipeline Hazard control. Responsible for stalling or flushing pipeline.
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package core

import spinal.core._

case class hdu_io(param: CPU_PARAM) extends Bundle {
  // Control output
  val if_valid  = out Bool
  val id_valid  = out Bool
  val ex_valid  = out Bool
  val mem_valid = out Bool
  val wb_valid  = out Bool

  val if_pipe_stall  = out Bool
  val id_pipe_stall  = out Bool
  val ex_pipe_stall  = out Bool
  val mem_pipe_stall = out Bool
  val wb_pipe_stall  = out Bool

  // input signal
  val branch_taken = in Bool
  val id_rs1_rd  = in Bool          
  val id_rs2_rd  = in Bool
  val ex_dmem_rd = in Bool
  val id_rs1_idx = in UInt(param.RF_ADDR_WDITH bits)
  val id_rs2_idx = in UInt(param.RF_ADDR_WDITH bits)
  val ex_rd_idx  = in UInt(param.RF_ADDR_WDITH bits)
  val mem_rd_idx = in UInt(param.RF_ADDR_WDITH bits)
  val ex_csr_rd  = in Bool
  val mem_csr_rd = in Bool

  // trap related signal
  val id_exception  = in Bool
  val ex_exception  = in Bool
  val mem_exception = in Bool
  val wb_exception  = in Bool
  val ex_mret       = in Bool
  val mem_mret      = in Bool
  val wb_mret       = in Bool
  val ex_ecall      = in Bool
  val mem_ecall     = in Bool
  val wb_ecall      = in Bool
  val ex_ebreak     = in Bool
  val mem_ebreak    = in Bool
  val wb_ebreak     = in Bool
  val int_flush     = in Bool
}

case class hdu(param: CPU_PARAM) extends Component {

  noIoPrefix()

  val io = hdu_io(param)

  // ======================================
  // Load dependency
  // ======================================
  // If there is immediate read data dependence on Load instruction,
  // we need to stall the pipe for one cycle
  // ID   |  EX  |  MEM | WB
  // I1   |  LW  |  OR  | ADD
  // I1   |  NOP |  LW  | OR
  // I2   |  I1  |  NOP | LW
  val id_rs1_depends_on_ex_rd = (io.id_rs1_idx === io.ex_rd_idx) & io.id_rs1_rd
  val id_rs2_depends_on_ex_rd = (io.id_rs2_idx === io.ex_rd_idx) & io.id_rs2_rd
  val stall_on_load_dependence = (id_rs1_depends_on_ex_rd | id_rs2_depends_on_ex_rd) & io.ex_dmem_rd

  // ======================================
  // csr dependency
  // ======================================
  // If any instruction has data dependence on csr instruction,
  // we need to stall the pipeline since we are accessing csr at the wb stage
  val id_rs1_depends_on_mem_rd = (io.id_rs1_idx === io.mem_rd_idx) & io.id_rs1_rd
  val id_rs2_depends_on_mem_rd = (io.id_rs2_idx === io.mem_rd_idx) & io.id_rs2_rd
  val id_rs1_depends_on_csr = (id_rs1_depends_on_ex_rd & io.ex_csr_rd) | (id_rs1_depends_on_mem_rd & io.mem_csr_rd)
  val id_rs2_depends_on_csr = (id_rs2_depends_on_ex_rd & io.ex_csr_rd) | (id_rs2_depends_on_mem_rd & io.mem_csr_rd)
  val stall_on_csr_dependence = id_rs2_depends_on_csr | id_rs1_depends_on_csr

  // ======================================
  // Trap
  // ======================================

  val exception_flush_wb = io.wb_exception
  val exception_flush_mem = exception_flush_wb
  val exception_flush_ex = io.mem_exception | exception_flush_mem
  val exception_flush_id = io.ex_exception  | exception_flush_ex
  val exception_flush_if = io.id_exception  | exception_flush_id

  val sys_flush_mem = io.wb_mret   | io.wb_ebreak  | io.wb_ecall
  val sys_flush_ex  = (io.mem_mret | io.mem_ebreak | io.mem_ecall) | sys_flush_mem
  val sys_flush_id  = (io.ex_mret  | io.ex_ebreak  | io.ex_ecall)  | sys_flush_ex
  val sys_flush_if  = sys_flush_id

  val trap_flush_if  = sys_flush_if  | exception_flush_if | io.int_flush
  val trap_flush_id  = sys_flush_id  | exception_flush_id | io.int_flush
  val trap_flush_ex  = sys_flush_ex  | exception_flush_ex | io.int_flush
  val trap_flush_mem = sys_flush_mem | exception_flush_mem | io.int_flush
  val trap_flush_wb  = exception_flush_wb


  // ======================================
  // Overall Stall Logic
  // ======================================
  io.if_pipe_stall  := stall_on_load_dependence | stall_on_csr_dependence
  io.id_pipe_stall  := stall_on_load_dependence | stall_on_csr_dependence
  io.ex_pipe_stall  := False
  io.mem_pipe_stall := False
  io.wb_pipe_stall  := False

  // ======================================
  // Overall Flushing logic
  // ======================================
  io.if_valid   := ~io.branch_taken & ~trap_flush_if
  io.id_valid   := ~io.branch_taken & ~stall_on_load_dependence & ~stall_on_csr_dependence & ~trap_flush_id
  io.ex_valid   := ~trap_flush_ex
  io.mem_valid  := ~trap_flush_mem
  io.wb_valid   := ~trap_flush_wb
}

