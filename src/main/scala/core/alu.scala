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

package core

import spinal.core._

case class alu_io(param: CPU_PARAM) extends Bundle {

    // data input/output
    val operand_1 = in Bits(param.DATA_WIDTH bits)
    val operand_2 = in Bits(param.DATA_WIDTH bits)
    val alu_out = out Bits(param.DATA_WIDTH bits)

    // ctrl input/output
    val alu_op_and  = in Bool
    val alu_op_or   = in Bool
    val alu_op_xor  = in Bool
    val alu_op_add  = in Bool
    val alu_op_sub  = in Bool
    val alu_op_sra  = in Bool
    val alu_op_srl  = in Bool
    val alu_op_sll  = in Bool
    val alu_op_slt  = in Bool
    val alu_op_sltu = in Bool
    val alu_op_eqt  = in Bool
    val alu_op_invb0 = in Bool
}

case class alu(param: CPU_PARAM) extends Component {

    noIoPrefix()

    val io = new alu_io(param)

    // Preprocess some value
    val op1_signed = io.operand_1.asSInt
    val op2_signed = io.operand_2.asSInt
    val op1_unsigned = io.operand_1.asUInt
    val op2_unsigned = io.operand_2.asUInt
    // Notes: the shift value is in the same field of operand_2 for both R-type and I-type
    // For R-type, it's bit [4:0] of register rs2 and rs2 is operand_2.
    // For I-type, it's bit [4:0] of the immediate value and the immediate value is chosen as operand_2.
    val shift_value = op2_unsigned(4 downto 0)

    // Calculation Result
    val add_result = op1_signed + op2_signed
    val sub_result = op1_signed - op2_signed
    val and_result = io.operand_1 & io.operand_2
    val or_result  = io.operand_1 | io.operand_2
    val xor_result = io.operand_1 ^ io.operand_2
    val sra_result_tmp = op1_signed >> shift_value // Arithmetic right shift
    val sra_result = sra_result_tmp.asBits
    val srl_result = io.operand_1 |>> shift_value
    val sll_result = io.operand_1 |<< shift_value
    val slt_result = Bits(param.DATA_WIDTH bits)
    slt_result(param.DATA_WIDTH - 1 downto 1) := 0
    slt_result(0) := (op1_signed < op2_signed) ^ io.alu_op_invb0
    val sltu_result = Bits(param.DATA_WIDTH bits)
    sltu_result(param.DATA_WIDTH - 1 downto 1) := 0
    sltu_result(0) := (op1_unsigned < op2_unsigned) ^ io.alu_op_invb0
    val eqt_result = Bits(param.DATA_WIDTH bits)
    eqt_result(param.DATA_WIDTH - 1 downto 1) := 0
    eqt_result(0) := (io.operand_1 === io.operand_2) ^ io.alu_op_invb0

    // Big MUX for alu output
    val alu_op_and_mask  = Bits(param.DATA_WIDTH bits)
    val alu_op_or_mask   = Bits(param.DATA_WIDTH bits)
    val alu_op_xor_mask  = Bits(param.DATA_WIDTH bits)
    val alu_op_add_mask  = Bits(param.DATA_WIDTH bits)
    val alu_op_sub_mask  = Bits(param.DATA_WIDTH bits)
    val alu_op_sra_mask  = Bits(param.DATA_WIDTH bits)
    val alu_op_srl_mask  = Bits(param.DATA_WIDTH bits)
    val alu_op_sll_mask  = Bits(param.DATA_WIDTH bits)
    val alu_op_slt_mask  = Bits(param.DATA_WIDTH bits)
    val alu_op_sltu_mask = Bits(param.DATA_WIDTH bits)
    val alu_op_eqt_mask  = Bits(param.DATA_WIDTH bits)

    alu_op_and_mask.setAllTo(io.alu_op_and)
    alu_op_or_mask.setAllTo(io.alu_op_or)
    alu_op_xor_mask.setAllTo(io.alu_op_xor)
    alu_op_add_mask.setAllTo(io.alu_op_add)
    alu_op_sub_mask.setAllTo(io.alu_op_sub)
    alu_op_sra_mask.setAllTo(io.alu_op_sra)
    alu_op_srl_mask.setAllTo(io.alu_op_srl)
    alu_op_sll_mask.setAllTo(io.alu_op_sll)
    alu_op_slt_mask.setAllTo(io.alu_op_slt)
    alu_op_sltu_mask.setAllTo(io.alu_op_sltu)
    alu_op_eqt_mask.setAllTo(io.alu_op_eqt)

    val alu_out_1 = alu_op_and_mask & and_result.asBits |
                    alu_op_or_mask  & or_result.asBits |
                    alu_op_xor_mask & xor_result.asBits |
                    alu_op_add_mask & add_result.asBits |
                    alu_op_sub_mask & sub_result.asBits |
                    alu_op_sra_mask & sra_result.asBits

    val alu_out_2 = alu_op_srl_mask & srl_result.asBits |
                    alu_op_sll_mask & sll_result.asBits |
                    alu_op_slt_mask & slt_result.asBits |
                    alu_op_sltu_mask & sltu_result.asBits |
                    alu_op_eqt_mask & eqt_result.asBits

    io.alu_out := alu_out_1 | alu_out_2
}