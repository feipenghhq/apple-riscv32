///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: Divider
//
// Author: Heqing Huang
// Date Created: 05/06/2021
//
// ================== Description ==================
//
// Hardware Divider.
//
// This divider is a serial divider. It take 33 clocks to complete.
//
///////////////////////////////////////////////////////////////////////////////////////////////////


package AppleRISCV

import spinal.core._
import spinal.lib.fsm._

/**  */
case class AppleRISCVDivider() extends Component {
  val io = new Bundle {
    val stage_valid = in Bool
    val div_req     = in Bool
    val div_opcode  = in(DivOpcodeEnum)
    val dividend    = in  Bits(AppleRISCVCfg.XLEN bit)
    val divisor     = in  Bits(AppleRISCVCfg.XLEN bits)
    val result      = out Bits(AppleRISCVCfg.XLEN bits)
    val div_stall_req = out Bool
  }
  noIoPrefix()
  
  val divider_inst = MixedDivider(AppleRISCVCfg.XLEN)
  divider_inst.io.div_req  := io.div_req & io.stage_valid & ~divider_inst.io.div_done
  divider_inst.io.dividend := io.dividend
  divider_inst.io.divisor  := io.divisor
  divider_inst.io.flush    := ~io.stage_valid
  divider_inst.io.signed   := io.div_opcode === DivOpcodeEnum.DIV | io.div_opcode === DivOpcodeEnum.REM
  val quotient = divider_inst.io.quotient
  val reminder = divider_inst.io.remainder
  switch(io.div_opcode) {
    is(DivOpcodeEnum.DIV)  {io.result := quotient}
    is(DivOpcodeEnum.DIVU) {io.result := quotient}
    is(DivOpcodeEnum.REM)  {io.result := reminder}
    is(DivOpcodeEnum.REMU) {io.result := reminder}
  }
  io.div_stall_req := io.div_req & io.stage_valid & ~divider_inst.io.div_done
}

/** Mixed signed Divider */
case class MixedDivider(WIDTH: Int) extends Component {
  val io = new Bundle {
    val flush     = in Bool
    val div_req   = in  Bool
    val div_ready = out Bool
    val signed    = in Bool
    val dividend  = in  Bits(WIDTH bit)
    val divisor   = in  Bits(WIDTH bits)
    val quotient  = out Bits(WIDTH bits)
    val remainder = out Bits(WIDTH bits)
    val div_done  = out Bool
    val div_early_done  = out Bool
  }
  noIoPrefix()

  val divider = UnsignedDivider(WIDTH)

  divider.io.div_req   := io.div_req
  divider.io.flush     := io.flush
  io.div_ready         := divider.io.div_ready
  io.div_done          := divider.io.div_done
  io.div_early_done    := divider.io.div_early_done
  // process the input signal
  divider.io.dividend := io.dividend.asSInt.abs(io.signed)
  divider.io.divisor  := io.divisor.asSInt.abs(io.signed)

  // process the output signal
  val not_divide_by_zero = io.divisor =/= 0
  val quotient_is_negative = RegNextWhen(io.signed & not_divide_by_zero & (io.dividend.msb ^ io.divisor.msb), io.div_req & io.div_ready)
  val remainder_is_negative = RegNextWhen(io.signed & not_divide_by_zero & io.dividend.msb, io.div_req & io.div_ready)
  io.quotient := divider.io.quotient.twoComplement(quotient_is_negative).asBits.resized
  io.remainder := divider.io.remainder.twoComplement(remainder_is_negative).asBits.resized
}

/** Unsigned Divider */
case class UnsignedDivider(WIDTH: Int) extends Component{

  val io = new Bundle {
    val flush     = in Bool
    val div_req   = in  Bool
    val div_ready = out Bool
    val dividend  = in  UInt(WIDTH bit)
    val divisor   = in  UInt(WIDTH bits)
    val quotient  = out UInt(WIDTH bits)
    val remainder = out UInt(WIDTH bits)
    val div_done  = out Bool
    val div_early_done  = out Bool
  }

  val dividend_ff = Reg(io.dividend.clone())
  val divisor_ff = Reg(io.divisor.clone())
  val quotient_ff = Reg(io.quotient.clone())

  val extended_dividend = U"32'h0" @@ dividend_ff
  val extended_divisor = divisor_ff @@ U"32'h0"

  val divCtrl = new StateMachine {
    val idle = new State with EntryPoint
    val run  = new State

    val iter = Reg(UInt(log2Up(WIDTH+1) bits))

    io.div_early_done := False
    io.div_done := False
    io.div_ready := True
    idle.whenIsActive {
      iter := 0
      dividend_ff := io.dividend
      divisor_ff  := io.divisor
      quotient_ff := 0
      when (io.div_req & io.div_ready) {
        goto(run)
      }
    }

    // Main iteration
    run.whenIsActive {
      io.div_ready := False
      iter := iter + 1
      val sub_result = extended_dividend - (extended_divisor >> iter)
      quotient_ff := quotient_ff |<< 1
      when(sub_result(sub_result.getBitsWidth-1) === False) { // Signed bit is zero => positive
        quotient_ff(0) := True
        dividend_ff := sub_result(dividend_ff.getBitsWidth-1 downto 0)
      }.otherwise {
        quotient_ff(0) := False
      }

      when(iter === WIDTH) {io.div_early_done := True}
      when(io.flush) {
        goto(idle)
      }.elsewhen(iter === WIDTH + 1) {
        io.div_done  := True
        goto(idle)
      }
    }
  }

  io.quotient  := quotient_ff
  io.remainder := dividend_ff
}

// ===================================================================== //

import spinal.core.sim._
import scala.util.Random

object SimDivider {
  def main(args: Array[String]): Unit = {
    SimConfig.withWave.compile(UnsignedDivider(32)).doSim{ dut =>
      dut.clockDomain.forkStimulus(period = 10)
      for (i <- 0 until 10) {
        dut.io.dividend #= Random.nextInt(5000)
        dut.io.divisor  #= Random.nextInt(200)
        dut.io.div_req  #= true
        dut.clockDomain.waitSampling()
        dut.io.div_req  #= false
        for (_ <- 0 until 40) {
          dut.clockDomain.waitSampling()
          if (dut.io.div_done.toBoolean) {
            assert(dut.io.quotient.toBigInt == dut.io.dividend.toBigInt / dut.io.divisor.toBigInt)
            assert(dut.io.remainder.toBigInt == dut.io.dividend.toBigInt % dut.io.divisor.toBigInt)
          }
        }
      }
    }
  }
}
