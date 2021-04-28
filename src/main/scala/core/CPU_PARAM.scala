///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: CPU_PARAM
//
// Author: Heqing Huang
// Date Created: 03/27/2021
//
// ================== Description ==================
//
// Define basic parameters for the CPU
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package core

import bus.sib._
import spinal.core._
import spinal.lib.bus.amba3.ahblite._

class CPU_PARAM {

    val NAME = "apple-riscv32"

    //////////////////////////////////////////////////////////////////////////////////
    //                              CPU Core Parameter                              //
    //////////////////////////////////////////////////////////////////////////////////

    // ========================== //
    //  CPU config and Parameter  //
    // ========================== //
    val XLEN       = 32
    val DATA_WIDTH = XLEN
    val PC_WIDTH   = DATA_WIDTH
    val RF_WIDTH   = DATA_WIDTH                 // Register File width
    val RF_SIZE    = 32                         // Register File size
    val RF_ADDR_WDITH   = log2Up(RF_SIZE)       // Register File address width

    // CSR related
    val MXLEN       = XLEN                      // MCSR register length
    val CSR_ADDR_WIDTH = 12

    // ========================== //
    //       SIB Bus Config       //
    // ========================== //
    val sibCfg = SibConfig(
        addressWidth = XLEN,
        dataWidth    = XLEN
    )

    //////////////////////////////////////////////////////////////////////////////////
    //                           Constant Definition                                //
    //////////////////////////////////////////////////////////////////////////////////

    // ========================== //
    //     Instruction Decode     //
    // ========================== //

    // == opcode == //
    // TODO: Potential optimization. Remove the "11" bits
    // RV32I Basic Instruction
    val OP_LOGIC_ARITH      = Integer.parseInt("0110011", 2)        // Logic and arithmetic operation
    val OP_LOGIC_ARITH_IMM  = Integer.parseInt("0010011", 2)        // Logic and arithmetic with immediate number
    val OP_MEM_LOAD         = Integer.parseInt("0000011", 2)        // Load instruction
    val OP_MEM_STORE        = Integer.parseInt("0100011", 2)        // Store instruction
    val OP_BRANCH           = Integer.parseInt("1100011", 2)        // Branch and jump instruction
    val OP_AUIPC            = Integer.parseInt("0010111", 2)        // AUIPC instruction
    val OP_LUI              = Integer.parseInt("0110111", 2)        // LUI instruction
    val OP_JAL              = Integer.parseInt("1101111", 2)        // JAL
    val OP_JALR             = Integer.parseInt("1100111", 2)        // JALR
    // RV32 Extension
    val OP_EXT_FEANCE       = Integer.parseInt("0001111", 2)        // FANCE
    val OP_EXT_SYS          = Integer.parseInt("1110011", 2)        // System Instruction: Zicsr, Trap RET

    // == func3 == //
    // Logic arithmetic func3 field
    val LA_F3_AND       = Integer.parseInt("111", 2) // AND
    val LA_F3_OR        = Integer.parseInt("110", 2) // OR
    val LA_F3_XOR       = Integer.parseInt("100", 2) // XOR
    val LA_F3_ADD_SUB   = Integer.parseInt("000", 2) // ADD, SUB
    val LA_F3_SR        = Integer.parseInt("101", 2) // SRL, SRLI, SRA, SRAI
    val LA_F3_SLL       = Integer.parseInt("001", 2) // SLL, SLLI
    val LA_F3_SLT       = Integer.parseInt("010", 2) // SLT, SLTI
    val LA_F3_SLTU      = Integer.parseInt("011", 2) // SLTU, SLTIU
    // Load/Store func3 field
    val LW_F3_LB    = Integer.parseInt("000", 2) // LB
    val LW_F3_LH    = Integer.parseInt("001", 2) // LH
    val LW_F3_LW    = Integer.parseInt("010", 2) // LW, SW
    val LW_F3_LBU   = Integer.parseInt("100", 2) // LBU
    val LW_F3_LHU   = Integer.parseInt("101", 2) // LHU
    val SW_F3_SB    = Integer.parseInt("000", 2) // SB
    val SW_F3_SH    = Integer.parseInt("001", 2) // SH
    val SW_F3_SW    = Integer.parseInt("010", 2) // SW
    // Branch func3 field
    val BR_F3_BEQ   = Integer.parseInt("000", 2) // BEQ
    val BR_F3_BNE   = Integer.parseInt("001", 2) // BNE
    val BR_F3_BLT   = Integer.parseInt("100", 2) // BLT
    val BR_F3_BGE   = Integer.parseInt("101", 2) // BGE
    val BR_F3_BLTU  = Integer.parseInt("110", 2) // BLTU
    val BR_F3_BGEU  = Integer.parseInt("111", 2) // BGEU
    // Fence func3 field
    val FE_F3_FENCE  =  Integer.parseInt("000", 2) // FENCE
    val FE_F3_FENCEI =  Integer.parseInt("001", 2) // FENCE.I
    // ZICSR func3 field
    val CSR_F3_RW   = Integer.parseInt("001", 2)    // CSRRW
    val CSR_F3_RS   = Integer.parseInt("010", 2)    // CSRRS
    val CSR_F3_RC   = Integer.parseInt("011", 2)    // CSRRC
    val CSR_F3_RWI  = Integer.parseInt("101", 2)    // CSRRW
    val CSR_F3_RSI  = Integer.parseInt("110", 2)    // CSRRS
    val CSR_F3_RCI  = Integer.parseInt("111", 2)    // CSRRC
    // Pirvileged Instruction
    val SYS_F3_PRIV = Integer.parseInt("000", 2)

    // == func7 == //
    // Logic arithmetic func7 field
    val LA_F7_SRL   = Integer.parseInt("0000000", 2) // SRL, SRLT
    val LA_F7_SRA   = Integer.parseInt("0100000", 2) // SRA, SRAT
    val LA_F7_ADD   = Integer.parseInt("0000000", 2) // ADD
    val LA_F7_SUB   = Integer.parseInt("0100000", 2) // SUB

    // == func12 == //
    val F12_MRET    = Integer.parseInt("001100000010", 2) // MRET
    val F12_ECALL   = Integer.parseInt("000000000000", 2) // ECALL
    val F12_EBREAK  = Integer.parseInt("000000000001", 2) // EBREAK

    // ========================== //
    //        Macro define        //
    // ========================== //

    // == Exception Code == //
    val EXCEP_CODE_instr_addr_misalign  = 0
    val EXCEP_CODE_illegal_instr        = 2
    val EXCEP_CODE_load_addr_misalign   = 4
    val EXCEP_CODE_store_addr_misalign  = 6
    val EXCEP_CODE_mecall               = 11

    val EXCEP_CODE_M_software_interrupt = 3
    val EXCEP_CODE_M_timer_interrupt    = 7
    val EXCEP_CODE_M_external_interrupt = 11     
}