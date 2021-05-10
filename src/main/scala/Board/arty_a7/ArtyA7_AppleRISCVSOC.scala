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

case class ArtyA7_AppleRISCVSoC(cfg: SoCCfg) extends Component {

  val io = new Bundle {
    val clk = in Bool
    val gpio0_port = master(TriStateArray(cfg.gpio0Cfg.GPIO_WIDTH bits))
    val gpio1_port = master(TriStateArray(cfg.gpio1Cfg.GPIO_WIDTH bits))
    val uart_port = master(Uart())
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
    val apple_riscv_soc_inst = AppleRISCVSoC(cfg)
    apple_riscv_soc_inst.io.clk   := clkCtrl.coreClockDomain.clock
    apple_riscv_soc_inst.io.reset := clkCtrl.coreClockDomain.reset
    apple_riscv_soc_inst.io.uart_port  <> io.uart_port
    apple_riscv_soc_inst.io.gpio0_port <> io.gpio0_port
    apple_riscv_soc_inst.io.gpio1_port <> io.gpio1_port
    apple_riscv_soc_inst.io.load_imem  :=  io.load_imem
  }
}

object ArtyA7_AppleRISCVSoCMain{
  def main(args: Array[String]) {
    AppleRISCVCfg.USE_RV32M   = true
    AppleRISCVCfg.USE_BPU     = true
    AppleRISCVCfg.USE_MHPMC3  = true
    AppleRISCVCfg.USE_MHPMC4  = true
    val cfg = SoCCfg (
      gpio0Cfg   = new GpioCfg(false, false, false, false, 8),
      gpio1Cfg   = new GpioCfg(false, false, false, false, 0),
      uartCfg   = new UartCfg(UartCtrlGenerics(), 8, 8)
    )
    SpinalVerilog(InOutWrapper(ArtyA7_AppleRISCVSoC(cfg))).printPruned()
  }
}