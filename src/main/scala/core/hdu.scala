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
// Hazard Detection Unit. Control pipeline hazard. Responsible for stalling or flush pipeline.
//
// Note:
//  1. Flushing should cancel the stalling request from other stage
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
  val wb_exception  = in Bool
  val wb_mret       = in Bool
  val wb_ecall      = in Bool
  val wb_ebreak     = in Bool
  val wb_interrupt  = in Bool
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
  val id_stall_on_load_dep = (id_rs1_depends_on_ex_rd | id_rs2_depends_on_ex_rd) & io.ex_dmem_rd

  // ======================================
  // csr dependency
  // ======================================
  // If any instruction has data dependence on csr instruction,
  // we need to stall the pipeline since we are accessing csr at the wb stage
  val id_rs1_depends_on_mem_rd = (io.id_rs1_idx === io.mem_rd_idx) & io.id_rs1_rd
  val id_rs2_depends_on_mem_rd = (io.id_rs2_idx === io.mem_rd_idx) & io.id_rs2_rd
  val id_rs1_depends_on_csr = (id_rs1_depends_on_ex_rd & io.ex_csr_rd) | (id_rs1_depends_on_mem_rd & io.mem_csr_rd)
  val id_rs2_depends_on_csr = (id_rs2_depends_on_ex_rd & io.ex_csr_rd) | (id_rs2_depends_on_mem_rd & io.mem_csr_rd)
  val id_stall_on_csr_dep = id_rs2_depends_on_csr | id_rs1_depends_on_csr

  // ======================================
  // Individual flush request
  // ======================================

  // Trap requested flush: this should flush IF/ID/EX/MEM stage
  val wb_trap_flush = io.wb_exception | io.wb_mret | io.wb_ecall | io.wb_ebreak
  
  // branch requested flush: this should flush IF/ID stage
  val ex_branch_flush = io.branch_taken

  // ======================================
  // Individual stalling request
  // ======================================

  // ID stage data dependency stall: this should stall IF/ID/EX stage
  val id_dep_stall_req  = id_stall_on_load_dep | id_stall_on_csr_dep

  // ======================================
  // Final Stall Logic
  // ======================================

  io.if_pipe_stall  := id_dep_stall_req & ~wb_trap_flush & ~ex_branch_flush
  io.id_pipe_stall  := id_dep_stall_req & ~wb_trap_flush & ~ex_branch_flush
  io.ex_pipe_stall  := False
  io.mem_pipe_stall := False
  io.wb_pipe_stall  := False

  // ======================================
  // Final Flushing logic
  // ======================================

  io.if_valid  := ~id_dep_stall_req & ~ex_branch_flush & ~wb_trap_flush
  io.id_valid  := ~id_dep_stall_req & ~ex_branch_flush & ~wb_trap_flush
  io.ex_valid  := ~wb_trap_flush
  io.mem_valid := ~wb_trap_flush
}

