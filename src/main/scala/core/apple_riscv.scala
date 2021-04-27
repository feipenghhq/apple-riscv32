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

package core

import bus.sib._
import spinal.core._
import spinal.lib.master

case class apple_riscv_io(param: CPU_PARAM) extends Bundle {
    val clk                 = in Bool
    val reset               = in Bool
    val imem_sib            = master(Sib(param.sibCfg))
    val dmem_sib            = master(Sib(param.sibCfg))
    val external_interrupt  = in Bool
    val timer_interrupt     = in Bool
    val software_interrupt  = in Bool
    val debug_interrupt     = in Bool
}

case class apple_riscv (param: CPU_PARAM) extends Component {

    noIoPrefix()

    val io = apple_riscv_io(param)

    val coreClockDomain = ClockDomain.internal(
        name = "core",
        frequency = FixedFrequency(50 MHz),
        config = ClockDomainConfig(
            clockEdge        = RISING,
            resetKind        = SYNC,
            resetActiveLevel = HIGH
        )
    )

    coreClockDomain.clock := io.clk
    coreClockDomain.reset := io.reset

    val core = new ClockingArea(coreClockDomain) {

        //////////////////////////////////////////////////////
        //     Pipeline Stage and Component Instantiation   //
        //////////////////////////////////////////////////////

        // =========================
        // Wires
        // =========================

        // Place holder for flush and bubble insertion
        // The stage prefix indicate if the instruction in the current stage is valid
        val if_instr_valid  = Bool
        val id_instr_valid  = Bool
        val ex_instr_valid  = Bool
        val mem_instr_valid = Bool
        val wb_instr_valid  = Bool

        // Place holder for the stall/run signal
        // The stage prefix indicate if we need to stall the pipeline entering this stage
        // For example, id_pipe_stall will stall if2id pipeline stage
        val if_pipe_stall  = Bool
        val id_pipe_stall  = Bool
        val ex_pipe_stall  = Bool
        val mem_pipe_stall = Bool
        val wb_pipe_stall  = Bool
        val if_pipe_run    = ~if_pipe_stall
        val id_pipe_run    = ~id_pipe_stall
        val ex_pipe_run    = ~ex_pipe_stall
        val mem_pipe_run   = ~mem_pipe_stall
        val wb_pipe_run    = ~wb_pipe_stall

        // Place holder for branch/jump related signal
        val target_pc    = UInt(param.PC_WIDTH bits)
        val branch_taken = Bool

        // =========================
        // IF Stage
        // =========================

        // == program counter == //
        val pc_inst = program_counter(param)
        pc_inst.io.branch := branch_taken
        pc_inst.io.stall  := if_pipe_stall
        pc_inst.io.branch_pc_in  := target_pc

        // == instruction RAM Controller == //
        val imem_ctrl_inst = imem_ctrl(param)
        io.imem_sib <> imem_ctrl_inst.imem_sib
        imem_ctrl_inst.io.cpu2mc_addr   := pc_inst.io.pc_out
        imem_ctrl_inst.io.cpu2mc_en     := if_pipe_run


        // =========================
        // IF/ID Pipeline
        // =========================

        val if2id_pc          = RegNextWhen(pc_inst.io.pc_out, id_pipe_run) init 0
        val if2id_instr_valid = RegNextWhen(if_instr_valid, id_pipe_run) init False

        // =========================
        // ID stage
        // =========================

        // == instruction decoder == //
        val instr_dec_inst = instr_dec(param)
        instr_dec_inst.io.instr := imem_ctrl_inst.io.mc2cpu_data
        instr_dec_inst.io.instr_vld := if2id_instr_valid

        // == register file == //
        val regfile_inst = regfile(param)
        regfile_inst.io.rs1_rd_addr := instr_dec_inst.io.rs1_idx
        regfile_inst.io.rs2_rd_addr := instr_dec_inst.io.rs2_idx
        val rs1_data_out = regfile_inst.io.rs1_data_out
        val rs2_data_out = regfile_inst.io.rs2_data_out


        // == exception == //
        val id_exception = instr_dec_inst.io.invld_instr

        // =========================
        // ID/EX Pipeline
        // =========================

        // == Control signal == //
        val id2ex_instr_valid   = RegNextWhen(id_instr_valid, ex_pipe_run) init False
        val id2ex_rd_wr         = RegNextWhen(instr_dec_inst.io.rd_wr       & id_instr_valid, ex_pipe_run) init False
        val id2ex_data_ram_wr   = RegNextWhen(instr_dec_inst.io.data_ram_wr & id_instr_valid, ex_pipe_run) init False
        val id2ex_data_ram_rd   = RegNextWhen(instr_dec_inst.io.data_ram_rd & id_instr_valid, ex_pipe_run) init False
        val id2ex_rs1_rd        = RegNextWhen(instr_dec_inst.io.rs1_rd      & id_instr_valid, ex_pipe_run) init False
        val id2ex_rs2_rd        = RegNextWhen(instr_dec_inst.io.rs2_rd      & id_instr_valid, ex_pipe_run) init False
        val id2ex_branch_op     = RegNextWhen(instr_dec_inst.io.branch_op   & id_instr_valid, ex_pipe_run) init False
        val id2ex_jal_op        = RegNextWhen(instr_dec_inst.io.jal_op      & id_instr_valid, ex_pipe_run) init False
        val id2ex_jalr_op       = RegNextWhen(instr_dec_inst.io.jalr_op     & id_instr_valid, ex_pipe_run) init False
        val id2ex_mret          = RegNextWhen(instr_dec_inst.io.mret        & id_instr_valid, ex_pipe_run) init False
        val id2ex_ecall         = RegNextWhen(instr_dec_inst.io.ecall       & id_instr_valid, ex_pipe_run) init False
        val id2ex_ebreak        = RegNextWhen(instr_dec_inst.io.ebreak      & id_instr_valid, ex_pipe_run) init False
        // CSR control signal
        val id2ex_csr_wr        = RegNextWhen(instr_dec_inst.io.csr_wr & id_instr_valid, ex_pipe_run) init False
        val id2ex_csr_rd        = RegNextWhen(instr_dec_inst.io.csr_rd & id_instr_valid, ex_pipe_run) init False
        val id2ex_csr_rw        = RegNextWhen(instr_dec_inst.io.csr_rw, ex_pipe_run)
        val id2ex_csr_rs        = RegNextWhen(instr_dec_inst.io.csr_rs, ex_pipe_run)
        val id2ex_csr_rc        = RegNextWhen(instr_dec_inst.io.csr_rc, ex_pipe_run)
        val id2ex_csr_sel_imm   = RegNextWhen(instr_dec_inst.io.csr_sel_imm, ex_pipe_run)
        val id2ex_csr_idx       = RegNextWhen(instr_dec_inst.io.csr_idx, ex_pipe_run)


        // == Other signal == //

        // Instruction field
        val id2ex_rd_idx    = RegNextWhen(instr_dec_inst.io.rd_idx , ex_pipe_run)
        val id2ex_rs1_idx   = RegNextWhen(instr_dec_inst.io.rs1_idx, ex_pipe_run)
        val id2ex_rs2_idx   = RegNextWhen(instr_dec_inst.io.rs2_idx, ex_pipe_run)

        // ALU control signal
        val id2ex_alu_op_and  = RegNextWhen(instr_dec_inst.io.alu_op_and  , ex_pipe_run)
        val id2ex_alu_op_or   = RegNextWhen(instr_dec_inst.io.alu_op_or   , ex_pipe_run)
        val id2ex_alu_op_xor  = RegNextWhen(instr_dec_inst.io.alu_op_xor  , ex_pipe_run)
        val id2ex_alu_op_add  = RegNextWhen(instr_dec_inst.io.alu_op_add  , ex_pipe_run)
        val id2ex_alu_op_sub  = RegNextWhen(instr_dec_inst.io.alu_op_sub  , ex_pipe_run)
        val id2ex_alu_op_sra  = RegNextWhen(instr_dec_inst.io.alu_op_sra  , ex_pipe_run)
        val id2ex_alu_op_srl  = RegNextWhen(instr_dec_inst.io.alu_op_srl  , ex_pipe_run)
        val id2ex_alu_op_sll  = RegNextWhen(instr_dec_inst.io.alu_op_sll  , ex_pipe_run)
        val id2ex_alu_op_slt  = RegNextWhen(instr_dec_inst.io.alu_op_slt  , ex_pipe_run)
        val id2ex_alu_op_sltu = RegNextWhen(instr_dec_inst.io.alu_op_sltu , ex_pipe_run)
        val id2ex_alu_op_eqt  = RegNextWhen(instr_dec_inst.io.alu_op_eqt  , ex_pipe_run)
        val id2ex_alu_op_invb0 = RegNextWhen(instr_dec_inst.io.alu_op_invb0, ex_pipe_run)

        // Data RAM signal
        val id2ex_data_ram_ld_byte   = RegNextWhen(instr_dec_inst.io.data_ram_ld_byte  , ex_pipe_run)
        val id2ex_data_ram_ld_hword  = RegNextWhen(instr_dec_inst.io.data_ram_ld_hword , ex_pipe_run)
        val id2ex_data_ram_ld_unsign = RegNextWhen(instr_dec_inst.io.data_ram_ld_unsign, ex_pipe_run)

        // register file value and immediate value
        val id2ex_rs1_value = RegNextWhen(regfile_inst.io.rs1_data_out, ex_pipe_run)
        val id2ex_rs2_value = RegNextWhen(regfile_inst.io.rs2_data_out, ex_pipe_run)
        val id2ex_imm_value = RegNextWhen(instr_dec_inst.io.imm_value , ex_pipe_run)
        val id2ex_brjp_imm_value = RegNextWhen(instr_dec_inst.io.jump_imm_value , ex_pipe_run)

        // Others
        val id2ex_pc            = RegNextWhen(if2id_pc, ex_pipe_run)
        val id2ex_op2_sel_imm   = RegNextWhen(instr_dec_inst.io.op2_sel_imm , ex_pipe_run)
        val id2ex_op1_sel_pc    = RegNextWhen(instr_dec_inst.io.op1_sel_pc  , ex_pipe_run)
        val id2ex_op1_sel_zero  = RegNextWhen(instr_dec_inst.io.op1_sel_zero, ex_pipe_run)
        val id2ex_instr         = RegNextWhen(imem_ctrl_inst.io.mc2cpu_data , ex_pipe_run)

        // Exception
        val id2ex_illegal_instr_exception = RegNextWhen(instr_dec_inst.io.invld_instr & id_instr_valid, ex_pipe_run) init False

        // =========================
        // EX stage
        // =========================

        // Place holder for the final register value after the forwarding logic
        val ex_rs1_value_forwarded = Bits(param.DATA_WIDTH bits)
        val ex_rs2_value_forwarded = Bits(param.DATA_WIDTH bits)

        // ALU instance
        val alu_inst = alu(param)

        // Mux for the ALU operand
        val alu_operand1_muxout  = Mux(id2ex_op1_sel_zero, B"32'h0", Mux(id2ex_op1_sel_pc, id2ex_pc.asBits, ex_rs1_value_forwarded))
        val alu_operand2_muxout  = Mux(id2ex_op2_sel_imm, id2ex_imm_value.asBits, ex_rs2_value_forwarded)

        alu_inst.io.operand_1    := alu_operand1_muxout
        alu_inst.io.operand_2    := alu_operand2_muxout
        alu_inst.io.alu_op_and   := id2ex_alu_op_and
        alu_inst.io.alu_op_or    := id2ex_alu_op_or
        alu_inst.io.alu_op_xor   := id2ex_alu_op_xor
        alu_inst.io.alu_op_add   := id2ex_alu_op_add
        alu_inst.io.alu_op_sub   := id2ex_alu_op_sub
        alu_inst.io.alu_op_sra   := id2ex_alu_op_sra
        alu_inst.io.alu_op_srl   := id2ex_alu_op_srl
        alu_inst.io.alu_op_sll   := id2ex_alu_op_sll
        alu_inst.io.alu_op_slt   := id2ex_alu_op_slt
        alu_inst.io.alu_op_sltu  := id2ex_alu_op_sltu
        alu_inst.io.alu_op_eqt   := id2ex_alu_op_eqt
        alu_inst.io.alu_op_invb0 := id2ex_alu_op_invb0

        // Branch unit instance
        val branch_unit_inst = branch_unit(param)

        branch_unit_inst.io.branch_result   := alu_inst.io.alu_out(0)
        branch_unit_inst.io.current_pc      := id2ex_pc
        branch_unit_inst.io.imm_value       := id2ex_brjp_imm_value
        branch_unit_inst.io.rs1_value       := ex_rs1_value_forwarded
        branch_unit_inst.io.br_op           := id2ex_branch_op
        branch_unit_inst.io.jal_op          := id2ex_jal_op
        branch_unit_inst.io.jalr_op         := id2ex_jalr_op
        target_pc                           := branch_unit_inst.io.target_pc
        branch_taken                        := branch_unit_inst.io.branch_taken

        // == exception == //
        val ex_exception = id2ex_illegal_instr_exception | branch_unit_inst.io.instr_addr_misalign_exception

        // =========================
        // EX/Mem Pipeline
        // =========================

        // == control signal == //
        val ex2mem_instr_valid = RegNextWhen(ex_instr_valid, mem_pipe_run) init False
        val ex2mem_rd_wr       = RegNextWhen(id2ex_rd_wr       & ex_instr_valid, mem_pipe_run) init False
        val ex2mem_data_ram_wr = RegNextWhen(id2ex_data_ram_wr & ex_instr_valid, mem_pipe_run) init False
        val ex2mem_data_ram_rd = RegNextWhen(id2ex_data_ram_rd & ex_instr_valid, mem_pipe_run) init False
        val ex2mem_mret        = RegNextWhen(id2ex_mret        & ex_instr_valid, mem_pipe_run) init False
        val ex2mem_ecall       = RegNextWhen(id2ex_ecall       & ex_instr_valid, mem_pipe_run) init False
        val ex2mem_ebreak      = RegNextWhen(id2ex_ebreak      & ex_instr_valid, mem_pipe_run) init False
        // CSR control signal
        val ex2mem_csr_wr       = RegNextWhen(id2ex_csr_wr & ex_instr_valid, mem_pipe_run) init False
        val ex2mem_csr_rd       = RegNextWhen(id2ex_csr_rd & ex_instr_valid, mem_pipe_run) init False
        val ex2mem_csr_rw       = RegNextWhen(id2ex_csr_rw, mem_pipe_run)
        val ex2mem_csr_rs       = RegNextWhen(id2ex_csr_rs, mem_pipe_run)
        val ex2mem_csr_rc       = RegNextWhen(id2ex_csr_rc, mem_pipe_run)
        val ex2mem_csr_sel_imm  = RegNextWhen(id2ex_csr_sel_imm, mem_pipe_run)
        val ex2mem_csr_idx      = RegNextWhen(id2ex_csr_idx, mem_pipe_run)

        // == other signal == //

        val ex2mem_rs1_value = RegNextWhen(ex_rs1_value_forwarded, mem_pipe_run)
        val ex2mem_alu_out   = RegNextWhen(alu_inst.io.alu_out, mem_pipe_run)
        val ex2mem_rs1_idx   = RegNextWhen(id2ex_rs1_idx, mem_pipe_run)
        val ex2mem_rs2_idx   = RegNextWhen(id2ex_rs2_idx, mem_pipe_run)
        val ex2mem_rd_idx    = RegNextWhen(id2ex_rd_idx, mem_pipe_run)
        val ex2mem_rs2_value = RegNextWhen(ex_rs2_value_forwarded, mem_pipe_run)
        val ex2mem_pc        = RegNextWhen(id2ex_pc, mem_pipe_run)
        val ex2mem_instr     = RegNextWhen(id2ex_instr, mem_pipe_run)

        val ex2mem_data_ram_ld_byte     = RegNextWhen(id2ex_data_ram_ld_byte, mem_pipe_run)
        val ex2mem_data_ram_ld_hword    = RegNextWhen(id2ex_data_ram_ld_hword, mem_pipe_run)
        val ex2mem_data_ram_ld_unsign   = RegNextWhen(id2ex_data_ram_ld_unsign, mem_pipe_run)


        // Exception
        val ex2mem_illegal_instr_exception = RegNextWhen(id2ex_illegal_instr_exception & ex_instr_valid, mem_pipe_run) init False
        val ex2mem_instr_addr_misalign_exception = RegNextWhen(branch_unit_inst.io.instr_addr_misalign_exception & ex_instr_valid, mem_pipe_run) init False

        // =========================
        // Mem stage
        // =========================

        val dmem_addr = ex2mem_alu_out.asUInt

        // == Memory Controller == //
        val dmem_ctrl_isnt = dmem_ctrl(param)
        dmem_ctrl_isnt.dmem_sib <> io.dmem_sib
        // CPU side
        dmem_ctrl_isnt.io.cpu2mc_wr                 := ex2mem_data_ram_wr
        dmem_ctrl_isnt.io.cpu2mc_rd                 := ex2mem_data_ram_rd
        dmem_ctrl_isnt.io.cpu2mc_addr               := dmem_addr
        dmem_ctrl_isnt.io.cpu2mc_data               := ex2mem_rs2_value
        dmem_ctrl_isnt.io.cpu2mc_mem_LS_byte        := ex2mem_data_ram_ld_byte
        dmem_ctrl_isnt.io.cpu2mc_mem_LS_halfword    := ex2mem_data_ram_ld_hword
        dmem_ctrl_isnt.io.cpu2mc_mem_LW_unsigned    := ex2mem_data_ram_ld_unsign

        // == exception == //
        val mem_exception = ex2mem_illegal_instr_exception | ex2mem_instr_addr_misalign_exception |
          dmem_ctrl_isnt.io.load_addr_misalign | dmem_ctrl_isnt.io.store_addr_misalign

        // =========================
        // Mem/WB stage
        // =========================

        // control signal
        val mem2wb_instr_valid  = RegNextWhen(mem_instr_valid, wb_pipe_run) init False
        val mem2wb_rd_wr        = RegNextWhen(ex2mem_rd_wr       & mem_instr_valid, wb_pipe_run) init False
        val mem2wb_data_ram_rd  = RegNextWhen(ex2mem_data_ram_rd & mem_instr_valid, wb_pipe_run) init False
        val mem2wb_mret         = RegNextWhen(ex2mem_mret        & mem_instr_valid, wb_pipe_run) init False
        val mem2wb_ecall        = RegNextWhen(ex2mem_ecall       & mem_instr_valid, wb_pipe_run) init False
        val mem2wb_ebreak       = RegNextWhen(ex2mem_ebreak      & mem_instr_valid, wb_pipe_run) init False
        // CSR control signal
        val mem2wb_csr_wr = RegNextWhen(ex2mem_csr_wr & mem_instr_valid, wb_pipe_run) init False
        val mem2wb_csr_rd = RegNextWhen(ex2mem_csr_rd & mem_instr_valid, wb_pipe_run) init False
        val mem2wb_csr_rw = RegNextWhen(ex2mem_csr_rw, wb_pipe_run)
        val mem2wb_csr_rs = RegNextWhen(ex2mem_csr_rs, wb_pipe_run)
        val mem2wb_csr_rc = RegNextWhen(ex2mem_csr_rc, wb_pipe_run)
        val mem2wb_csr_sel_imm = RegNextWhen(ex2mem_csr_sel_imm, wb_pipe_run)
        val mem2wb_csr_idx = RegNextWhen(ex2mem_csr_idx, wb_pipe_run)

        // data signal
        val mem2wb_rs1_idx   = RegNextWhen(ex2mem_rs1_idx, wb_pipe_run)
        val mem2wb_rs1_value = RegNextWhen(ex2mem_rs1_value, wb_pipe_run)
        val mem2wb_alu_out  = RegNextWhen(ex2mem_alu_out, wb_pipe_run)
        val mem2wb_rd_idx   = RegNextWhen(ex2mem_rd_idx, wb_pipe_run)
        val mem2wb_pc       = RegNextWhen(ex2mem_pc, wb_pipe_run)
        val mem2wb_instr    = RegNextWhen(ex2mem_instr, wb_pipe_run)
        val mem2wb_dmem_addr = RegNextWhen(dmem_addr, wb_pipe_run)

        // Exception
        val mem2wb_illegal_instr_exception = RegNextWhen(ex2mem_illegal_instr_exception & mem_instr_valid, wb_pipe_run) init False
        val mem2wb_instr_addr_misalign_exception = RegNextWhen(ex2mem_instr_addr_misalign_exception & mem_instr_valid, mem_pipe_run) init False
        val mem2wb_load_addr_misalign = RegNextWhen(dmem_ctrl_isnt.io.load_addr_misalign & mem_instr_valid, mem_pipe_run) init False
        val mem2wb_store_addr_misalign = RegNextWhen(dmem_ctrl_isnt.io.store_addr_misalign & mem_instr_valid, mem_pipe_run) init False

        // =========================
        // WB stage
        // =========================

        // instantiate module first
        val mcsr_inst       = mcsr(param)  // mcsr with hart 0
        val trap_ctrl_inst  = trap_ctrl(param)

        // == trap controller wire connection == //
        trap_ctrl_inst.io.external_interrupt            := io.external_interrupt
        trap_ctrl_inst.io.timer_interrupt               := io.timer_interrupt
        trap_ctrl_inst.io.software_interrupt            := io.software_interrupt
        trap_ctrl_inst.io.debug_interrupt               := io.debug_interrupt
        trap_ctrl_inst.io.illegal_instr_exception       := mem2wb_illegal_instr_exception
        trap_ctrl_inst.io.instr_addr_misalign_exception := mem2wb_instr_addr_misalign_exception
        trap_ctrl_inst.io.load_addr_misalign            := mem2wb_load_addr_misalign
        trap_ctrl_inst.io.store_addr_misalign           := mem2wb_store_addr_misalign
        trap_ctrl_inst.io.mret                          := mem2wb_mret
        trap_ctrl_inst.io.ecall                         := mem2wb_ecall
        trap_ctrl_inst.io.wb_pc                         := mem2wb_pc
        trap_ctrl_inst.io.wb_instr                      := mem2wb_instr
        trap_ctrl_inst.io.wb_dmem_addr                  := mem2wb_dmem_addr
        trap_ctrl_inst.io.mtvec                         := mcsr_inst.io.mtvec
        trap_ctrl_inst.io.mepc                          := mcsr_inst.io.mepc
        trap_ctrl_inst.io.mie_meie                      := mcsr_inst.io.mie_meie
        trap_ctrl_inst.io.mie_mtie                      := mcsr_inst.io.mie_mtie
        trap_ctrl_inst.io.mie_msie                      := mcsr_inst.io.mie_msie
        trap_ctrl_inst.io.mstatus_mie                   := mcsr_inst.io.mstatus_mie

        pc_inst.io.trap                                 := trap_ctrl_inst.io.pc_trap
        pc_inst.io.trap_pc_in                           := trap_ctrl_inst.io.pc_value

        // == mcsr wire connection == //
        mcsr_inst.io.mcsr_addr := mem2wb_csr_idx
        // Note: uimm is the same field as rs1 in instruction so use rs1 here instead
        val mcsr_data          = Mux(mem2wb_csr_sel_imm, mem2wb_rs1_idx.asBits.resized, mem2wb_rs1_value)
        val mcsr_masked_set    = mcsr_inst.io.mcsr_dout | mcsr_data
        val mcsr_masked_clear  = mcsr_inst.io.mcsr_dout & ~mcsr_data
        mcsr_inst.io.mcsr_din  := Mux(mem2wb_csr_rw, mcsr_data, Mux(mem2wb_csr_rs, mcsr_masked_set, mcsr_masked_clear))
        mcsr_inst.io.mcsr_wen  := mem2wb_csr_wr
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

        // == Write back to register == //
        regfile_inst.io.register_wr := mem2wb_rd_wr & wb_instr_valid
        regfile_inst.io.register_wr_addr := mem2wb_rd_idx
        // Select data between memory output and alu output
        val from_csr = mem2wb_csr_rw | mem2wb_csr_rs | mem2wb_csr_rc
        val wb_rd_wr_data = Mux(mem2wb_data_ram_rd, dmem_ctrl_isnt.io.mc2cpu_data,
            Mux(from_csr, mcsr_inst.io.mcsr_dout, mem2wb_alu_out))
        regfile_inst.io.rd_in := wb_rd_wr_data

        // == exception == //
        val wb_exception = mem2wb_illegal_instr_exception | mem2wb_instr_addr_misalign_exception |
          mem2wb_load_addr_misalign | mem2wb_store_addr_misalign

        //////////////////////////////////////////////////
        //         Other Components                     //
        //////////////////////////////////////////////////

        // =========================
        // FU - Forwarding Unit
        // =========================

        val fu_inst = fu(param)

        fu_inst.io.ex_rs1_idx := id2ex_rs1_idx
        fu_inst.io.ex_rs2_idx := id2ex_rs2_idx
        fu_inst.io.mem_rd_idx := ex2mem_rd_idx
        fu_inst.io.wb_rd_idx  := mem2wb_rd_idx
        fu_inst.io.ex_rs1_rd  := id2ex_rs1_rd
        fu_inst.io.ex_rs2_rd  := id2ex_rs2_rd
        fu_inst.io.mem_rd_wr  := ex2mem_rd_wr
        fu_inst.io.wb_rd_wr   := mem2wb_rd_wr & wb_instr_valid

        ex_rs1_value_forwarded := Mux(fu_inst.io.forward_rs1_from_mem, ex2mem_alu_out,
            Mux(fu_inst.io.forward_rs1_from_wb, wb_rd_wr_data, id2ex_rs1_value))
        ex_rs2_value_forwarded := Mux(fu_inst.io.forward_rs2_from_mem, ex2mem_alu_out,
            Mux(fu_inst.io.forward_rs2_from_wb, wb_rd_wr_data, id2ex_rs2_value))

        // ===============================
        // HDU - Hazard Detection Unit
        // ===============================

        val hdu_inst = hdu(param)
        hdu_inst.io.branch_taken := branch_taken
        hdu_inst.io.id_rs1_rd    := instr_dec_inst.io.rs1_rd
        hdu_inst.io.id_rs2_rd    := instr_dec_inst.io.rs2_rd
        hdu_inst.io.id_rs1_idx   := instr_dec_inst.io.rs1_idx
        hdu_inst.io.id_rs2_idx   := instr_dec_inst.io.rs2_idx
        hdu_inst.io.ex_dmem_rd   := id2ex_data_ram_rd
        hdu_inst.io.ex_rd_idx    := id2ex_rd_idx
        hdu_inst.io.mem_rd_idx   := ex2mem_rd_idx
        hdu_inst.io.ex_csr_rd    := id2ex_csr_rd
        hdu_inst.io.mem_csr_rd   := ex2mem_csr_rd
        hdu_inst.io.id_exception := id_exception
        hdu_inst.io.ex_exception := ex_exception
        hdu_inst.io.mem_exception := mem_exception
        hdu_inst.io.wb_exception := wb_exception
        hdu_inst.io.ex_mret      := id2ex_mret
        hdu_inst.io.mem_mret     := ex2mem_mret
        hdu_inst.io.wb_mret      := mem2wb_mret
        hdu_inst.io.ex_ecall     := id2ex_mret
        hdu_inst.io.mem_ecall    := ex2mem_mret
        hdu_inst.io.wb_ecall     := mem2wb_mret
        hdu_inst.io.ex_ebreak    := id2ex_mret
        hdu_inst.io.mem_ebreak   := ex2mem_mret
        hdu_inst.io.wb_ebreak    := mem2wb_mret
        hdu_inst.io.int_flush    := trap_ctrl_inst.io.int_flush

        // ===============================
        // Flushing/stalling logic
        // ===============================

        // valid signal is ANDing the valid signal from HDU and the valid signal from previous pipe stage
        if_instr_valid  := hdu_inst.io.if_valid & ~branch_unit_inst.io.instr_addr_misalign_exception
        id_instr_valid  := if2id_instr_valid  & hdu_inst.io.id_valid & ~instr_dec_inst.io.invld_instr
        ex_instr_valid  := id2ex_instr_valid  & hdu_inst.io.ex_valid
        mem_instr_valid := ex2mem_instr_valid & hdu_inst.io.mem_valid
        wb_instr_valid  := mem2wb_instr_valid & hdu_inst.io.wb_valid

        // Place holder for the stall/run signal
        // The stage prefix indicate if we need to stall the pipeline entering this stage
        // For example, id_pipe_stall will stall if2id pipeline stage

        if_pipe_stall  :=  hdu_inst.io.if_pipe_stall
        id_pipe_stall  :=  hdu_inst.io.id_pipe_stall
        ex_pipe_stall  :=  hdu_inst.io.ex_pipe_stall
        mem_pipe_stall :=  hdu_inst.io.mem_pipe_stall
        wb_pipe_stall  :=  hdu_inst.io.wb_pipe_stall
    }
}