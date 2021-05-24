///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: ArtyA7_top
//
// Author: Heqing Huang
// Date Created: 04/26/2021
//
// ================== Description ==================
//
// The ScC top level for Arty A7 FPGA Board
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package Board

import AppleRISCV._
import AppleRISCVSoC._
import spinal.core._
import spinal.lib._
import spinal.lib.com.uart.Uart
import spinal.lib.io._

case class ArtyA7_top() extends Component {
  
  val io = new Bundle {
    val clk   = in Bool
    val reset = in Bool
    val uart0 = master(Uart())
    val gpio = master(TriStateArray(12 bits))
    val pwm = out Bits(4 bits)
  }
  noIoPrefix()

  // Clock domain and PLL
  val clkCtrl = new Area {
    val mmcm = new mmcm
    mmcm.io.clk_in1 := io.clk

    //Create a new clock domain named 'core'
    val socClockDomain = ClockDomain.internal(
      name = "soc",
      frequency = FixedFrequency(100 MHz),
      config = ClockDomainConfig(
        clockEdge        = RISING,
        resetKind        = SYNC,
        resetActiveLevel = HIGH
      )
    )

    socClockDomain.clock := mmcm.io.clk_out1
    // button create low level when pressed
    socClockDomain.reset := ResetCtrl.asyncAssertSyncDeassert(~io.reset, socClockDomain)
  }

  val top = new ClockingArea(clkCtrl.socClockDomain) {
    val AppleSoC_arty_inst = ArtySoC()
    AppleSoC_arty_inst.io.clk    := clkCtrl.socClockDomain.clock
    AppleSoC_arty_inst.io.reset  := clkCtrl.socClockDomain.reset
    AppleSoC_arty_inst.io.uart0  <> io.uart0
    AppleSoC_arty_inst.io.load_imem  :=  io.gpio(8).read
    if(SoCCfg.USE_GPIO) AppleSoC_arty_inst.io.gpio <> io.gpio
    if(SoCCfg.USE_PWM0) AppleSoC_arty_inst.io.pwm0 <> io.pwm
  }
}

object ArtyA7_topMain{
  def main(args: Array[String]) {
    // CPU Configuration
    AppleRISCVCfg.USE_RV32M   = true
    AppleRISCVCfg.USE_BPU     = true
    CsrCfg.USE_MHPMC3         = true
    CsrCfg.USE_MHPMC4         = true
    SpinalVerilog(InOutWrapper(ArtyA7_top()))
  }
}