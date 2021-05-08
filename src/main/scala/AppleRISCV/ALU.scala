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
    // RV32 Multiplier Result
    val ex_stage_valid = in Bool
    val alu_stall_req  = out Bool
    val product_valid  = if (AppleRISCVCfg.RV32M) out Bool else null
    val product        = if (AppleRISCVCfg.RV32M) out Bits(AppleRISCVCfg.XLEN bits) else null

}

case class ALU() extends Component {

    val io = ALUIO()
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
    val pcplus4_result = io.pc + 4

    if (!AppleRISCVCfg.RV32M) io.alu_stall_req := False

    val rv32mArea = if (AppleRISCVCfg.RV32M) new Area {

        // Multiplier using FPGA DSP block => Inferring DSP block
        val multiplier_inst = Multiplier("DSP", 4, AppleRISCVCfg.XLEN+1, AppleRISCVCfg.XLEN+1)

        val is_mul = io.alu_opcode === AluOpcodeEnum.MUL    | io.alu_opcode === AluOpcodeEnum.MULH |
                     io.alu_opcode === AluOpcodeEnum.MULHSU | io.alu_opcode === AluOpcodeEnum.MULHU

        // Process the signed and unsigned bits
        // Note: We always use signed multiplier here to save DSP resource
        // In make unsigned bit looks like signed, we add addition 1 bit to the
        // top as the new signed bit for both signed and unsigned value
        // For signed value, we will add 1, for unsigned value we will add 0
        // Now the multiplier become 33 * 33 bit this is OK for FPGA because
        // FPGA usually has 18 * 18 as DSP block and we will need to use multiple of them anyway.
        val multiplicand = SInt(AppleRISCVCfg.XLEN + 1 bits) // RS1
        when(io.alu_opcode === AluOpcodeEnum.MULHU) {
            multiplicand := (False ## io.operand_1).asSInt
        }.otherwise{
            multiplicand := io.operand_1.asSInt.resized
        }
        val multiplier = SInt(AppleRISCVCfg.XLEN+1 bits) // RS2
        when(io.alu_opcode === AluOpcodeEnum.MULHU || io.alu_opcode === AluOpcodeEnum.MULHSU) {
            multiplier := (False ## io.operand_2).asSInt
        }.otherwise{
            multiplier := io.operand_2.asSInt.resized
        }

        multiplier_inst.io.multiplier   := multiplier
        multiplier_inst.io.multiplicand := multiplicand
        multiplier_inst.io.mul_valid    := is_mul & io.ex_stage_valid

        val is_mul_s   = RegNext(io.alu_opcode === AluOpcodeEnum.MUL);
        val product_lo = multiplier_inst.io.product(AppleRISCVCfg.XLEN-1 downto 0)
        val product_hi = multiplier_inst.io.product(2*AppleRISCVCfg.XLEN-1 downto AppleRISCVCfg.XLEN)

        io.product       := Mux(is_mul_s, product_lo, product_hi).asBits
        io.product_valid := multiplier_inst.io.product_valid // EX/MEM stage pipeline is embedded here
        val mul_stall_req = (multiplier_inst.io.mul_valid & multiplier_inst.io.mul_ready) | (~multiplier_inst.io.mul_ready & ~multiplier_inst.io.product_early_valid)

        // Divider
        val divider_inst = MixedDivider(AppleRISCVCfg.XLEN)
        val is_div = io.alu_opcode === AluOpcodeEnum.DIV    | io.alu_opcode === AluOpcodeEnum.DIVU |
                     io.alu_opcode === AluOpcodeEnum.REM    | io.alu_opcode === AluOpcodeEnum.REMU
        divider_inst.io.div_req  := is_div & io.ex_stage_valid
        divider_inst.io.dividend := io.operand_1
        divider_inst.io.divisor  := io.operand_2
        divider_inst.io.flush    := ~io.ex_stage_valid
        divider_inst.io.signed   := io.alu_opcode === AluOpcodeEnum.DIV | io.alu_opcode === AluOpcodeEnum.REM
        val quotient = divider_inst.io.quotient
        val reminder = divider_inst.io.remainder
        val div_stall_req = (is_div & divider_inst.io.div_ready) | (~divider_inst.io.div_ready & ~divider_inst.io.div_done)

        io.alu_stall_req := io.ex_stage_valid & (div_stall_req | mul_stall_req)
    } else null

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
        if(AppleRISCVCfg.RV32M) {
            is(AluOpcodeEnum.DIV)  {io.alu_out := rv32mArea.quotient}
            is(AluOpcodeEnum.DIVU) {io.alu_out := rv32mArea.quotient}
            is(AluOpcodeEnum.REM)  {io.alu_out := rv32mArea.reminder}
            is(AluOpcodeEnum.REMU) {io.alu_out := rv32mArea.reminder}
        }
        default {io.alu_out := and_result.asBits}
    }
}