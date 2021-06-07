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
    val CLK   = in Bool
    val RESET = in Bool
    val UART0 = master(Uart())
    val GPIO = master(TriStateArray(12 bits))
    val PWM0 = out Bits(4 bits)
  }

  // Clock domain and PLL
  val clkCtrl = new Area {
    val mmcm = new mmcm_50mhz
    mmcm.io.clk_in1 := io.CLK

    //Create a new clock domain named 'core'
    val socClockDomain = ClockDomain.internal(
      name = "soc",
      frequency = FixedFrequency(50 MHz),
      config = ClockDomainConfig(
        clockEdge        = RISING,
        resetKind        = SYNC,
        resetActiveLevel = HIGH
      )
    )

    socClockDomain.clock := mmcm.io.clk_out1
    // button create low level when pressed
    socClockDomain.reset := ResetCtrl.asyncAssertSyncDeassert(~io.RESET, socClockDomain)
  }

  val top = new ClockingArea(clkCtrl.socClockDomain) {
    val artySoC = ArtySoC()
    artySoC.io.clk    := clkCtrl.socClockDomain.clock
    artySoC.io.reset  := clkCtrl.socClockDomain.reset
    artySoC.io.uart0  <> io.UART0
    artySoC.io.load_imem  :=  io.GPIO(8).read
    if(SoCCfg.USE_GPIO) artySoC.io.gpio <> io.GPIO
    if(SoCCfg.USE_PWM0) artySoC.io.pwm0 <> io.PWM0
  }
}

object ArtyA7_topMain{
  def main(args: Array[String]) {
    // CPU Configuration
    AppleRISCVCfg.USE_RV32M   = true
    AppleRISCVCfg.USE_BPU     = false
    CsrCfg.USE_MHPMC3         = true
    CsrCfg.USE_MHPMC4         = true
    SpinalVerilog(InOutWrapper(ArtyA7_top()))
  }
}