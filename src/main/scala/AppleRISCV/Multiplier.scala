///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: Multiplier
//
// Author: Heqing Huang
// Date Created: 05/06/2021
//
// ================== Description ==================
//
// Multiplier
//
///////////////////////////////////////////////////////////////////////////////////////////////////


package AppleRISCV

import spinal.core._

case class AppleRISCVCMultiplier() extends Component {
  val io = new Bundle {
    val stage_valid   = in Bool
    val mul_req       = in Bool
    val mul_opcode    = in(MulOpcodeEnum())
    val multiplier    = in Bits(AppleRISCVCfg.XLEN bits)
    val multiplicand  = in Bits(AppleRISCVCfg.XLEN bits)
    val result        = out Bits(AppleRISCVCfg.XLEN bits)
    val mul_stall_req = out Bool
  }
  noIoPrefix()

  // Multiplier using FPGA DSP block => Inferring DSP block
  val multiplier_inst = Multiplier("DSP", 4, AppleRISCVCfg.XLEN+1, AppleRISCVCfg.XLEN+1)

  // Process the signed and unsigned bits
  // Note: We always use signed multiplier here to save DSP resource
  // In make unsigned bit looks like signed, we add addition 1 bit to the
  // top as the new signed bit for both signed and unsigned value
  // For signed value, we will add 1, for unsigned value we will add 0
  // Now the multiplier become 33 * 33 bit this is OK for FPGA because
  // FPGA usually has 18 * 18 as DSP block and we will need to use multiple of them anyway.
  val multiplicand = SInt(AppleRISCVCfg.XLEN + 1 bits) // RS1
  when(io.mul_opcode === MulOpcodeEnum.MULHU) {
    multiplicand := (False ## io.multiplicand).asSInt
  }.otherwise{
    multiplicand := io.multiplicand.asSInt.resized
  }
  val multiplier = SInt(AppleRISCVCfg.XLEN+1 bits) // RS2
  when(io.mul_opcode === MulOpcodeEnum.MULHU || io.mul_opcode === MulOpcodeEnum.MULHSU) {
    multiplier := (False ## io.multiplier).asSInt
  }.otherwise{
    multiplier := io.multiplier.asSInt.resized
  }

  multiplier_inst.io.multiplier   := multiplier
  multiplier_inst.io.multiplicand := multiplicand
  multiplier_inst.io.mul_valid    := io.stage_valid & io.mul_req & ~multiplier_inst.io.product_valid

  val is_mul_s   = RegNext(io.mul_opcode === MulOpcodeEnum.MUL);
  val product_lo = multiplier_inst.io.product(AppleRISCVCfg.XLEN-1 downto 0)
  val product_hi = multiplier_inst.io.product(2*AppleRISCVCfg.XLEN-1 downto AppleRISCVCfg.XLEN)

  io.result        := Mux(is_mul_s, product_lo, product_hi).asBits
  io.mul_stall_req := io.stage_valid & io.mul_req & ~multiplier_inst.io.product_valid
}

/**
 * Multiplier
 * Use product_valid if you take the ouyput to the downstream pipeline
 * Use product_early_valid if you use the DSP output register as the pipeline stage (to reduce latency)
 */
case class Multiplier(_type: String, stages: Int, multiplierSize: Int, multiplicandSize: Int) extends Component {

  val io = new Bundle {
    val mul_valid     = in Bool
    val mul_ready     = out Bool
    val multiplier    = in SInt(multiplierSize bits)
    val multiplicand  = in SInt(multiplicandSize bits)
    val product       = out SInt(multiplierSize+multiplicandSize bits)
    val product_valid = out Bool
    val product_early_valid = out Bool   // set 1 cycle earlier than product_valid
  }
  noIoPrefix()

  // Inferring DSP block in FPGA
  val dsp = if (_type == "DSP") new Area {
    val busy = RegInit(False)
    val new_op = io.mul_valid & io.mul_ready

    // pipeline stages except the first stage
    val stage = Vec(Reg(SInt(multiplierSize+multiplicandSize bits)), AppleRISCVCfg.MULSTAGE - 1)
    // pipeline valid stage, including the fist stage
    val stage_valid = Vec(RegInit(False), AppleRISCVCfg.MULSTAGE)

    // Stage 0 - flop the input operand
    val multiplier_s0 = RegNext(io.multiplier)
    val multiplicand_s0 = RegNext(io.multiplicand)
    // Stage 1 - do the multiplier
    stage(0) := (multiplier_s0 * multiplicand_s0)(multiplierSize+multiplicandSize-1 downto 0)
    // Remaining Stage pipeline
    for (i <- Range(1, stage.length)) {
      stage(i) := stage(i-1)
    }

    // Stage valid pipeline
    stage_valid(0) := new_op
    for (i <- Range(1, stage_valid.length)) {
      stage_valid(i) := stage_valid(i-1)
    }

    when(new_op) {
      busy := True
    }.elsewhen(io.product_valid) {
      busy := False
    }

    io.product := stage(AppleRISCVCfg.MULSTAGE-2)
    io.product_valid := stage_valid(AppleRISCVCfg.MULSTAGE-1)
    io.product_early_valid := stage_valid(AppleRISCVCfg.MULSTAGE-2)
    io.mul_ready := ~busy | io.product_valid
  }

}
