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
    val alu_mulop = if (AppleRISCVCfg.RV32M) out Bool else null
    val mul_out   = if (AppleRISCVCfg.RV32M) out Bits(AppleRISCVCfg.XLEN bits) else null

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
    // Multiplier using FPGA DSP block => Inferring DSP block
    val multiplierDSPArea = if (AppleRISCVCfg.RV32M) new Area {
        val is_multl = io.alu_opcode === AluOpcodeEnum.MUL
        val is_multh = io.alu_opcode === AluOpcodeEnum.MULH | io.alu_opcode === AluOpcodeEnum.MULHSU | io.alu_opcode === AluOpcodeEnum.MULHU
        val is_mult = (is_multh | is_multl)
        val busy = RegInit(False)
        val new_op = is_mult & ~busy
        // pipeline stages except the input stage
        val stage = Vec(Reg(SInt((2 * AppleRISCVCfg.XLEN) bits)), AppleRISCVCfg.MULSTAGE - 1)
        // pipeline valid stage, including the fist stage
        val stage_valid = Vec(RegInit(False), AppleRISCVCfg.MULSTAGE)

        // Stage 0 - flop the operand
        val op1 = RegNext(io.operand_1)
        val op2 = RegNext(io.operand_2)

        // Process the signed and unsigned bits
        // Note: We always use signed multiplier here to save DSP resource
        // In make unsigned bit looks like signed, we add addition 1 bit to the
        // top as the new signed bit for both signed and unsigned value
        // For signed value, we will add 1, for unsigned value we will add 0
        // Now the multiplier become 33 * 33 bit this is OK for FPGA because
        // FPGA usually has 18 * 18 as DSP block and we will need to use multiple of them anyway.
        val multiplicand = SInt(AppleRISCVCfg.XLEN + 1 bits)    // RS1
        when(io.alu_opcode === AluOpcodeEnum.MULHU) {
            multiplicand := (False ## op1).asSInt
        }.otherwise{
            multiplicand := op1.asSInt.resized
        }
        val multiplier = SInt(AppleRISCVCfg.XLEN+1 bits)      // RS2
        when(io.alu_opcode === AluOpcodeEnum.MULHU || io.alu_opcode === AluOpcodeEnum.MULHSU) {
            multiplier := (False ## op2).asSInt
        }.otherwise{
            multiplier := op2.asSInt.resized
        }

        // Stage 1
        stage(0) := (multiplicand * multiplier)(2*AppleRISCVCfg.XLEN-1 downto 0)

        // Remaining Stage pipeline
        for (i <- Range(1, stage.length)) {
            stage(i) := stage(i-1)
        }

        // Stage valid pipeline
        stage_valid(0) := new_op
        for (i <- Range(1, stage_valid.length)) {
            stage_valid(i) := stage_valid(i-1)
        }

        val done = stage_valid(AppleRISCVCfg.MULSTAGE-1)
        when(new_op) {
            busy := True
        }.elsewhen(done) {
            busy := False
        }

        val lo = stage(AppleRISCVCfg.MULSTAGE-2)(AppleRISCVCfg.XLEN-1 downto 0).asBits
        val hi = stage(AppleRISCVCfg.MULSTAGE-2)(2*AppleRISCVCfg.XLEN-1 downto AppleRISCVCfg.XLEN).asBits
        val is_multl_s1 = RegNext(is_multl)    // need to delay this for one cycle as we are selecting the data at MEM stage
        io.mul_out := Mux(is_multl_s1, lo, hi) // EX/MEM stage pipeline is embedded in the DSP block
        io.alu_mulop := done // EX/MEM stage pipeline is embedded in also embedded here
        // the stall request is from stage 0 to stage N-2
        io.alu_stall_req := (new_op | busy & ~stage_valid(AppleRISCVCfg.MULSTAGE-2)) & io.ex_stage_valid
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
        default {io.alu_out := and_result.asBits}
    }
}