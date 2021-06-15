///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: AppleRISCV
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

import spinal.core._
import spinal.lib.bus.amba3.ahblite.AhbLite3Master
import spinal.lib.master

case class AppleRISCVIO() extends Bundle {
    val ibus_ahb            = master(AhbLite3Master(AppleRISCVCfg.ibusAhbCfg))
    val dbus_ahb            = master(AhbLite3Master(AppleRISCVCfg.dbusAhbCfg))
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
    val ifu_inst = IFU()

    // ID Stage
    val instr_dec_inst = InstrDec()
    val regfile_inst   = RegFile()

    // EX Stage
    val alu_inst = ALU()
    val mul_inst = if (AppleRISCVCfg.USE_RV32M) AppleRISCVCMultiplier() else null
    val div_inst = if (AppleRISCVCfg.USE_RV32M) AppleRISCVDivider() else null
    val branch_unit_inst = BU()

    // MEM Stage
    val lsu_inst = LSU()

    // WB Stage
    val mcsr_inst       = MCSR()  // mcsr with hart 0
    val trap_ctrl_inst  = TrapCtrl()

    /////////////////////////////////////////////////////////////
    //     Pipeline Stage and Wire Connection For each Stage   //
    /////////////////////////////////////////////////////////////

    // =========================
    // Wires
    // =========================

    // Place holder for pipeline stage control
    val if_stage_valid  = Bool
    val id_stage_valid  = Bool
    val ex_stage_valid  = Bool
    val mem_stage_valid = Bool
    val wb_stage_valid  = Bool

    val if2id_pipe_valid  = Bool
    val id2ex_pipe_valid  = Bool
    val ex2mem_pipe_valid = Bool
    val mem2wb_pipe_valid = Bool

    val if2id_pipe_stall  = Bool
    val id2ex_pipe_stall  = Bool
    val ex2mem_pipe_stall = Bool
    val mem2wb_pipe_stall = Bool

    // =========================
    // IF Stage
    // =========================
    val IFStage = new Area {
        // PC
        pc_inst.io.branch := branch_unit_inst.io.take_branch
        pc_inst.io.stall  := if2id_pipe_stall   // this is used to stall the instruction
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
            bpu_inst.io.pc                  := pc_inst.io.pc_out
            bpu_inst.io.branch_update       := branch_unit_inst.io.is_branch_instr
            bpu_inst.io.branch_should_take  := branch_unit_inst.io.branch_should_take
            bpu_inst.io.branch_target_pc    := branch_unit_inst.io.target_pc
            bpu_inst.io.branch_instr_pc     := branch_instr_pc
            bpu_inst.io.stage_valid         := if_stage_valid
        }

        // IFU
        io.ibus_ahb <> ifu_inst.io.ibus_ahb
        ifu_inst.io.ifu_valid := if_stage_valid & ~lsu_inst.io.lsu_disable_ibus
        ifu_inst.io.pc := pc_inst.io.pc_out
        ifu_inst.io.stage_enable := ~if2id_pipe_stall
    }

    // =========================
    // IF/ID Pipeline
    // =========================
    val if2id = new Area {
        val pc          = RegNextWhen(pc_inst.io.pc_out, ~if2id_pipe_stall) init 0
        val stage_valid = RegNextWhen(if2id_pipe_valid,  ~if2id_pipe_stall) init False

        // Exception
        val exc_instr_acc_flt = RegNext(ifu_inst.io.exc_instr_acc_flt & if2id_pipe_valid) init False

        // [Optional] BPU result
        val pred_take = if (AppleRISCVCfg.USE_BPU) RegNextWhen(bpu_inst.io.pred_take, ~if2id_pipe_stall) else null
        val pred_pc   = if (AppleRISCVCfg.USE_BPU) RegNextWhen(bpu_inst.io.pred_pc,   ~if2id_pipe_stall) else null
    }

    // =========================
    // ID stage
    // =========================

    val IDStage = new Area {
        // InstrDec
        instr_dec_inst.io.instr     := ifu_inst.io.instruction
        instr_dec_inst.io.instr_vld := if2id.stage_valid

        // register file
        regfile_inst.io.rs1_rd_addr := instr_dec_inst.io.rs1_idx
        regfile_inst.io.rs2_rd_addr := instr_dec_inst.io.rs2_idx

        // Place holder for the forwarding detection
        val rs1_dep_ex_rd   = Bool
        val rs1_dep_mem_rd  = Bool
        val rs2_dep_ex_rd   = Bool
        val rs2_dep_mem_rd  = Bool

        val rs1Mux = new Area {
            val rs1_final = regfile_inst.io.rs1_data_out.clone()
            when(instr_dec_inst.io.op1_sel_pc) {
                rs1_final := if2id.pc.asBits
            }.elsewhen(instr_dec_inst.io.op1_sel_zero) {
                rs1_final := 0
            }.otherwise{
                rs1_final := regfile_inst.io.rs1_data_out
            }
        }
    }

    // =========================
    // ID/EX Pipeline
    // =========================
    val id2ex = new Area {
        // control signal
        val stage_valid   = RegNextWhen(id2ex_pipe_valid,             ~id2ex_pipe_stall) init False
        val rd_wr         = RegNextWhen(instr_dec_inst.io.rd_wr,      ~id2ex_pipe_stall) init False
        val lsu_wr       = RegNextWhen(instr_dec_inst.io.lsu_wr,    ~id2ex_pipe_stall) init False
        val lsu_rd       = RegNextWhen(instr_dec_inst.io.lsu_rd,    ~id2ex_pipe_stall) init False
        val branch_op     = RegNextWhen(instr_dec_inst.io.branch_op,  ~id2ex_pipe_stall) init False
        val jal_op        = RegNextWhen(instr_dec_inst.io.jal_op,     ~id2ex_pipe_stall) init False
        val jalr_op       = RegNextWhen(instr_dec_inst.io.jalr_op,    ~id2ex_pipe_stall) init False
        val mret          = RegNextWhen(instr_dec_inst.io.mret,       ~id2ex_pipe_stall) init False
        val ecall         = RegNextWhen(instr_dec_inst.io.ecall,      ~id2ex_pipe_stall) init False
        val ebreak        = RegNextWhen(instr_dec_inst.io.ebreak,     ~id2ex_pipe_stall) init False
        val csr_wr        = RegNextWhen(instr_dec_inst.io.csr_wr,     ~id2ex_pipe_stall) init False
        val csr_rd        = RegNextWhen(instr_dec_inst.io.csr_rd,     ~id2ex_pipe_stall) init False

        // Payload
        val rd_idx         = RegNextWhen(instr_dec_inst.io.rd_idx ,         ~id2ex_pipe_stall)
        val rs1_idx        = RegNextWhen(instr_dec_inst.io.rs1_idx,         ~id2ex_pipe_stall)
        val csr_sel_imm    = RegNextWhen(instr_dec_inst.io.csr_sel_imm,     ~id2ex_pipe_stall)
        val csr_idx        = RegNextWhen(instr_dec_inst.io.csr_idx,         ~id2ex_pipe_stall)
        val rd_sel         = RegNextWhen(instr_dec_inst.io.rd_sel,          ~id2ex_pipe_stall)
        val csr_sel        = RegNextWhen(instr_dec_inst.io.csr_sel,         ~id2ex_pipe_stall)
        val alu_opcode     = RegNextWhen(instr_dec_inst.io.alu_opcode,      ~id2ex_pipe_stall)
        val bu_opcode      = RegNextWhen(instr_dec_inst.io.bu_opcode,       ~id2ex_pipe_stall)
        val lsu_ld_byte   = RegNextWhen(instr_dec_inst.io.lsu_ld_byte,    ~id2ex_pipe_stall)
        val lsu_ld_hword  = RegNextWhen(instr_dec_inst.io.lsu_ld_hword,   ~id2ex_pipe_stall)
        val lsu_ld_unsign = RegNextWhen(instr_dec_inst.io.lsu_ld_unsign,  ~id2ex_pipe_stall)
        val rs1_value      = RegNextWhen(IDStage.rs1Mux.rs1_final ,         ~id2ex_pipe_stall)
        val rs2_value      = RegNextWhen(regfile_inst.io.rs2_data_out,      ~id2ex_pipe_stall)
        val imm_value      = RegNextWhen(instr_dec_inst.io.imm_value ,      ~id2ex_pipe_stall)
        val rs1_dep_mem    = RegNextWhen(IDStage.rs1_dep_ex_rd ,            ~id2ex_pipe_stall)
        val rs1_dep_wb     = RegNextWhen(IDStage.rs1_dep_mem_rd,            ~id2ex_pipe_stall)
        val rs2_dep_mem    = RegNextWhen(IDStage.rs2_dep_ex_rd ,            ~id2ex_pipe_stall)
        val rs2_dep_wb     = RegNextWhen(IDStage.rs2_dep_mem_rd,            ~id2ex_pipe_stall)
        val pc             = RegNextWhen(if2id.pc,                          ~id2ex_pipe_stall)
        val op2_sel_imm    = RegNextWhen(instr_dec_inst.io.op2_sel_imm ,    ~id2ex_pipe_stall)
        val instr          = RegNextWhen(ifu_inst.io.instruction ,          ~id2ex_pipe_stall)

        // Exception
        val exc_instr_acc_flt = RegNext(if2id.exc_instr_acc_flt         & id2ex_pipe_valid) init False
        val exc_ill_instr     = RegNext(instr_dec_inst.io.exc_ill_instr & id2ex_pipe_valid) init False

        // [Optional] mul/div
        val mul_op     = if (AppleRISCVCfg.USE_RV32M) RegNextWhen(instr_dec_inst.io.mul_op, ~id2ex_pipe_stall) init False else null
        val div_op     = if (AppleRISCVCfg.USE_RV32M) RegNextWhen(instr_dec_inst.io.div_op, ~id2ex_pipe_stall) init False else null
        val mul_opcode = if (AppleRISCVCfg.USE_RV32M) RegNextWhen(instr_dec_inst.io.mul_opcode, ~id2ex_pipe_stall) else null
        val div_opcode = if (AppleRISCVCfg.USE_RV32M) RegNextWhen(instr_dec_inst.io.div_opcode, ~id2ex_pipe_stall) else null

        // [Optional] BPU
        val pred_take  = if (AppleRISCVCfg.USE_BPU) RegNextWhen(if2id.pred_take, ~id2ex_pipe_stall) else null
        val pred_pc    = if (AppleRISCVCfg.USE_BPU) RegNextWhen(if2id.pred_pc, ~id2ex_pipe_stall) else null
    }

    // =========================
    // EX stage
    // =========================
    val EXStage = new Area {
        // Place holder for the final register value after the forwarding logic
        val rs1_value_forwarded = Bits(AppleRISCVCfg.XLEN bits)
        val rs2_value_forwarded = Bits(AppleRISCVCfg.XLEN bits)

        // Mux for the ALU operand
        val alu_operand2_muxout  = Mux(id2ex.op2_sel_imm, id2ex.imm_value.asBits, rs2_value_forwarded)

        // ALU
        alu_inst.io.operand_1    := rs1_value_forwarded
        alu_inst.io.operand_2    := alu_operand2_muxout
        alu_inst.io.pc           := id2ex.pc
        alu_inst.io.alu_opcode   := id2ex.alu_opcode

        if (AppleRISCVCfg.USE_RV32M) {
            // Multiplier
            mul_inst.io.stage_valid  := ex_stage_valid
            mul_inst.io.multiplicand := rs1_value_forwarded
            mul_inst.io.multiplier   := rs2_value_forwarded
            mul_inst.io.mul_req      := id2ex.mul_op & ex_stage_valid
            mul_inst.io.mul_opcode   := id2ex.mul_opcode

            // Divider
            div_inst.io.stage_valid  := ex_stage_valid
            div_inst.io.dividend     := rs1_value_forwarded
            div_inst.io.divisor      := rs2_value_forwarded
            div_inst.io.div_req      := id2ex.div_op & ex_stage_valid
            div_inst.io.div_opcode   := id2ex.div_opcode
        }

        // Select data between alu, mul, div
        val rd_wdata = alu_inst.io.alu_out.clone()
        if (AppleRISCVCfg.USE_RV32M) {
            switch(id2ex.rd_sel){
                is(RdSelEnum.MUL) {rd_wdata := mul_inst.io.result}
                is(RdSelEnum.DIV) {rd_wdata := div_inst.io.result}
                default {rd_wdata := alu_inst.io.alu_out}
            }
        } else {
            rd_wdata := alu_inst.io.alu_out
        }

        // Branch Unit
        branch_unit_inst.io.current_pc      := id2ex.pc
        branch_unit_inst.io.imm_value       := id2ex.imm_value(20 downto 0)
        branch_unit_inst.io.rs1_value       := rs1_value_forwarded
        branch_unit_inst.io.rs2_value       := rs2_value_forwarded
        branch_unit_inst.io.bu_opcode       := id2ex.bu_opcode
        branch_unit_inst.io.br_op           := id2ex.branch_op & ex_stage_valid
        branch_unit_inst.io.jal_op          := id2ex.jal_op    & ex_stage_valid
        branch_unit_inst.io.jalr_op         := id2ex.jalr_op   & ex_stage_valid
        branch_unit_inst.io.stage_valid     := ex_stage_valid
        if (AppleRISCVCfg.USE_BPU) {
            branch_unit_inst.io.pred_take   := id2ex.pred_take
            branch_unit_inst.io.pred_pc     := id2ex.pred_pc
            IFStage.branch_instr_pc         := id2ex.pc
        }

        // Memory Controller Input
        lsu_inst.io.dbus_ahb    <> io.dbus_ahb
        lsu_inst.io.write       := id2ex.lsu_wr & ex_stage_valid
        lsu_inst.io.read        := id2ex.lsu_rd & ex_stage_valid
        lsu_inst.io.addr        := id2ex.imm_value.asUInt + rs1_value_forwarded.asUInt
        lsu_inst.io.wdata       := rs2_value_forwarded
        lsu_inst.io.rw_byte     := id2ex.lsu_ld_byte
        lsu_inst.io.rw_half     := id2ex.lsu_ld_hword
        lsu_inst.io.rd_unsigned := id2ex.lsu_ld_unsign
    }

    // =========================
    // EX/Mem Pipeline
    // =========================
    val ex2mem = new Area {
        // control signal
        val stage_valid  = RegNextWhen(ex2mem_pipe_valid, ~ex2mem_pipe_stall) init False
        val rd_wr        = RegNextWhen(id2ex.rd_wr , ~ex2mem_pipe_stall) init False
        val mret         = RegNextWhen(id2ex.mret  , ~ex2mem_pipe_stall) init False
        val ecall        = RegNextWhen(id2ex.ecall , ~ex2mem_pipe_stall) init False
        val ebreak       = RegNextWhen(id2ex.ebreak, ~ex2mem_pipe_stall) init False
        val csr_wr       = RegNextWhen(id2ex.csr_wr, ~ex2mem_pipe_stall) init False
        val csr_rd       = RegNextWhen(id2ex.csr_rd, ~ex2mem_pipe_stall) init False
        val is_branch_instr = RegNextWhen(branch_unit_inst.io.take_branch, ~ex2mem_pipe_stall) init False

        // payload
        val rs1_value      = RegNextWhen(EXStage.rs1_value_forwarded,   ~ex2mem_pipe_stall)
        val alu_out        = RegNextWhen(alu_inst.io.alu_out,           ~ex2mem_pipe_stall)
        val rd_wdata       = RegNextWhen(EXStage.rd_wdata,              ~ex2mem_pipe_stall)
        val rs1_idx        = RegNextWhen(id2ex.rs1_idx,                 ~ex2mem_pipe_stall)
        val rd_idx         = RegNextWhen(id2ex.rd_idx,                  ~ex2mem_pipe_stall)
        val pc             = RegNextWhen(id2ex.pc,                      ~ex2mem_pipe_stall)
        val instr          = RegNextWhen(id2ex.instr,                   ~ex2mem_pipe_stall)
        val csr_sel_imm    = RegNextWhen(id2ex.csr_sel_imm,             ~ex2mem_pipe_stall)
        val csr_idx        = RegNextWhen(id2ex.csr_idx,                 ~ex2mem_pipe_stall)
        val rd_sel         = RegNextWhen(id2ex.rd_sel,                  ~ex2mem_pipe_stall)
        val csr_sel        = RegNextWhen(id2ex.csr_sel,                 ~ex2mem_pipe_stall)
        val target_pc      = RegNextWhen(branch_unit_inst.io.target_pc, ~ex2mem_pipe_stall)

        // Exception
        val exc_instr_acc_flt  = RegNext(id2ex.exc_instr_acc_flt               & ex2mem_pipe_valid) init False
        val exc_ill_instr      = RegNext(id2ex.exc_ill_instr                   & ex2mem_pipe_valid) init False
        val exc_instr_addr_ma  = RegNext(branch_unit_inst.io.exc_instr_addr_ma & ex2mem_pipe_valid) init False
        val exc_ld_addr_ma     = RegNext(lsu_inst.io.exc_ld_addr_ma      & ex2mem_pipe_valid) init False
        val exc_sd_addr_ma     = RegNext(lsu_inst.io.exc_sd_addr_ma      & ex2mem_pipe_valid) init False
        val exc_ld_acc_flt     = RegNext(lsu_inst.io.exc_ld_acc_flt      & ex2mem_pipe_valid) init False
        val exc_sd_acc_flt     = RegNext(lsu_inst.io.exc_sd_acc_flt      & ex2mem_pipe_valid) init False
    }

    // =========================
    // Mem stage
    // =========================
    val MEMStage = new Area {
        // csr
        mcsr_inst.io.csr_bus.wdata := Mux(ex2mem.csr_sel_imm, ex2mem.rs1_idx.asBits.resized, ex2mem.rs1_value)
        mcsr_inst.io.csr_bus.addr  := ex2mem.csr_idx.asUInt
        mcsr_inst.io.csr_bus.wtype := ex2mem.csr_sel
        mcsr_inst.io.csr_bus.wen   := ex2mem.csr_wr & mem_stage_valid
        mcsr_inst.io.inc_br_cnt    := branch_unit_inst.io.is_branch_instr
        mcsr_inst.io.inc_pred_good := branch_unit_inst.io.is_branch_instr & ~branch_unit_inst.io.take_branch

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
        trap_ctrl_inst.io.exc_instr_acc_flt  := ex2mem.exc_instr_acc_flt
        trap_ctrl_inst.io.exc_ill_instr      := ex2mem.exc_ill_instr
        trap_ctrl_inst.io.exc_instr_addr_ma  := ex2mem.exc_instr_addr_ma
        trap_ctrl_inst.io.exc_ld_addr_ma     := ex2mem.exc_ld_addr_ma
        trap_ctrl_inst.io.exc_sd_addr_ma     := ex2mem.exc_sd_addr_ma
        trap_ctrl_inst.io.exc_ld_acc_flt     := ex2mem.exc_ld_acc_flt
        trap_ctrl_inst.io.exc_sd_acc_flt     := ex2mem.exc_sd_acc_flt
        trap_ctrl_inst.io.mret               := ex2mem.mret  & ex2mem.stage_valid
        trap_ctrl_inst.io.ecall              := ex2mem.ecall & ex2mem.stage_valid
        trap_ctrl_inst.io.cur_pc             := ex2mem.pc
        trap_ctrl_inst.io.is_branch_instr    := ex2mem.is_branch_instr
        trap_ctrl_inst.io.branch_target_pc   := ex2mem.target_pc
        trap_ctrl_inst.io.stage_valid        := ex2mem.stage_valid // no one should flush mem stage right now
        trap_ctrl_inst.io.cur_instr          := ex2mem.instr
        trap_ctrl_inst.io.cur_lsu_addr       := ex2mem.alu_out.asUInt
        trap_ctrl_inst.io.mtvec              := mcsr_inst.io.mtvec
        trap_ctrl_inst.io.mepc               := mcsr_inst.io.mepc
        trap_ctrl_inst.io.mie_meie           := mcsr_inst.io.mie_meie
        trap_ctrl_inst.io.mie_mtie           := mcsr_inst.io.mie_mtie
        trap_ctrl_inst.io.mie_msie           := mcsr_inst.io.mie_msie
        trap_ctrl_inst.io.mstatus_mie        := mcsr_inst.io.mstatus_mie
        pc_inst.io.trap                      := trap_ctrl_inst.io.pc_trap
        pc_inst.io.trap_pc_in                := trap_ctrl_inst.io.pc_value

        // Select data between memory, alu, mul, div, and csr
        val rd_wdata = ex2mem.alu_out.clone()
        switch(ex2mem.rd_sel){
            is(RdSelEnum.MEM) {rd_wdata := lsu_inst.io.rdata}
            is(RdSelEnum.CSR) {rd_wdata := mcsr_inst.io.csr_bus.rdata}
            default {rd_wdata := ex2mem.rd_wdata}
        }
    }

    // =========================
    // Mem/WB stage pipeline
    // =========================
    val mem2wb = new Area {
        // control signal
        val stage_valid  = RegNextWhen(mem2wb_pipe_valid, ~mem2wb_pipe_stall) init False
        val rd_wr        = RegNextWhen(ex2mem.rd_wr,    ~mem2wb_pipe_stall) init False

        // payload
        val rd_wdata = RegNextWhen(MEMStage.rd_wdata, ~mem2wb_pipe_stall)
        val rd_idx   = RegNextWhen(ex2mem.rd_idx,     ~mem2wb_pipe_stall)
    }

    // =========================
    // WB stage
    // =========================
    val WBStage = new Area {
        // == Write back to register == //
        regfile_inst.io.register_wr      := mem2wb.rd_wr & wb_stage_valid
        regfile_inst.io.register_wr_addr := mem2wb.rd_idx
        regfile_inst.io.rd_wdata         := mem2wb.rd_wdata
    }

    //////////////////////////////////////////////////
    //         Other Components                     //
    //////////////////////////////////////////////////

    // Bypassing Logic
    val Bypassing = new Area {
        val id_rs1_match_ex_rd  = instr_dec_inst.io.rs1_rd & (instr_dec_inst.io.rs1_idx === id2ex.rd_idx)  & id_stage_valid
        val id_rs1_match_mem_rd = instr_dec_inst.io.rs1_rd & (instr_dec_inst.io.rs1_idx === ex2mem.rd_idx) & id_stage_valid
        val id_rs2_match_ex_rd  = instr_dec_inst.io.rs2_rd & (instr_dec_inst.io.rs2_idx === id2ex.rd_idx)  & id_stage_valid
        val id_rs2_match_mem_rd = instr_dec_inst.io.rs2_rd & (instr_dec_inst.io.rs2_idx === ex2mem.rd_idx) & id_stage_valid
        IDStage.rs1_dep_ex_rd  := id_rs1_match_ex_rd  & id2ex.rd_wr  & ex_stage_valid
        IDStage.rs1_dep_mem_rd := id_rs1_match_mem_rd & ex2mem.rd_wr & mem_stage_valid
        IDStage.rs2_dep_ex_rd  := id_rs2_match_ex_rd  & id2ex.rd_wr  & ex_stage_valid
        IDStage.rs2_dep_mem_rd := id_rs2_match_mem_rd & ex2mem.rd_wr & mem_stage_valid
        EXStage.rs1_value_forwarded := Mux(id2ex.rs1_dep_mem, ex2mem.rd_wdata, Mux(id2ex.rs1_dep_wb, mem2wb.rd_wdata, id2ex.rs1_value))
        EXStage.rs2_value_forwarded := Mux(id2ex.rs2_dep_mem, ex2mem.rd_wdata, Mux(id2ex.rs2_dep_wb, mem2wb.rd_wdata, id2ex.rs2_value))
    }

    // HDU - Hazard Detection Unit
    val HDU = new Area {
        // Control Hazard Detection
        // Load dependency on ID
        val id_stall_on_load_dep = (IDStage.rs1_dep_ex_rd | IDStage.rs2_dep_ex_rd) & id2ex.lsu_rd & ex_stage_valid
        // csr dependency
        val id_rs1_depends_on_csr = (Bypassing.id_rs1_match_ex_rd  & id2ex.csr_rd  & ex_stage_valid) |
                                    (Bypassing.id_rs1_match_mem_rd & ex2mem.csr_rd & mem_stage_valid)
        val id_rs2_depends_on_csr = (Bypassing.id_rs2_match_ex_rd  & id2ex.csr_rd  & ex_stage_valid) |
                                    (Bypassing.id_rs2_match_mem_rd & ex2mem.csr_rd & mem_stage_valid)
        val id_stall_on_csr_dep   = id_rs2_depends_on_csr | id_rs1_depends_on_csr
        // mul/div stall request
        val muldiv_stall_req      = if (AppleRISCVCfg.USE_RV32M) mul_inst.io.mul_stall_req | div_inst.io.div_stall_req else False

        // Memory requested pipeline stall should not stall the entire pipeline, it should let the MEM/WB stage go.
        // This is to handle the situation when we have back-to-back memory read access request.
        // However, there is one exception, that is when the address calculation or write data depends on the data in WB stage
        // In this case, we should stall the MEM/WB stage and once the memory access complete, we should release MEM/WB stall
        val mem_stall_on_addr_dep = lsu_inst.io.lsu_wait_data &
          (id2ex.rs1_dep_mem | id2ex.rs1_dep_wb | id2ex.rs2_dep_mem | id2ex.rs2_dep_wb )

        // ================================
        // Pipeline Stage Control
        // ================================

        // Flush request
        val if_flush  = branch_unit_inst.io.take_branch | trap_ctrl_inst.io.exc_flush | trap_ctrl_inst.io.int_flush
        val id_flush  = branch_unit_inst.io.take_branch | trap_ctrl_inst.io.exc_flush | trap_ctrl_inst.io.int_flush
        val ex_flush  = trap_ctrl_inst.io.exc_flush | trap_ctrl_inst.io.int_flush
        val mem_flush = trap_ctrl_inst.io.exc_flush

        // Stall request
        val if2id_stall  = id_stall_on_load_dep | id_stall_on_csr_dep | lsu_inst.io.lsu_wait_data |
                           muldiv_stall_req | ifu_inst.io.ifu_wait_ibus | lsu_inst.io.lsu_wait_dbus
        val id2ex_stall  = lsu_inst.io.lsu_wait_data | muldiv_stall_req | lsu_inst.io.lsu_wait_dbus
        val ex2mem_stall = lsu_inst.io.lsu_wait_data | mem_stall_on_addr_dep | lsu_inst.io.lsu_wait_dbus
        val mem2wb_stall = mem_stall_on_addr_dep | lsu_inst.io.lsu_wait_dbus

        // Insert NOP
        val if2id_nop = lsu_inst.io.lsu_disable_ibus
        val id2ex_nop = id_stall_on_csr_dep | id_stall_on_load_dep | ifu_inst.io.ifu_wait_ibus
        val ex2mem_nop = muldiv_stall_req
        val mem2wb_nop = lsu_inst.io.lsu_wait_data

        // Final Stage valid signal
        if_stage_valid  := ~if_flush
        id_stage_valid  := if2id.stage_valid  & ~id_flush
        ex_stage_valid  := id2ex.stage_valid  & ~ex_flush
        mem_stage_valid := ex2mem.stage_valid & ~mem_flush
        wb_stage_valid  := mem2wb.stage_valid

        // Final Pipeline valid signal (insert nop or not)
        if2id_pipe_valid  := if_stage_valid & ~if2id_nop
        id2ex_pipe_valid  := id_stage_valid & ~id2ex_nop
        ex2mem_pipe_valid := ex_stage_valid & ~ex2mem_nop
        mem2wb_pipe_valid := mem_stage_valid & ~mem2wb_nop

        if2id_pipe_stall  := if2id_stall  & ~if_flush
        id2ex_pipe_stall  := id2ex_stall  & ~id_flush
        ex2mem_pipe_stall := ex2mem_stall & ~ex_flush
        mem2wb_pipe_stall := mem2wb_stall & ~mem_flush
    }
}

object AppleRISCVMain {
    def main(args: Array[String]) {
        SpinalVerilog(new AppleRISCV).printPruned()
    }
}