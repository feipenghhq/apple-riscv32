///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: InstrINFO
//
// Author: Heqing Huang
// Date Created: 04/30/2021
//
// ================== Description ==================
//
// Contains information for RISCV Instruction. Used by the Instruction Decoder
//
///////////////////////////////////////////////////////////////////////////////////////////////////


package AppleRISCV

import scala.collection.mutable


// ==========================================
// Defining operation for each Instruction
// ==========================================

/** Action Trait defies different action for each instruction */
trait Action
object READ_RS1   extends Action
object READ_RS2   extends Action
object WRITE_RD   extends Action
object SELECT_IMM extends Action
object WRITE_DMEM extends Action
object READ_DMEM  extends Action
object BRANCH     extends Action
object AJAL        extends Action
object AJALR       extends Action


case class ActionMap() {
  val act = mutable.Map[Action, Boolean] (
    READ_RS1   -> false,
    READ_RS2   -> false,
    WRITE_RD   -> false,
    SELECT_IMM -> false,
    WRITE_DMEM -> false,
    READ_DMEM  -> false,
    BRANCH     -> false,
    AJAL        -> false,
    AJALR       -> false
  )
}

class ACT_B_TYPE extends ActionMap {
  act.update(READ_RS1,true)
  act.update(READ_RS2,true)
  act.update(SELECT_IMM,true)
  act.update(BRANCH,true)
}

class ACT_U_TYPE extends ActionMap {
  act.update(WRITE_RD,true)
  act.update(SELECT_IMM,true)
}

/** Basic Instruction Trait defines information for each instruction category */
trait Instruction {
  val opcode   : Int
  val f3       : Int = 0
  val f7       : Int = 0
  val action   : ActionMap
  val aluOp    = ALUCtrlEnum.NOP
  val branchOp = BranchCtrlEnum.NONE
  val immSel   = ImmCtrlEnum.I
}

/** Basic Instruction Trait defines information for Logic/Arithmetic R type Instruction */
trait LARInstr extends Instruction {
  override val opcode = Integer.parseInt("0110011", 2)
  override val f7 = 0
  override val action = ActionMap()
  action.act.update(READ_RS1,true)
  action.act.update(READ_RS2,true)
  action.act.update(WRITE_RD,true)
}

/** Basic Instruction Trait defines information for Logic/Arithmetic I type Instruction */
trait LAIInstr extends Instruction {
  override val opcode = Integer.parseInt("0010011", 2)
  override val action = ActionMap()
  action.act.update(READ_RS1,true)
  action.act.update(WRITE_RD,true)
  action.act.update(SELECT_IMM,true)
  action.act.update(READ_DMEM,true)
  override val immSel = ImmCtrlEnum.I
}

/** Basic Instruction Trait defines information for S type Instruction */
trait SDInstr extends Instruction {
  override val opcode = Integer.parseInt("0100011", 2)
  override val action = ActionMap()
  action.act.update(READ_RS1,true)
  action.act.update(READ_RS2,true)
  action.act.update(SELECT_IMM,true)
  action.act.update(WRITE_DMEM,true)
  override val aluOp = ALUCtrlEnum.ADD // Use ADD to calculate address
  override val immSel = ImmCtrlEnum.S
}

/**
 * Basic Instruction Trait defines information for LOAD Instruction
 */
trait LDInstr extends Instruction {
  override val opcode = Integer.parseInt("0000011", 2)
  override val action = ActionMap()
  action.act.update(READ_RS1,true)
  action.act.update(SELECT_IMM,true)
  action.act.update(WRITE_DMEM,true)
  override val aluOp = ALUCtrlEnum.ADD // Use ADD to calculate address
  override val immSel = ImmCtrlEnum.I
}

/**
 * Basic Instruction Trait defines information for Branch Instruction
 */
trait BRInstr extends Instruction {
  override val opcode = Integer.parseInt("1100011", 2)
  override val action = ActionMap()
  action.act.update(READ_RS1,true)
  action.act.update(READ_RS2,true)
  action.act.update(SELECT_IMM,true)
  action.act.update(BRANCH,true)
  override val immSel = ImmCtrlEnum.B
}

/**
 * Basic Instruction Trait defines information for JALR Instruction
 */
trait JALRInstr extends Instruction {
  override val opcode = Integer.parseInt("1100111", 2)
  override val action = ActionMap()
  action.act.update(READ_RS1,true)
  action.act.update(WRITE_RD,true)
  action.act.update(SELECT_IMM,true)
  action.act.update(AJALR,true)
  override val immSel = ImmCtrlEnum.I
}


/**
 * Basic Instruction Trait defines information for JAL Instruction
 */
trait JALInstr extends Instruction {
  override val opcode = Integer.parseInt("1101111", 2)
  override val action = ActionMap()
  action.act.update(WRITE_RD,true)
  action.act.update(SELECT_IMM,true)
  action.act.update(AJAL,true)
  override val immSel = ImmCtrlEnum.J
}

/**
 * Basic Instruction Trait defines information for AUIPC Instruction
 */
trait AUIPCInstr extends Instruction {
  override val opcode = Integer.parseInt("0010111", 2)
  override val action = ActionMap()
  action.act.update(WRITE_RD,true)
  action.act.update(SELECT_IMM,true)
}

