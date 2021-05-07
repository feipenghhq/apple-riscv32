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
  if (_type == "DSP") {
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

    io.mul_ready := ~busy
    io.product   := stage(AppleRISCVCfg.MULSTAGE-2)
    io.product_valid := stage_valid(AppleRISCVCfg.MULSTAGE-1)
    io.product_early_valid := stage_valid(AppleRISCVCfg.MULSTAGE-2)
  }

}
