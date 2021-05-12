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

package Board.arty_a7

import AppleRISCV._
import AppleRISCVSoC._
import spinal.core._
import spinal.lib._
import spinal.lib.com.uart.Uart
import spinal.lib.io._

case class ArtyA7_top() extends Component {

  val cfg = AppleSoCCfg_arty()
  val io = new Bundle {
    val clk = in Bool
    val reset = in Bool
    val gpio0 = master(TriStateArray(12 bits))
    val uart0 = master(Uart())  // this is needed for debug
  }
  noIoPrefix()

  // Clock domain and PLL
  val clkCtrl = new Area {
    val mmcm = new mmcm
    mmcm.io.clk_in1 := io.clk

    //Create a new clock domain named 'core'
    val coreClockDomain = ClockDomain.internal(
      name = "soc",
      frequency = FixedFrequency(100 MHz),
      config = ClockDomainConfig(
        clockEdge        = RISING,
        resetKind        = SYNC,
        resetActiveLevel = HIGH
      )
    )

    coreClockDomain.clock := mmcm.io.clk_out1
    // button create low level when pressed
    coreClockDomain.reset := ResetCtrl.asyncAssertSyncDeassert(~io.reset, coreClockDomain)
  }


  val top = new ClockingArea(clkCtrl.coreClockDomain) {
    val AppleSoC_arty_inst = AppleSoC_arty()
    AppleSoC_arty_inst.io.clk    := clkCtrl.coreClockDomain.clock
    AppleSoC_arty_inst.io.reset  := clkCtrl.coreClockDomain.reset
    AppleSoC_arty_inst.io.uart0  <> io.uart0
    if(cfg.USE_GPIO0) AppleSoC_arty_inst.io.gpio0  <> io.gpio0
    AppleSoC_arty_inst.io.load_imem  :=  io.gpio0(8).read
  }
}

object ArtyA7_AppleRISCVSoCMain{
  def main(args: Array[String]) {
    // CPU Configuration
    AppleRISCVCfg.USE_RV32M   = true
    AppleRISCVCfg.USE_BPU     = true
    CsrCfg.USE_MHPMC3         = true
    CsrCfg.USE_MHPMC4         = true
    SpinalVerilog(InOutWrapper(ArtyA7_top()))
  }
}