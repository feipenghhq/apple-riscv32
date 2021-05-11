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
// Revision V2: 05/10/2021
//
// ================== Description ==================
//
// The Arty A7 SoC top level
//
// Fixed Component:
// - AppleRISCV Core
// - On-chip Instruction RAM and Data RAM
// - CLIC/PLIC
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

import scala.collection.mutable.ArrayBuffer

/**
 * Configuration for this SoC
 */

case class AppleSoCCfg_arty() {

    val NAME = "AppleSoC"

    val CLIC_TIMER_WIDTH = 64
    val TIMER_TIMER_WIDTH = 64

    val INSTR_RAM_ADDR_WIDTH = 16
    val INSTR_RAM_BASE = SoCAddrMap.QSPI0_BASE
    val INSTR_RAM_TOP  = SoCAddrMap.QSPI0_BASE + 0xFFFF

    val DATA_RAM_ADDR_WIDTH  = SoCAddrMap.DTIM_ADDR_WIDTH
    val DATA_RAM_BASE = SoCAddrMap.DTIM_BASE
    val DATA_RAM_TOP  = SoCAddrMap.DTIM_TOP

    val cpuSibCfg = SibConfig(
        addressWidth = AppleRISCVCfg.XLEN,
        dataWidth    = AppleRISCVCfg.XLEN,
        addr_lo      = SoCAddrMap.CPU_BASE,
        addr_hi      = SoCAddrMap.CPU_TOP
    )

    val imemSibCfg = SibConfig(
      addressWidth = INSTR_RAM_ADDR_WIDTH,
      dataWidth    = AppleRISCVCfg.XLEN,
      addr_lo      = INSTR_RAM_BASE,
      addr_hi      = INSTR_RAM_TOP
    )

    val dmemSibCfg = SibConfig(
      addressWidth = DATA_RAM_ADDR_WIDTH,
      dataWidth    = AppleRISCVCfg.XLEN,
      addr_lo      = DATA_RAM_BASE,
      addr_hi      = DATA_RAM_TOP
    )

    var uartCfg = UartCfg(UartCtrlGenerics())
    var gpio0Cfg = new GpioCfg(false, false, false, false, 32)
    var uartDbgBaudRate = 115200
    
    var USE_UART0 = true
    var USE_GPIO0 = true
}


