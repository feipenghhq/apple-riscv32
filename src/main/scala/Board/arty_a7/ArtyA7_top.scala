///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: apple_riscv_soc_top
//
// Author: Heqing Huang
// Date Created: 04/26/2021
//
// ================== Description ==================
//
// The SOC top level for FPGA
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package Board.arty_a7

import AppleRISCV._
import AppleRISCVSoC.ip._
import AppleRISCVSoC._
import spinal.core._
import spinal.lib._
import spinal.lib.com.uart.{Uart, UartCtrlGenerics}
import spinal.lib.io.{InOutWrapper, TriStateArray}

case class ArtyA7_top() extends Component {
  val cfg = AppleSoCCfg_arty()
  val io = new Bundle {
    val clk = in Bool
    val gpio0 = if (cfg.USE_GPIO0) master(TriStateArray(32 bits)) else null
    val uart0 = master(Uart())  // this is needed for debug
    val load_imem = in Bool
  }

  noIoPrefix()

  // Clock domain and PLL
  val clkCtrl = new Area {
    val mmcm = new mmcm
    mmcm.io.clk_in1 := io.clk

    //Create a new clock domain named 'core'
    val coreClockDomain = ClockDomain.internal(
      name = "core",
      frequency = FixedFrequency(100 MHz),
      config = ClockDomainConfig(
        clockEdge        = RISING,
        resetKind        = SYNC,
        resetActiveLevel = HIGH
      )
    )

    coreClockDomain.clock := mmcm.io.clk_out1
    coreClockDomain.reset := False
  }

  val top = new ClockingArea(clkCtrl.coreClockDomain) {
    val apple_riscv_soc_inst = AppleSoC_arty()
    apple_riscv_soc_inst.io.clk   := clkCtrl.coreClockDomain.clock
    apple_riscv_soc_inst.io.reset := clkCtrl.coreClockDomain.reset
    apple_riscv_soc_inst.io.uart0  <> io.uart0
    apple_riscv_soc_inst.io.load_imem  :=  io.load_imem
    if (cfg.USE_GPIO0) apple_riscv_soc_inst.io.gpio0  <> io.gpio0
  }
}

object ArtyA7_AppleRISCVSoCMain{
  def main(args: Array[String]) {
    AppleRISCVCfg.USE_RV32M   = true
    AppleRISCVCfg.USE_BPU     = true
    CsrCfg.USE_MHPMC3  = true
    CsrCfg.USE_MHPMC4  = true
    SpinalVerilog(InOutWrapper(ArtyA7_top()))
  }
}