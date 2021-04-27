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

package soc

import core._
import ip._
import bus.sib._
import debug._

import spinal.core._
import spinal.lib._
import spinal.lib.com.uart.Uart


case class soc_cfg(
    soc_param: SOC_PARAM  = null,
    cpu_param: CPU_PARAM  = null,
    gpioCfg: GpioCfg      = null,
    uartCfg: UartCfg      = null,
    uartDbgBuardRate: Int = 115200
)

case class apple_riscv_soc(cfg: soc_cfg) extends Component {

    val io = new Bundle {
        val clk         = in Bool
        val reset       = in Bool
        val load_imem   = in Bool
        val gpio0_port  = gpio_io(cfg.gpioCfg, useInt = false)
        val gpio1_port  = gpio_io(cfg.gpioCfg, useInt = false)
        val uart_port   = master(Uart())
    }

    noIoPrefix()

    //Create a new clock domain named 'core'
    val coreClockDomain = ClockDomain.internal(
        name = "core",
        frequency = FixedFrequency(50 MHz),
        config = ClockDomainConfig(
            clockEdge        = RISING,
            resetKind        = SYNC,
            resetActiveLevel = HIGH
        )
    )

    coreClockDomain.clock := io.clk
    coreClockDomain.reset := io.reset

    val soc = new ClockingArea(coreClockDomain) {

        // == soc component instance == //
        val cpu_core  = apple_riscv(cfg.cpu_param)
        val imem_inst = imem(cfg.soc_param)
        val dmem_inst = dmem(cfg.soc_param)
        val clic_inst = clic(cfg.soc_param.clicSibCfg, cfg.soc_param.CLIC_TIMER_WIDTH)
        val plic_inst = plic(cfg.soc_param.plicSibCfg)
        val timer_inst = timer(cfg.soc_param.timerSibCfg)
        val uart_inst = SibUart(cfg.uartCfg, cfg.soc_param.uartSibCfg)
        val gpio0_inst = gpio(cfg.gpioCfg, cfg.soc_param.gpio0SibCfg)
        val gpio1_inst = gpio(cfg.gpioCfg, cfg.soc_param.gpio1SibCfg)
        val uart2imem_inst = uart2imem(cfg.soc_param.imemSibCfg, cfg.uartDbgBuardRate)

        // == SOC Bus Switch instance == //
        // imem switch
        val imemClientSibCfg = Array(cfg.soc_param.imemSibCfg)
        val imem_switch = Sib_decoder(cfg.soc_param.cpuSibCfg, imemClientSibCfg)

        // dmem bus switch
        val dmemClientSibCfg = Array(cfg.soc_param.dmemSibCfg, cfg.soc_param.clicSibCfg,
            cfg.soc_param.plicSibCfg, cfg.soc_param.peripHostSibCfg)
        val dmem_switch = Sib_decoder(cfg.soc_param.cpuSibCfg, dmemClientSibCfg)

        // peripheral switch
        val peripClientSibCfg = Array(cfg.soc_param.timerSibCfg, cfg.soc_param.uartSibCfg,
            cfg.soc_param.gpio0SibCfg, cfg.soc_param.gpio1SibCfg)
        val perip_switch = Sib_decoder(cfg.soc_param.peripHostSibCfg, peripClientSibCfg)


        // == SOC bus connection == //

        // imem switch connection
        imem_switch.hostSib <> cpu_core.imem_sib
        imem_switch.clientSib(0) <> imem_inst.imem_cpu_sib

        // dmem switch connection
        dmem_switch.hostSib <> cpu_core.dmem_sib            // To CPU
        dmem_switch.clientSib(0) <> dmem_inst.dmem_sib      // To dmem
        dmem_switch.clientSib(1) <> clic_inst.clic_sib      // To CLIC
        dmem_switch.clientSib(2) <> plic_inst.plic_sib      // To PLIC
        dmem_switch.clientSib(3) <> perip_switch.hostSib    // To Peripheral SIB Switch

        // peripheral switch connection
        perip_switch.clientSib(0) <> timer_inst.timer_sib   // To Timer
        perip_switch.clientSib(1) <> uart_inst.uart_sib     // To Uart
        perip_switch.clientSib(2) <> gpio0_inst.gpio_sib    // To GPIO0
        perip_switch.clientSib(3) <> gpio1_inst.gpio_sib    // To GPIO1

        // == Imem debug bus == //
        uart2imem_inst.io.imem_dbg_sib <> imem_inst.imem_dbg_sib

        // == Other port connection == //

        io.gpio0_port.gpio <> gpio0_inst.io.gpio
        io.gpio1_port.gpio <> gpio1_inst.io.gpio
        io.uart_port <> uart_inst.uart
        io.uart_port.rxd <> uart2imem_inst.io.uart.rxd
        io.load_imem <> uart2imem_inst.io.load_imem

        // connect interrupt to cpu core
        cpu_core.io.external_interrupt  := plic_inst.io.external_interrupt
        cpu_core.io.timer_interrupt     := clic_inst.io.timer_interrupt
        cpu_core.io.software_interrupt  := clic_inst.io.software_interrupt
        cpu_core.io.debug_interrupt     := False

        // connect peripheral interrupt to plic
        plic_inst.io.gpio0_int  := gpio0_inst.io.gpio_int_pe
        plic_inst.io.gpio1_int  := gpio1_inst.io.gpio_int_pe
        plic_inst.io.timer_int := timer_inst.io.timer_interrupt
        plic_inst.io.uart_int  := uart_inst.io.uart_interrupt
    }



}