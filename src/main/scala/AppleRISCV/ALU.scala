///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: ALU
//
// Author: Heqing Huang
// Date Created: 04/30/2021
//
// ================== Description ==================
//
// Arithmetic Logic Unit
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._
import spinal.lib._

case class ALU() extends Component {

  val io = new Bundle {
    val operand1 = in Bits(AppleRISCVCfg.xlen bits)
    val operand2 = in Bits(AppleRISCVCfg.xlen bits)
    val aluCtrl  = slave(AluCtrlStage())
    val aluOut   = out Bits(AppleRISCVCfg.xlen bits)
  }
  noIoPrefix()

  // Preprocess some value
  val op1_signed: SInt = io.operand1.asSInt
  val op2_signed: SInt = io.operand2.asSInt
  val op1_unsigned: UInt = io.operand1.asUInt
  val op2_unsigned: UInt = io.operand2.asUInt
  // Notes: the shift value is in the same field of operand2 for both R-type and I-type
  // For R-type, it's bit [4:0] of register rs2 and rs2 is operand2.
  // For I-type, it's bit [4:0] of the immediate value and the immediate value is chosen as operand2.
  val shift_value: op2_unsigned.type = op2_unsigned(4 downto 0)

  // Calculation Result
  val addResult: SInt = op1_signed + op2_signed
  val subResult: SInt = op1_signed - op2_signed
  val andResult: Bits = io.operand1 & io.operand2
  val orResult : Bits = io.operand1 | io.operand2
  val xorResult: Bits = io.operand1 ^ io.operand2
  val sraResult_tmp: SInt = op1_signed >> shift_value // Arithmetic right shift
  val sraResult: Bits = sraResult_tmp.asBits
  val srlResult: Bits = io.operand1 |>> shift_value
  val sllResult: Bits = io.operand1 |<< shift_value
  val sltResult: Bits = (op1_signed < op2_signed).asBits.resize(AppleRISCVCfg.xlen bits)
  val sltuResult: Bits = (op1_unsigned < op2_unsigned).asBits.resize(AppleRISCVCfg.xlen bits)

  switch(io.aluCtrl.aluOp) {
    is(ALUCtrlEnum.ADD) {io.aluOut := addResult.asBits}
    is(ALUCtrlEnum.SUB) {io.aluOut := subResult.asBits}
    is(ALUCtrlEnum.AND) {io.aluOut := andResult.asBits}
    is(ALUCtrlEnum.OR ) {io.aluOut := orResult.asBits}
    is(ALUCtrlEnum.XOR) {io.aluOut := xorResult.asBits}
    is(ALUCtrlEnum.SRA) {io.aluOut := sraResult.asBits}
    is(ALUCtrlEnum.SRL) {io.aluOut := srlResult.asBits}
    is(ALUCtrlEnum.SLL) {io.aluOut := sllResult.asBits}
    is(ALUCtrlEnum.SLT) {io.aluOut := sltResult.asBits}
    is(ALUCtrlEnum.SLTU) {io.aluOut := sltuResult.asBits}
    default {io.aluOut := addResult.asBits}
  }
}
