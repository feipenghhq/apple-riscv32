///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: alu
//
// Author: Heqing Huang
// Date Created: 03/29/2021
//
// ================== Description ==================
//
// ALU
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._

case class ALUIO() extends Bundle {
    val operand_1 = in Bits(AppleRISCVCfg.XLEN bits)
    val operand_2 = in Bits(AppleRISCVCfg.XLEN bits)
    val pc        = in UInt(AppleRISCVCfg.XLEN bits)
    val alu_opcode = in(AluOpcodeEnum())
    val alu_out = out Bits(AppleRISCVCfg.XLEN bits)

}

case class ALU() extends Component {

    val io = new ALUIO()
    noIoPrefix()

    // Preprocess some value
    val op1_signed = io.operand_1.asSInt
    val op2_signed = io.operand_2.asSInt
    val op1_unsigned = io.operand_1.asUInt
    val op2_unsigned = io.operand_2.asUInt
    // Notes: the shift value is in the same field of operand_2 for both R-type and I-type
    // For R-type, it's bit [4:0] of register rs2 and rs2 is operand_2.
    // For I-type, it's bit [4:0] of the immediate value and the immediate value is chosen as operand_2.
    val shift_value = op2_unsigned(4 downto 0)

    // Calculation result
    val add_result = op1_signed + op2_signed
    val sub_result = op1_signed - op2_signed
    val and_result = io.operand_1 & io.operand_2
    val or_result  = io.operand_1 | io.operand_2
    val xor_result = io.operand_1 ^ io.operand_2
    val sra_result_tmp = op1_signed >> shift_value // Arithmetic right shift
    val sra_result = sra_result_tmp.asBits
    val srl_result = io.operand_1 |>> shift_value
    val sll_result = io.operand_1 |<< shift_value
    val slt_result = (op1_signed < op2_signed).asBits.resize(AppleRISCVCfg.XLEN bits)
    val sltu_result = (op1_unsigned < op2_unsigned).asBits.resize(AppleRISCVCfg.XLEN bits)
    val lui_result = io.operand_2(31 downto 12) ## B"12'h0"
    val auipc_result = io.pc + (io.operand_2(31 downto 12) ## B"12'h0").asUInt
    val pcplus4_result = io.pc + 4

    switch(io.alu_opcode) {
        is(AluOpcodeEnum.ADD) {io.alu_out := add_result.asBits}
        is(AluOpcodeEnum.SUB) {io.alu_out := sub_result.asBits}
        is(AluOpcodeEnum.AND) {io.alu_out := and_result.asBits}
        is(AluOpcodeEnum.OR ) {io.alu_out := or_result.asBits}
        is(AluOpcodeEnum.XOR) {io.alu_out := xor_result.asBits}
        is(AluOpcodeEnum.SRA) {io.alu_out := sra_result.asBits}
        is(AluOpcodeEnum.SRL) {io.alu_out := srl_result.asBits}
        is(AluOpcodeEnum.SLL) {io.alu_out := sll_result.asBits}
        is(AluOpcodeEnum.SLT) {io.alu_out := slt_result.asBits}
        is(AluOpcodeEnum.SLTU) {io.alu_out := sltu_result.asBits}
        is(AluOpcodeEnum.PC4) {io.alu_out := pcplus4_result.asBits}
        default {io.alu_out := and_result.asBits}
    }
}