///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: CPU_AppleRISCVCfg
//
// Author: Heqing Huang
// Date Created: 03/27/2021
//
// ================== Description ==================
//
// Define basic parameter for the CPU
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._

/** Defines for Instruction decode */
object InstrDefine {

  // ===================
  // opcode
  // ===================
  // RV32I Basic Instruction
  val OP_LOGIC_ARITH      = Integer.parseInt("0110011", 2) // Logic and arithmetic operation
  val OP_LOGIC_ARITH_IMM  = Integer.parseInt("0010011", 2) // Logic and arithmetic with immediate number
  val OP_MEM_LOAD         = Integer.parseInt("0000011", 2) // Load instruction
  val OP_MEM_STORE        = Integer.parseInt("0100011", 2) // Store instruction
  val OP_BRANCH           = Integer.parseInt("1100011", 2) // Branch and jump instruction
  val OP_AUIPC            = Integer.parseInt("0010111", 2) // AUIPC instruction
  val OP_LUI              = Integer.parseInt("0110111", 2) // LUI instruction
  val OP_JAL              = Integer.parseInt("1101111", 2) // JAL
  val OP_JALR             = Integer.parseInt("1100111", 2) // JALR
  // RV32 Extension
  val OP_EXT_FEANCE       = Integer.parseInt("0001111", 2) // FANCE
  val OP_SYS              = Integer.parseInt("1110011", 2) // System Instruction: Zicsr, Trap RET

  // val OP_RV32M            = Integer.parseInt("0110011", 2) // RV32M Standard Extension
  // => Same as Logic and arithmetic

  // ===================
  // func3 fiedl
  // ===================
  // Logic arithmetic func3 field
  val LA_F3_AND  = Integer.parseInt("111", 2) // AND
  val LA_F3_OR   = Integer.parseInt("110", 2) // OR
  val LA_F3_XOR  = Integer.parseInt("100", 2) // XOR
  val LA_F3_ADD  = Integer.parseInt("000", 2) // ADD
  val LA_F3_SUB  = Integer.parseInt("000", 2) // ADD
  val LA_F3_SR   = Integer.parseInt("101", 2) // SRL, SRLI, SRA, SRAI
  val LA_F3_SLL  = Integer.parseInt("001", 2) // SLL, SLLI
  val LA_F3_SLT  = Integer.parseInt("010", 2) // SLT, SLTI
  val LA_F3_SLTU = Integer.parseInt("011", 2) // SLTU, SLTIU
  // Load/Store func3 field
  val LW_F3_LB  = Integer.parseInt("000", 2) // LB
  val LW_F3_LH  = Integer.parseInt("001", 2) // LH
  val LW_F3_LW  = Integer.parseInt("010", 2) // LW, SW
  val LW_F3_LBU = Integer.parseInt("100", 2) // LBU
  val LW_F3_LHU = Integer.parseInt("101", 2) // LHU
  val SW_F3_SB  = Integer.parseInt("000", 2) // SB
  val SW_F3_SH  = Integer.parseInt("001", 2) // SH
  val SW_F3_SW  = Integer.parseInt("010", 2) // SW
  // Branch func3 field
  val BR_F3_BEQ  = Integer.parseInt("000", 2) // BEQ
  val BR_F3_BNE  = Integer.parseInt("001", 2) // BNE
  val BR_F3_BLT  = Integer.parseInt("100", 2) // BLT
  val BR_F3_BGE  = Integer.parseInt("101", 2) // BGE
  val BR_F3_BLTU = Integer.parseInt("110", 2) // BLTU
  val BR_F3_BGEU = Integer.parseInt("111", 2) // BGEU
  // Fence func3 field
  val FE_F3_FENCE  = Integer.parseInt("000", 2) // FENCE
  val FE_F3_FENCEI = Integer.parseInt("001", 2) // FENCE.I
  // ZICSR func3 field
  val CSR_F3_RW  = Integer.parseInt("001", 2) // CSRRW
  val CSR_F3_RS  = Integer.parseInt("010", 2) // CSRRS
  val CSR_F3_RC  = Integer.parseInt("011", 2) // CSRRC
  val CSR_F3_RWI = Integer.parseInt("101", 2) // CSRRW
  val CSR_F3_RSI = Integer.parseInt("110", 2) // CSRRS
  val CSR_F3_RCI = Integer.parseInt("111", 2) // CSRRC
  // Pirvileged Instruction
  val SYS_F3_PRIV = Integer.parseInt("000", 2)
  // RV32M Standard Extension
  val RV32M_MUL    = Integer.parseInt("000", 2)
  val RV32M_MULH   = Integer.parseInt("001", 2)
  val RV32M_MULHSU = Integer.parseInt("010", 2)
  val RV32M_MULHU  = Integer.parseInt("011", 2)
  val RV32M_DIV    = Integer.parseInt("100", 2)
  val RV32M_DIVU   = Integer.parseInt("101", 2)
  val RV32M_REM    = Integer.parseInt("110", 2)
  val RV32M_REMU   = Integer.parseInt("111", 2)

