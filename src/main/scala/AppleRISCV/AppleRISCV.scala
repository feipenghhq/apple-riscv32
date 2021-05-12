///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: apple_riscv
//
// Author: Heqing Huang
// Date Created: 03/29/2021
//
// ================== Description ==================
//
// The top level of the cpu core.
//
// - The top level instantiate all the cpu core components.
// - The pipeline logic is written in this file directly to reduce I/O connection.
// - Instruction RAM and Data RAM are out side the cpu core
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import AppleRISCVSoC.bus._
import spinal.core._
import spinal.lib.master

case class AppleRISCVIO() extends Bundle {
    val imem_sib            = master(Sib(AppleRISCVCfg.sibCfg))
    val dmem_sib            = master(Sib(AppleRISCVCfg.sibCfg))
    val external_interrupt  = in Bool
    val timer_interrupt     = in Bool
    val software_interrupt  = in Bool
    val debug_interrupt     = in Bool
}

case class AppleRISCV() extends Component {

    val io = AppleRISCVIO()
    noIoPrefix()

    ///////////////////////////////////////
    //     Instantiate all the modules   //
    ///////////////////////////////////////
    // IF Stage
    val pc_inst = PC()
    val bpu_inst = if(AppleRISCVCfg.USE_BPU) BPU() else null
    val imem_ctrl_inst = ImemCtrl()

    // ID Stage
    val instr_dec_inst = InstrDec()
    val regfile_inst   = RegFile()

    // EX Stage
    val alu_inst = ALU()
    val mul_inst = if (AppleRISCVCfg.USE_RV32M) AppleRISCVCMultiplier() else null
    val div_inst = if (AppleRISCVCfg.USE_RV32M) AppleRISCVDivider() else null
    val branch_unit_inst = BU()

    // MEM Stage
    val dmem_ctrl_isnt = DmemCtrl()

    // WB Stage
    val mcsr_inst       = MCSR()  // mcsr with hart 0
    val trap_ctrl_inst  = TrapCtrl()

    /////////////////////////////////////////////////////////////
    //     Pipeline Stage and Wire Connection For each Stage   //
    /////////////////////////////////////////////////////////////

    // =========================
    // Wires
    // =========================

    // Place holder for flush and bubble insertion
    // The stage prefix indicate if the instruction in the current stage is valid
    val if_stage_valid  = Bool
    val id_stage_valid  = Bool
    val id_stage_valid_final  = Bool
    val ex_stage_valid  = Bool
    val ex_stage_valid_final  = Bool
    val mem_stage_valid = Bool
    val wb_stage_valid  = Bool

    val if_pipe_stall  = Bool
    val id_pipe_stall  = Bool
    val ex_pipe_stall  = Bool
    val mem_pipe_stall = Bool

    // =========================
    // IF Stage
    // =========================

    // PC
    pc_inst.io.branch := branch_unit_inst.io.take_branch
    pc_inst.io.stall  := if_pipe_stall
    pc_inst.io.branch_pc_in  := branch_unit_inst.io.target_pc

    if (AppleRISCVCfg.USE_BPU) {
        pc_inst.io.bpu_pred_take := bpu_inst.io.pred_take
        pc_inst.io.bpu_pc_in     := bpu_inst.io.pred_pc
    } else {
        pc_inst.io.bpu_pred_take := False
        pc_inst.io.bpu_pc_in     := 0
    }

    // BPU
    val branch_instr_pc = if (AppleRISCVCfg.USE_BPU) UInt(AppleRISCVCfg.XLEN bits) else null // place holder
    if (AppleRISCVCfg.USE_BPU) {
        bpu_inst.io.pc            := pc_inst.io.pc_out
        bpu_inst.io.branch_update := branch_unit_inst.io.is_branch
        bpu_inst.io.branch_should_take := branch_unit_inst.io.branch_should_take
        bpu_inst.io.branch_target_pc := branch_unit_inst.io.target_pc
        bpu_inst.io.branch_instr_pc := branch_instr_pc
        bpu_inst.io.if_stage_valid := if_stage_valid
    }

    // ImemCtrl
    io.imem_sib <> imem_ctrl_inst.io.imem_sib
    imem_ctrl_inst.io.cpu2mc_addr   := pc_inst.io.pc_out
    imem_ctrl_inst.io.cpu2mc_en     := ~if_pipe_stall


    // IF/ID Pipeline
    val if2id = new Area {
        val pc          = RegNextWhen(pc_inst.io.pc_out,        ~if_pipe_stall) init 0
        val stage_valid = RegNextWhen(if_stage_valid,           ~if_pipe_stall) init False

        // Optional BPU
        val pred_take   = if (AppleRISCVCfg.USE_BPU) RegNextWhen(bpu_inst.io.pred_take, ~if_pipe_stall) else null
        val pred_pc     = if (AppleRISCVCfg.USE_BPU) RegNextWhen(bpu_inst.io.pred_pc, ~if_pipe_stall) else null
    }

    // =========================
    // ID stage
    // =========================

    // InstrDec
    instr_dec_inst.io.instr     := imem_ctrl_inst.io.mc2cpu_data
    instr_dec_inst.io.instr_vld := if2id.stage_valid

    // register file
    regfile_inst.io.rs1_rd_addr := instr_dec_inst.io.rs1_idx
    regfile_inst.io.rs2_rd_addr := instr_dec_inst.io.rs2_idx

    // Place holder for the forwarding detection
    val id_rs1_dep_ex_rd   = Bool
    val id_rs1_dep_mem_rd  = Bool
    val id_rs2_dep_ex_rd   = Bool
    val id_rs2_dep_mem_rd  = Bool

    val idRs1Mux = new Area {
        val id_rs1_final = regfile_inst.io.rs1_data_out.clone()
        when(instr_dec_inst.io.op1_sel_pc) {
            id_rs1_final := if2id.pc.asBits
        }.elsewhen(instr_dec_inst.io.op1_sel_zero) {
            id_rs1_final := 0
        }.otherwise{
            id_rs1_final := regfile_inst.io.rs1_data_out
        }
    }

    // =========================
    // ID/EX Pipeline
    // =========================

    val id2ex = new Area {
        // control signal
        val stage_valid   = RegNextWhen(id_stage_valid_final, ~id_pipe_stall) init False
        val rd_wr         = RegNextWhen(instr_dec_inst.io.rd_wr       & id_stage_valid_final, ~id_pipe_stall) init False
        val dmem_wr       = RegNextWhen(instr_dec_inst.io.dmem_wr     & id_stage_valid_final, ~id_pipe_stall) init False
        val dmem_rd       = RegNextWhen(instr_dec_inst.io.dmem_rd     & id_stage_valid_final, ~id_pipe_stall) init False
        val branch_op     = RegNextWhen(instr_dec_inst.io.branch_op   & id_stage_valid_final, ~id_pipe_stall) init False
        val jal_op        = RegNextWhen(instr_dec_inst.io.jal_op      & id_stage_valid_final, ~id_pipe_stall) init False
        val jalr_op       = RegNextWhen(instr_dec_inst.io.jalr_op     & id_stage_valid_final, ~id_pipe_stall) init False
        val mret          = RegNextWhen(instr_dec_inst.io.mret        & id_stage_valid_final, ~id_pipe_stall) init False
        val ecall         = RegNextWhen(instr_dec_inst.io.ecall       & id_stage_valid_final, ~id_pipe_stall) init False
        val ebreak        = RegNextWhen(instr_dec_inst.io.ebreak      & id_stage_valid_final, ~id_pipe_stall) init False
        val csr_wr        = RegNextWhen(instr_dec_inst.io.csr_wr      & id_stage_valid_final, ~id_pipe_stall) init False
        val csr_rd        = RegNextWhen(instr_dec_inst.io.csr_rd      & id_stage_valid_final, ~id_pipe_stall) init False
        // Payload
        val rd_idx         = RegNextWhen(instr_dec_inst.io.rd_idx ,         ~id_pipe_stall)
        val rs1_idx        = RegNextWhen(instr_dec_inst.io.rs1_idx,         ~id_pipe_stall)
        val csr_sel_imm    = RegNextWhen(instr_dec_inst.io.csr_sel_imm,     ~id_pipe_stall)
        val csr_idx        = RegNextWhen(instr_dec_inst.io.csr_idx,         ~id_pipe_stall)
        val rd_sel         = RegNextWhen(instr_dec_inst.io.rd_sel,          ~id_pipe_stall)
        val csr_sel        = RegNextWhen(instr_dec_inst.io.csr_sel,         ~id_pipe_stall)
        val alu_opcode     = RegNextWhen(instr_dec_inst.io.alu_opcode,      ~id_pipe_stall)
        val bu_opcode      = RegNextWhen(instr_dec_inst.io.bu_opcode,       ~id_pipe_stall)
        val dmem_ld_byte   = RegNextWhen(instr_dec_inst.io.dmem_ld_byte,    ~id_pipe_stall)
        val dmem_ld_hword  = RegNextWhen(instr_dec_inst.io.dmem_ld_hword,   ~id_pipe_stall)
        val dmem_ld_unsign = RegNextWhen(instr_dec_inst.io.dmem_ld_unsign,  ~id_pipe_stall)
        val rs1_value      = RegNextWhen(idRs1Mux.id_rs1_final ,            ~id_pipe_stall)
        val rs2_value      = RegNextWhen(regfile_inst.io.rs2_data_out,      ~id_pipe_stall)
        val imm_value      = RegNextWhen(instr_dec_inst.io.imm_value ,      ~id_pipe_stall)
        val rs1_dep_mem    = RegNextWhen(id_rs1_dep_ex_rd ,                 ~id_pipe_stall)
        val rs1_dep_wb     = RegNextWhen(id_rs1_dep_mem_rd,                 ~id_pipe_stall)
        val rs2_dep_mem    = RegNextWhen(id_rs2_dep_ex_rd ,                 ~id_pipe_stall)
        val rs2_dep_wb     = RegNextWhen(id_rs2_dep_mem_rd,                 ~id_pipe_stall)
        val pc             = RegNextWhen(if2id.pc,                          ~id_pipe_stall)
        val op2_sel_imm    = RegNextWhen(instr_dec_inst.io.op2_sel_imm ,    ~id_pipe_stall)
        val instr          = RegNextWhen(imem_ctrl_inst.io.mc2cpu_data ,    ~id_pipe_stall)
        // Exception, don't insert bubble to exception
        val exc_ill_instr = RegNextWhen(instr_dec_inst.io.exc_ill_instr & id_stage_valid, ~id_pipe_stall) init False

        // Optional mul/div
        val mul_op     = if (AppleRISCVCfg.USE_RV32M) RegNextWhen(instr_dec_inst.io.mul_op & id_stage_valid_final, ~id_pipe_stall) init False else null
        val div_op     = if (AppleRISCVCfg.USE_RV32M) RegNextWhen(instr_dec_inst.io.div_op & id_stage_valid_final, ~id_pipe_stall) init False else null
        val mul_opcode = if (AppleRISCVCfg.USE_RV32M) RegNextWhen(instr_dec_inst.io.mul_opcode, ~id_pipe_stall) else null
        val div_opcode = if (AppleRISCVCfg.USE_RV32M) RegNextWhen(instr_dec_inst.io.div_opcode, ~id_pipe_stall) else null

        // Optional BPU
        val pred_take  = if (AppleRISCVCfg.USE_BPU) RegNextWhen(if2id.pred_take, ~id_pipe_stall) else null
        val pred_pc    = if (AppleRISCVCfg.USE_BPU) RegNextWhen(if2id.pred_pc, ~id_pipe_stall) else null
    }

    // =========================
    // EX stage
    // =========================

    // Place holder for the final register value after the forwarding logic
    val ex_rs1_value_forwarded = Bits(AppleRISCVCfg.XLEN bits)
    val ex_rs2_value_forwarded = Bits(AppleRISCVCfg.XLEN bits)

    // Mux for the ALU operand
    val alu_operand2_muxout  = Mux(id2ex.op2_sel_imm, id2ex.imm_value.asBits, ex_rs2_value_forwarded)

    // ALU
    alu_inst.io.operand_1    := ex_rs1_value_forwarded
    alu_inst.io.operand_2    := alu_operand2_muxout
    alu_inst.io.pc           := id2ex.pc
    alu_inst.io.alu_opcode   := id2ex.alu_opcode

    if (AppleRISCVCfg.USE_RV32M) {
        // Multiplier
        mul_inst.io.stage_valid  := ex_stage_valid
        mul_inst.io.multiplicand := ex_rs1_value_forwarded
        mul_inst.io.multiplier   := ex_rs2_value_forwarded
        mul_inst.io.mul_req      := id2ex.mul_op
        mul_inst.io.mul_opcode   := id2ex.mul_opcode

        // Divider
        div_inst.io.stage_valid  := ex_stage_valid
        div_inst.io.dividend     := ex_rs1_value_forwarded
        div_inst.io.divisor      := ex_rs2_value_forwarded
        div_inst.io.div_req      := id2ex.div_op
        div_inst.io.div_opcode   := id2ex.div_opcode
    }


    // Select data between alu, mul, div
    val ex_rd_wdata = alu_inst.io.alu_out.clone()
    if (AppleRISCVCfg.USE_RV32M) {
        switch(id2ex.rd_sel){
            is(RdSelEnum.MUL) {ex_rd_wdata := mul_inst.io.result}
            is(RdSelEnum.DIV) {ex_rd_wdata := div_inst.io.result}
            default {ex_rd_wdata := alu_inst.io.alu_out}
        }
    } else {
        ex_rd_wdata := alu_inst.io.alu_out
    }

    // Branch Unit
    branch_unit_inst.io.current_pc      := id2ex.pc
    branch_unit_inst.io.imm_value       := id2ex.imm_value(20 downto 0)
    branch_unit_inst.io.rs1_value       := ex_rs1_value_forwarded
    branch_unit_inst.io.rs2_value       := ex_rs2_value_forwarded
    branch_unit_inst.io.bu_opcode       := id2ex.bu_opcode
    branch_unit_inst.io.br_op           := id2ex.branch_op
    branch_unit_inst.io.jal_op          := id2ex.jal_op
    branch_unit_inst.io.jalr_op         := id2ex.jalr_op
    branch_unit_inst.io.ex_stage_valid  := ex_stage_valid_final
    if (AppleRISCVCfg.USE_BPU) {
        branch_unit_inst.io.pred_take   := id2ex.pred_take
        branch_unit_inst.io.pred_pc     := id2ex.pred_pc
        branch_instr_pc                 := id2ex.pc
    }

    // Memory Controller Input
    dmem_ctrl_isnt.dmem_sib                     <> io.dmem_sib
    dmem_ctrl_isnt.io.cpu2mc_wr                 := id2ex.dmem_wr & ex_stage_valid_final
    dmem_ctrl_isnt.io.cpu2mc_rd                 := id2ex.dmem_rd & ex_stage_valid_final
    dmem_ctrl_isnt.io.cpu2mc_addr               := id2ex.imm_value.asUInt + ex_rs1_value_forwarded.asUInt
    dmem_ctrl_isnt.io.cpu2mc_data               := ex_rs2_value_forwarded
    dmem_ctrl_isnt.io.cpu2mc_mem_LS_byte        := id2ex.dmem_ld_byte
    dmem_ctrl_isnt.io.cpu2mc_mem_LS_halfword    := id2ex.dmem_ld_hword
    dmem_ctrl_isnt.io.cpu2mc_mem_LW_unsigned    := id2ex.dmem_ld_unsign

    // EX/Mem Pipeline
    val ex2mem = new Area {
        // control signal
        val stage_valid  = RegNextWhen(ex_stage_valid_final                , ~ex_pipe_stall) init False
        val rd_wr        = RegNextWhen(id2ex.rd_wr   & ex_stage_valid_final, ~ex_pipe_stall) init False
        val mret         = RegNextWhen(id2ex.mret    & ex_stage_valid_final, ~ex_pipe_stall) init False
        val ecall        = RegNextWhen(id2ex.ecall   & ex_stage_valid_final, ~ex_pipe_stall) init False
        val ebreak       = RegNextWhen(id2ex.ebreak  & ex_stage_valid_final, ~ex_pipe_stall) init False
        val csr_wr       = RegNextWhen(id2ex.csr_wr  & ex_stage_valid_final, ~ex_pipe_stall) init False
        val csr_rd       = RegNextWhen(id2ex.csr_rd  & ex_stage_valid_final, ~ex_pipe_stall) init False

        // payload
        val rs1_value      = RegNextWhen(ex_rs1_value_forwarded,    ~ex_pipe_stall)
        val alu_out        = RegNextWhen(alu_inst.io.alu_out,       ~ex_pipe_stall)
        val rd_wdata       = RegNextWhen(ex_rd_wdata,               ~ex_pipe_stall)
        val rs1_idx        = RegNextWhen(id2ex.rs1_idx,             ~ex_pipe_stall)
        val rd_idx         = RegNextWhen(id2ex.rd_idx,              ~ex_pipe_stall)
        val pc             = RegNextWhen(id2ex.pc,                  ~ex_pipe_stall)
        val instr          = RegNextWhen(id2ex.instr,               ~ex_pipe_stall)
        val csr_sel_imm    = RegNextWhen(id2ex.csr_sel_imm,         ~ex_pipe_stall)
        val csr_idx        = RegNextWhen(id2ex.csr_idx,             ~ex_pipe_stall)
        val rd_sel         = RegNextWhen(id2ex.rd_sel,              ~ex_pipe_stall)
        val csr_sel        = RegNextWhen(id2ex.csr_sel,             ~ex_pipe_stall)

        // Exception
        val exc_ill_instr      = RegNextWhen(id2ex.exc_ill_instr                    & ex_stage_valid_final, ~ex_pipe_stall) init False
        val exc_instr_addr_ma  = RegNextWhen(branch_unit_inst.io.exc_instr_addr_ma  & ex_stage_valid_final, ~ex_pipe_stall) init False
        val exc_ld_addr_ma     = RegNextWhen(dmem_ctrl_isnt.io.exc_ld_addr_ma       & ex_stage_valid_final, ~ex_pipe_stall) init False
        val exc_sd_addr_ma     = RegNextWhen(dmem_ctrl_isnt.io.exc_sd_addr_ma       & ex_stage_valid_final, ~ex_pipe_stall) init False
    }

    // =========================
    // Mem stage
    // =========================

    // csr
    // Note: uimm is the same field as rs1 in instruction so use rs1 here instead
    mcsr_inst.io.csr_bus.wdata := Mux(ex2mem.csr_sel_imm, ex2mem.rs1_idx.asBits.resized, ex2mem.rs1_value)
    mcsr_inst.io.csr_bus.addr  := ex2mem.csr_idx.asUInt
    mcsr_inst.io.csr_bus.wtype := ex2mem.csr_sel
    mcsr_inst.io.csr_bus.wen   := ex2mem.csr_wr & mem_stage_valid
    mcsr_inst.io.inc_br_cnt := branch_unit_inst.io.is_branch
    mcsr_inst.io.inc_pred_good := branch_unit_inst.io.is_branch & ~branch_unit_inst.io.take_branch


    // mem2wb_csr_rd is not used so far
    mcsr_inst.io.mtrap_enter  := trap_ctrl_inst.io.mtrap_enter
    mcsr_inst.io.mtrap_exit   := trap_ctrl_inst.io.mtrap_exit
    mcsr_inst.io.mtrap_mepc   := trap_ctrl_inst.io.mtrap_mepc
    mcsr_inst.io.mtrap_mcause := trap_ctrl_inst.io.mtrap_mcause
    mcsr_inst.io.mtrap_mtval  := trap_ctrl_inst.io.mtrap_mtval
    mcsr_inst.io.external_interrupt  := io.external_interrupt
    mcsr_inst.io.timer_interrupt     := io.timer_interrupt
    mcsr_inst.io.software_interrupt  := io.software_interrupt
    mcsr_inst.io.hartId              := B"0".resized
    mcsr_inst.io.inc_minstret        := wb_stage_valid

    // trap controller wire connection
    trap_ctrl_inst.io.external_interrupt := io.external_interrupt
    trap_ctrl_inst.io.timer_interrupt    := io.timer_interrupt
    trap_ctrl_inst.io.software_interrupt := io.software_interrupt
    trap_ctrl_inst.io.debug_interrupt    := io.debug_interrupt
    trap_ctrl_inst.io.exc_ill_instr      := ex2mem.exc_ill_instr
    trap_ctrl_inst.io.exc_instr_addr_ma  := ex2mem.exc_instr_addr_ma
    trap_ctrl_inst.io.exc_ld_addr_ma     := ex2mem.exc_ld_addr_ma
    trap_ctrl_inst.io.exc_sd_addr_ma     := ex2mem.exc_sd_addr_ma
    trap_ctrl_inst.io.mret               := ex2mem.mret
    trap_ctrl_inst.io.ecall              := ex2mem.ecall
    trap_ctrl_inst.io.cur_pc             := ex2mem.pc
    trap_ctrl_inst.io.stage_valid        := ex2mem.stage_valid // no one should flush mem stage right now
    trap_ctrl_inst.io.cur_instr          := ex2mem.instr
    trap_ctrl_inst.io.cur_dmem_addr      := ex2mem.alu_out.asUInt
    trap_ctrl_inst.io.mtvec              := mcsr_inst.io.mtvec
    trap_ctrl_inst.io.mepc               := mcsr_inst.io.mepc
    trap_ctrl_inst.io.mie_meie           := mcsr_inst.io.mie_meie
    trap_ctrl_inst.io.mie_mtie           := mcsr_inst.io.mie_mtie
    trap_ctrl_inst.io.mie_msie           := mcsr_inst.io.mie_msie
    trap_ctrl_inst.io.mstatus_mie        := mcsr_inst.io.mstatus_mie
    pc_inst.io.trap                      := trap_ctrl_inst.io.pc_trap
    pc_inst.io.trap_pc_in                := trap_ctrl_inst.io.pc_value

    // Select data between memory, alu, mul, div, and csr
    val mem_rd_wdata = ex2mem.alu_out.clone()
    switch(ex2mem.rd_sel){
        is(RdSelEnum.MEM) {mem_rd_wdata := dmem_ctrl_isnt.io.mc2cpu_data}
        is(RdSelEnum.CSR) {mem_rd_wdata := mcsr_inst.io.csr_bus.rdata}
        default {mem_rd_wdata := ex2mem.rd_wdata}
    }

    // Mem/WB stage pipeline
    val mem2wb = new Area {
        // control signal
        val stage_valid  = RegNextWhen(mem_stage_valid,           ~mem_pipe_stall) init False
        val rd_wr   = RegNextWhen(ex2mem.rd_wr & mem_stage_valid, ~mem_pipe_stall) init False

        // payload
        val rd_wdata = RegNextWhen(mem_rd_wdata,                  ~mem_pipe_stall)
        val rd_idx   = RegNextWhen(ex2mem.rd_idx,                 ~mem_pipe_stall)
    }

    // =========================
    // WB stage
    // =========================

    // == Write back to register == //
    regfile_inst.io.register_wr := mem2wb.rd_wr & wb_stage_valid
    regfile_inst.io.register_wr_addr := mem2wb.rd_idx
    regfile_inst.io.rd_wdata := mem2wb.rd_wdata

    //////////////////////////////////////////////////
    //         Other Components                     //
    //////////////////////////////////////////////////

    // Bypassing Logic
    val Bypassing = new Area {
        val id_rs1_match_ex_rd  = instr_dec_inst.io.rs1_rd & (instr_dec_inst.io.rs1_idx === id2ex.rd_idx)
        val id_rs1_match_mem_rd = instr_dec_inst.io.rs1_rd & (instr_dec_inst.io.rs1_idx === ex2mem.rd_idx)
        val id_rs2_match_ex_rd  = instr_dec_inst.io.rs2_rd & (instr_dec_inst.io.rs2_idx === id2ex.rd_idx)
        val id_rs2_match_mem_rd = instr_dec_inst.io.rs2_rd & (instr_dec_inst.io.rs2_idx === ex2mem.rd_idx)
        id_rs1_dep_ex_rd  := id_rs1_match_ex_rd & id2ex.rd_wr
        id_rs1_dep_mem_rd := id_rs1_match_mem_rd & ex2mem.rd_wr
        id_rs2_dep_ex_rd  := id_rs2_match_ex_rd & id2ex.rd_wr
        id_rs2_dep_mem_rd := id_rs2_match_mem_rd & ex2mem.rd_wr
        ex_rs1_value_forwarded := Mux(id2ex.rs1_dep_mem, ex2mem.rd_wdata, Mux(id2ex.rs1_dep_wb, mem2wb.rd_wdata, id2ex.rs1_value))
        ex_rs2_value_forwarded := Mux(id2ex.rs2_dep_mem, ex2mem.rd_wdata, Mux(id2ex.rs2_dep_wb, mem2wb.rd_wdata, id2ex.rs2_value))
    }

    // HDU - Hazard Detection Unit
    val HDU = new Area {
        // Control Hazard Detection
        // Load dependency on ID
        val id_stall_on_load_dep = (id_rs1_dep_ex_rd | id_rs2_dep_ex_rd) & id2ex.dmem_rd
        // csr dependency
        val id_rs1_depends_on_csr = (Bypassing.id_rs1_match_ex_rd & id2ex.csr_rd) | (Bypassing.id_rs1_match_mem_rd & ex2mem.csr_rd)
        val id_rs2_depends_on_csr = (Bypassing.id_rs2_match_ex_rd & id2ex.csr_rd) | (Bypassing.id_rs2_match_mem_rd & ex2mem.csr_rd)
        val id_stall_on_csr_dep = id_rs2_depends_on_csr | id_rs1_depends_on_csr

        // Flushing/Bubble Insertion
        val muldiv_stall_req = if (AppleRISCVCfg.USE_RV32M) mul_inst.io.mul_stall_req | div_inst.io.div_stall_req else False
        // Flush
        if_stage_valid  := ~branch_unit_inst.io.take_branch  & ~trap_ctrl_inst.io.trap_flush
        id_stage_valid  := if2id.stage_valid  & ~branch_unit_inst.io.take_branch   & ~trap_ctrl_inst.io.trap_flush
        ex_stage_valid  := id2ex.stage_valid  & ~trap_ctrl_inst.io.trap_flush
        mem_stage_valid := ex2mem.stage_valid & ~trap_ctrl_inst.io.trap_flush
        wb_stage_valid  := mem2wb.stage_valid & ~dmem_ctrl_isnt.io.dmem_stall_req
        // flush plus bubble insertion
        id_stage_valid_final := id_stage_valid & ~id_stall_on_load_dep & ~id_stall_on_csr_dep
        ex_stage_valid_final := ex_stage_valid & ~muldiv_stall_req

        // Stall
        if_pipe_stall  := if_stage_valid &
          (id_stall_on_load_dep | id_stall_on_csr_dep | dmem_ctrl_isnt.io.dmem_stall_req | muldiv_stall_req)
        id_pipe_stall  := id_stage_valid_final & muldiv_stall_req
        ex_pipe_stall  := ex_stage_valid_final & dmem_ctrl_isnt.io.dmem_stall_req
        mem_pipe_stall := mem_stage_valid & dmem_ctrl_isnt.io.dmem_stall_req
    }
}

object AppleRISCVMain {
    def main(args: Array[String]) {
        SpinalVerilog(new AppleRISCV).printPruned()
    }
}