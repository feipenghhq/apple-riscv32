///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: apple_riscv_soc
//
// Author: Heqing Huang
// Date Created: 03/30/2021
//
// ================== Description ==================
//
// The SOC top level
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC

import AppleRISCV._
import ip._
import bus._
import spinal.core._
import spinal.lib._
import spinal.lib.com.uart.{Uart, UartCtrlGenerics}
import spinal.lib.io.{InOutWrapper, TriStateArray}


case class SoCCfg(
    gpio0Cfg: GpioCfg     = null,
    gpio1Cfg: GpioCfg     = null,
    uartCfg: UartCfg      = null,
    uartDbgBuardRate: Int = 115200
)

case class AppleRISCVSoC(cfg: SoCCfg) extends Component {

    val io = new Bundle {
        val clk        = in Bool
        val reset      = in Bool
        val load_imem  = in Bool
        val gpio0_port = master(TriStateArray(cfg.gpio0Cfg.GPIO_WIDTH bits))
        val gpio1_port = master(TriStateArray(cfg.gpio1Cfg.GPIO_WIDTH bits))
        val uart_port  = master(Uart())
    }

    noIoPrefix()

    val socClockDomain = ClockDomain.internal(
        name = "AppleRISCVSoC",
        frequency = FixedFrequency(50 MHz),
        config = ClockDomainConfig(
            clockEdge        = RISING,
            resetKind        = SYNC,
            resetActiveLevel = HIGH
        )
    )

    socClockDomain.clock := io.clk
    socClockDomain.reset := io.reset

    val soc = new ClockingArea(socClockDomain) {

        // == soc component instance == //
        val cpu_core  = AppleRISCV()
        val rstctrl   = RstCtrl()
        val imem_inst = Imem()
        val dmem_inst = Dmem()
        val clic_inst = Clic(SIBCfg.clicSibCfg)
        val plic_inst = Plic(SIBCfg.plicSibCfg)
        val timer_inst = Timer(SIBCfg.timerSibCfg)
        val uart_inst  = SibUart(cfg.uartCfg, SIBCfg.uartSibCfg)
        val gpio0_inst = Gpio(cfg.gpio0Cfg, SIBCfg.gpio0SibCfg)
        val gpio1_inst = Gpio(cfg.gpio1Cfg, SIBCfg.gpio1SibCfg)
        val uart2imem_inst = ip.Uart2imem(SIBCfg.imemSibCfg, cfg.uartDbgBuardRate)


        // cpu core signal connection == //

        // clock and reset
        cpu_core.io.clk     := socClockDomain.clock
        cpu_core.io.reset   := rstctrl.io.cpu_reset_req

        // connect interrupt to cpu core
        cpu_core.io.external_interrupt := plic_inst.io.external_interrupt
        cpu_core.io.timer_interrupt := clic_inst.io.timer_interrupt
        cpu_core.io.software_interrupt := clic_inst.io.software_interrupt
        cpu_core.io.debug_interrupt := False

        // == SOC Bus Switch instance == //

        // imem switch
        val imemClientSibCfg = Array(SIBCfg.imemSibCfg)
        val imem_switch = Sib_decoder(SIBCfg.cpuSibCfg, imemClientSibCfg)

        // dmem bus switch
        val dmemClientSibCfg = Array(
            SIBCfg.dmemSibCfg,
            SIBCfg.clicSibCfg,
            SIBCfg.plicSibCfg,
            SIBCfg.peripHostSibCfg)
        val dmem_switch = Sib_decoder(SIBCfg.cpuSibCfg, dmemClientSibCfg)

        // peripheral switch
        val peripClientSibCfg = Array(
            SIBCfg.timerSibCfg,
            SIBCfg.uartSibCfg,
            SIBCfg.gpio0SibCfg,
            SIBCfg.gpio1SibCfg)
        val perip_switch = Sib_decoder(SIBCfg.peripHostSibCfg, peripClientSibCfg)


        // == SOC bus connection == //

        // imem switch connection
        imem_switch.hostSib      <> cpu_core.io.imem_sib
        imem_switch.clientSib(0) <> imem_inst.imem_cpu_sib

        // dmem switch connection
        dmem_switch.hostSib      <> cpu_core.io.dmem_sib // To CPU
        dmem_switch.clientSib(0) <> dmem_inst.dmem_sib // To dmem
        dmem_switch.clientSib(1) <> clic_inst.io.clic_sib // To CLIC
        dmem_switch.clientSib(2) <> plic_inst.io.plic_sib // To PLIC
        dmem_switch.clientSib(3) <> perip_switch.hostSib // To Peripheral SIB Switch

        // peripheral switch connection
        perip_switch.clientSib(0) <> timer_inst.io.timer_sib // To Timer
        perip_switch.clientSib(1) <> uart_inst.io.uart_sib // To Uart
        perip_switch.clientSib(2) <> gpio0_inst.io.gpio_sib // To GPIO0
        perip_switch.clientSib(3) <> gpio1_inst.io.gpio_sib // To GPIO1

        // == Other ports/interface connection == //

        // Imem debug bus
        uart2imem_inst.io.imem_dbg_sib <> imem_inst.imem_dbg_sib

        // GPIO port
        io.gpio0_port <> gpio0_inst.io.gpio
        io.gpio1_port <> gpio1_inst.io.gpio

        // Uart port
        io.uart_port <> uart_inst.io.uart
        io.uart_port.rxd <> uart2imem_inst.io.uart.rxd

        // reset controller
        rstctrl.io.uart2imem_downloading <> uart2imem_inst.io.downloading

        // strap port
        io.load_imem <> uart2imem_inst.io.load_imem

        // connect peripheral interrupt to plic
        plic_inst.io.gpio0_int := gpio0_inst.io.gpio_int_pe
        plic_inst.io.gpio1_int := gpio1_inst.io.gpio_int_pe
        plic_inst.io.timer_int := timer_inst.io.timer_interrupt
        plic_inst.io.uart_int  := uart_inst.io.uart_interrupt
    }
}

object AppleRISCVSoCMain{
    def main(args: Array[String]) {
        val cfg = SoCCfg(
            gpio0Cfg = new GpioCfg(false, false, false, false, 8),
            gpio1Cfg = new GpioCfg(false, false, false, false, 0),
            uartCfg = UartCfg(UartCtrlGenerics(), 8, 8)
        )
        SpinalVerilog(InOutWrapper(AppleRISCVSoC(cfg))).printPruned()
    }
}