  // ===================
  // func7 field
  // ===================
  // Logic arithmetic func7 field
  val LA_F7_SRL = Integer.parseInt("0000000", 2) // SRL, SRLT
  val LA_F7_SRA = Integer.parseInt("0100000", 2) // SRA, SRAT
  val LA_F7_ADD = Integer.parseInt("0000000", 2) // ADD
  val LA_F7_SUB = Integer.parseInt("0100000", 2) // SUB

  // ===================
  // func12 field
  // ===================
  val F12_MRET   = Integer.parseInt("001100000010", 2) // MRET
  val F12_ECALL  = Integer.parseInt("000000000000", 2) // ECALL
  val F12_EBREAK = Integer.parseInt("000000000001", 2) // EBREAK
}

// ==========================================
// Enum type for different control signal
// ==========================================
object AluOpcodeEnum extends SpinalEnum(binaryOneHot) {
  val NOP, ADD, SUB, AND, OR, XOR, SRA, SRL, SLL, SLT, SLTU, PC4 = newElement()
}

object BranchOpcodeEnum extends SpinalEnum(){
  val NOP, BEQ, BNE, BLT, BGE, BLTU, BGEU = newElement()
}

object CsrOpcodeEnum extends SpinalEnum(){
  val RD, WR, RW, RS, RC, BGEU = newElement()
}

object MulOpcodeEnum extends SpinalEnum(){
  val MUL, MULH, MULHSU, MULHU = newElement()
}

object DivOpcodeEnum extends SpinalEnum(){
  val DIV, DIVU, REM, REMU = newElement()
}

// IMPORTANT: We Need to use binaryOneHot encoding here
// If we use natural encoding (0, 1, 2, ...), there is a bug in the Vivado.
// It put a not used value for DIV instruction hence create a wrong rd wdata for DIV instruction.
// Using one-hot encoding will fix this issue.
object RdSelEnum extends SpinalEnum(binaryOneHot){
  val MEM, CSR, ALU = newElement()
  val MUL, DIV = if (AppleRISCVCfg.USE_RV32M) newElement() else null
}

object CsrSelEnum extends SpinalEnum(){
  val DATA, SET, CLEAR = newElement()
}

// ==========================================
// Exception Code
// ==========================================
object ExcCode {
  val EXC_CODE_INSTR_ADDR_MA  = 0
  val EXC_CODE_ILL_INSTR      = 2
  val EXC_CODE_LD_ADDR_MA     = 4
  val EXC_CODE_SD_ADDR_MA     = 6
  val EXC_CODE_MECALL         = 11

  val EXC_CODE_M_SW_INT       = 3
  val EXC_CODE_M_TIMER_INT    = 7
  val EXC_CODE_M_EXT_INT      = 11
}