case class AppleSoC_arty() extends Component {

    val cfg = AppleSoCCfg_arty()

    val io = new Bundle {
        val clk        = in Bool
        val reset      = in Bool
        val load_imem  = in Bool
        val gpio0 = if (cfg.USE_GPIO0) master(TriStateArray(32 bits)) else null
        val uart0 = master(Uart())  // this is needed for debug
    }
    noIoPrefix()

    val socClockDomain = ClockDomain.internal(
        name = "soc",
        frequency = FixedFrequency(100 MHz),
        config = ClockDomainConfig(
            clockEdge        = RISING,
            resetKind        = SYNC,
            resetActiveLevel = HIGH
        )
    )

    socClockDomain.clock := io.clk
    socClockDomain.reset := io.reset

    val soc = new ClockingArea(socClockDomain) {

        // ====================================
        // soc component instance
        // ====================================

        // Fixed Component
        val cpu_core  = AppleRISCV()
        val imem_inst = BlockRAM(usePort2 = true, cfg.imemSibCfg, cfg.imemSibCfg)
        val dmem_inst = BlockRAM(usePort2 = false, cfg.dmemSibCfg)
        val clic_inst = Clic(PeripSibCfg.clicSibCfg)
        val plic_inst = Plic(PeripSibCfg.plicSibCfg)
        val rstctrl_inst   = RstCtrl()
        val uart2imem_inst = ip.Uart2imem(cfg.imemSibCfg, cfg.uartDbgBaudRate)

        // Optional Component
        val peripList  = ArrayBuffer[(Component, SibConfig, Sib)]()

        val uart0_inst = if (cfg.USE_UART0) SibUart(cfg.uartCfg, PeripSibCfg.uart0SibCfg) else null
        if (cfg.USE_UART0) peripList.append((uart0_inst, PeripSibCfg.uart0SibCfg, uart0_inst.io.uart_sib))

        val gpio0_inst = if (cfg.USE_GPIO0) Gpio(cfg.gpio0Cfg, PeripSibCfg.gpio0SibCfg) else null
        if (cfg.USE_GPIO0) peripList.append((gpio0_inst, PeripSibCfg.gpio0SibCfg, gpio0_inst.io.gpio_sib))


        // ====================================
        // cpu core signal connection
        // ====================================

        // clock and reset
        cpu_core.io.clk     := socClockDomain.clock
        cpu_core.io.reset   := rstctrl_inst.io.cpu_reset_req

        // connect interrupt to cpu core
        cpu_core.io.external_interrupt  := plic_inst.io.external_interrupt
        cpu_core.io.timer_interrupt     := clic_inst.io.timer_interrupt
        cpu_core.io.software_interrupt  := clic_inst.io.software_interrupt
        cpu_core.io.debug_interrupt     := False

        // ====================================
        // SOC Bus Switch instance
        // ====================================

        // imem switch
        val imemClientSibCfg = Array(cfg.imemSibCfg)
        val imem_switch      = SibDecoder(cfg.cpuSibCfg, imemClientSibCfg)

        // imem mux for the data port
        val imemDataMuxCfg = Array(cfg.imemSibCfg, cfg.imemSibCfg)
        val imem_data_mux  = SibMux(imemDataMuxCfg, cfg.imemSibCfg)

        // dmem bus switch
        val dmemClientSibCfg = Array(
            cfg.imemSibCfg,
            cfg.dmemSibCfg,
            PeripSibCfg.peripSibCfg
        )
        val dmem_switch = SibDecoder(cfg.cpuSibCfg, dmemClientSibCfg)

        // peripheral switch
        val peripClientSibCfg = ArrayBuffer(
            PeripSibCfg.clicSibCfg,
            PeripSibCfg.plicSibCfg)
        for (perip <- peripList) {
            peripClientSibCfg.append(perip._2)
        }
        val perip_switch = SibDecoder(PeripSibCfg.peripSibCfg, peripClientSibCfg.toArray)

        // ====================================
        // SOC bus connection
        // ====================================

        // imem switch connection
        imem_switch.hostSib      <> cpu_core.io.imem_sib
        imem_switch.clientSib(0) <> imem_inst.io.port1

        // imem data mux connection
        imem_inst.io.port2  <> imem_data_mux.outputSib

        // dmem switch connection
        dmem_switch.hostSib      <> cpu_core.io.dmem_sib        // To CPU
        dmem_switch.clientSib(0) <> imem_data_mux.inputSib(0)   // To imem data mux port 0
        dmem_switch.clientSib(1) <> dmem_inst.io.port1          // To dmem
        dmem_switch.clientSib(2) <> perip_switch.hostSib        // To Peripheral SIB Switch

        // peripheral switch connection
        perip_switch.clientSib(0) <> clic_inst.io.clic_sib      // To CLIC
        perip_switch.clientSib(1) <> plic_inst.io.plic_sib      // To PLIC
        // Connect to the remaining peripherals
        for ((perip, idx) <- peripList.zipWithIndex) {
            perip_switch.clientSib(idx+2) <> perip._3
        }
        // ====================================
        // Other ports/interface connection
        // ====================================

        // strap port
        io.load_imem <> uart2imem_inst.io.load_imem

        // reset controller
        rstctrl_inst.io.uart2imem_downloading <> uart2imem_inst.io.downloading

        // Imem debug bus
        uart2imem_inst.io.imem_dbg_sib <> imem_data_mux.inputSib(1)  // To imem data mux port 1

        // GPIO port
        if(cfg.USE_GPIO0) io.gpio0 <> gpio0_inst.io.gpio

        // Uart port
        io.uart0.rxd <> uart2imem_inst.io.uart.rxd
        if(cfg.USE_UART0) io.uart0 <> uart0_inst.io.uart

        // connect peripheral interrupt to plic
        plic_inst.io.gpio0_int := False
        plic_inst.io.gpio1_int := False
        plic_inst.io.timer_int := False
        plic_inst.io.uart_int  := uart0_inst.io.uart_interrupt
    }
}

object AppleSoC_artyMain{
    def main(args: Array[String]) {
        AppleRISCVCfg.USE_RV32M   = true
        AppleRISCVCfg.USE_BPU     = true
        CsrCfg.USE_MHPMC3  = true
        CsrCfg.USE_MHPMC4  = true
        SpinalVerilog(InOutWrapper(AppleSoC_arty()))
    }
}