/**
 * Basic Instruction Trait defines information for LUI Instruction
 */
trait LUIInstr extends Instruction {
  override val opcode = Integer.parseInt("0110111", 2)
  override val action = ActionMap()
  action.act.update(WRITE_RD,true)
  action.act.update(SELECT_IMM,true)
}


// ==========================================
// Use MaskedLiteral for Instruction Decode
// ==========================================

object LUI   extends LUIInstr
object AUIPC extends AUIPCInstr
object JAL   extends JALInstr
object JALR  extends JALRInstr

// B - Type
object BEQ  extends BRInstr {override val f3 = 0}
object BNE  extends BRInstr {override val f3 = 1}
object BLT  extends BRInstr {override val f3 = 4}
object BGE  extends BRInstr {override val f3 = 5}
object BLTU extends BRInstr {override val f3 = 6}
object BGEU extends BRInstr {override val f3 = 7}

// I Type - Load
object LB  extends LDInstr {override val f3 = 0}
object LH  extends LDInstr {override val f3 = 1}
object LW  extends LDInstr {override val f3 = 2}
object LBU extends LDInstr {override val f3 = 3}
object LHU extends LDInstr {override val f3 = 4}

// S Type
object SB extends SDInstr {override val f3 = 0}
object SH extends SDInstr {override val f3 = 1}
object SW extends SDInstr {override val f3 = 2}

// I Type
object ADDI  extends LAIInstr {
  override val f3 = 0
  override val aluOp = ALUCtrlEnum.ADD
}
object SLTI  extends LAIInstr {
  override val f3 = 2
  override val aluOp = ALUCtrlEnum.SLT
}
object SLTIU extends LAIInstr {
  override val f3 = 3
  override val aluOp = ALUCtrlEnum.SLTU
}
object XORI extends LAIInstr {
  override val f3 = 4
  override val aluOp = ALUCtrlEnum.XOR
}
object ORI extends LAIInstr {
  override val f3 = 6
  override val aluOp = ALUCtrlEnum.OR
}
object ANDI extends LAIInstr {
  override val f3 = 7
  override val aluOp = ALUCtrlEnum.AND
}
object SLLI extends LAIInstr {
  override val f3 = 1
  override val aluOp = ALUCtrlEnum.SLL
}
object SRLI extends LAIInstr {
  override val f3 = 5
  override val aluOp = ALUCtrlEnum.SRL
}
object SRAI extends LAIInstr {
  override val f3 = 5
  override val f7 = Integer.parseInt("0100000", 2)
  override val aluOp = ALUCtrlEnum.SRA
}

// R Type
object ADD extends LARInstr {
  override val f3 = 0
  override val aluOp = ALUCtrlEnum.ADD
}
object SUB extends LARInstr {
  override val f3 = 0
  override val f7 = Integer.parseInt("0100000", 2)
  override val aluOp = ALUCtrlEnum.SUB
}
object SLL extends LARInstr {
  override val f3 = 1
  override val aluOp = ALUCtrlEnum.SLL
}
object SLT extends LARInstr {
  override val f3 = 2
  override val aluOp = ALUCtrlEnum.SLT
}
object SLTU extends LARInstr {
  override val f3 = 3
  override val aluOp = ALUCtrlEnum.SLTU
}
object XOR extends LARInstr {
  override val f3 = 4
  override val aluOp = ALUCtrlEnum.XOR
}

object SRL extends LARInstr {
  override val f3 = 5
  override val aluOp = ALUCtrlEnum.SRL
}

object SRA extends LARInstr {
  override val f3 = 5
  override val f7 = Integer.parseInt("0100000", 2)
  override val aluOp = ALUCtrlEnum.SRA
}

object OR extends LARInstr {
  override val f3 = 6
  override val aluOp = ALUCtrlEnum.OR
}

object AND extends LARInstr {
  override val f3 = 7
  override val aluOp = ALUCtrlEnum.AND
}

/*
object FENCE  extends Instruction {val category = InstrCatEnum.FEN}
object ECALL  extends Instruction {val category = InstrCatEnum.SYS}
object EBREAK extends Instruction {val category = InstrCatEnum.SYS}
*/

object InstrCodeMapList {
  val instrTypeMap = Map[Instruction, Array[Instruction]](
    LUI   -> Array[Instruction](LUI),
    AUIPC -> Array[Instruction](AUIPC),
    JAL   -> Array[Instruction](JAL),
    JALR  -> Array[Instruction](JALR),
    ADD   -> Array[Instruction](ADD, SUB, SLL, SLT, SLTU, XOR, SRL, SRA, OR, AND),
    ADDI  -> Array[Instruction](ADDI, SLTI, SLTIU, XORI, ORI, ANDI, SLLI, SRLI, SRAI),
    SB    -> Array[Instruction](SB, SH, SW),
    LB    -> Array[Instruction](LB, LH, LW, LBU, LHU),
    BEQ   -> Array[Instruction](BEQ, BNE, BLT, BGE, BLTU, BGEU)
  )
  def apply() ={
    instrTypeMap
  }
}