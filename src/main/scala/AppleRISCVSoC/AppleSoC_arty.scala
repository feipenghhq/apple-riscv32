///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: AppleSoC_arty
//
// Author: Heqing Huang
// Date Created: 03/30/2021
// Revision V2: 05/10/2021
//
// ================== Description ==================
//
// The Arty A7 SoC top level
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC

import AppleRISCV._
import ip._
import bus._
import spinal.core._
import spinal.lib._
import spinal.lib.com.uart._
import spinal.lib.io.{InOutWrapper, TriStateArray}

import scala.collection.mutable.ArrayBuffer

/**
 * Configuration for this SoC
 */

object AppleSoCCfg_arty {

    val NAME = "AppleSoC"

    val INSTR_RAM_BASE = SoCAddrMap.QSPI0_BASE
    val INSTR_RAM_TOP  = SoCAddrMap.QSPI0_BASE + ((0x1 << SoCCfg.INSTR_RAM_ADDR_WIDTH)-1)

    val DATA_RAM_BASE = SoCAddrMap.DTIM_BASE
    val DATA_RAM_TOP  = SoCAddrMap.DTIM_BASE + ((0x1 << SoCCfg.DATA_RAM_ADDR_WIDTH)-1)

    val cpuSibCfg = SibConfig(
        addressWidth = AppleRISCVCfg.XLEN,
        dataWidth    = AppleRISCVCfg.XLEN,
        addr_lo      = SoCAddrMap.CPU_BASE,
        addr_hi      = SoCAddrMap.CPU_TOP
    )

    val imemSibCfg = SibConfig(
      addressWidth = SoCCfg.INSTR_RAM_ADDR_WIDTH,
      dataWidth    = AppleRISCVCfg.XLEN,
      addr_lo      = INSTR_RAM_BASE,
      addr_hi      = INSTR_RAM_TOP
    )

    val dmemSibCfg = SibConfig(
      addressWidth = SoCCfg.DATA_RAM_ADDR_WIDTH,
      dataWidth    = AppleRISCVCfg.XLEN,
      addr_lo      = DATA_RAM_BASE,
      addr_hi      = DATA_RAM_TOP
    )

    var gpio0Cfg = GpioCfg(HI_INT = true, LO_INT = true, RISE_INT = true, FALL_INT = true, 12)
    var uartDbgBaudRate = 115200

    var USE_UART0 = true
    var USE_GPIO0 = true
    var USE_PWM0  = true
}


case class AppleSoC_arty() extends Component {

    val cfg = AppleSoCCfg_arty

    val io = new Bundle {
        val clk         = in Bool
        val reset       = in Bool
        val load_imem   = in Bool
        val uart0       = master(Uart())
        val gpio0       = if (cfg.USE_GPIO0) master(TriStateArray(12 bits)) else null
        val pwm0cmpgpio = if (cfg.USE_PWM0)  out Bits(4 bits) else null
    }
    noIoPrefix()

    val cpu_rst = Bool

    val socClkDomain = ClockDomain(
        clock = io.clk,
        reset = io.reset,
        frequency = FixedFrequency(100 MHz),
        config = ClockDomainConfig(
            clockEdge        = RISING,
            resetKind        = SYNC,
            resetActiveLevel = HIGH
        )
    )

    val cpuClkDomain = ClockDomain(
        clock = io.clk,
        reset = cpu_rst,
        frequency = socClkDomain.frequency,
        config = socClkDomain.config
    )

    val cpu = new ClockingArea(cpuClkDomain) {
        val core = AppleRISCV()
    }

    val soc = new ClockingArea(socClkDomain) {

        // ====================================
        // soc component instance
        // ====================================

        // Fixed Component

        val imem_inst = BlockRAM(usePort2 = true, cfg.imemSibCfg, cfg.imemSibCfg)
        val dmem_inst = BlockRAM(usePort2 = false, cfg.dmemSibCfg)
        val clic_inst = Clic(PeripSibCfg.clicSibCfg)
        val plic_inst = Plic(PeripSibCfg.plicSibCfg)
        val uart2imem_inst = ip.Uart2Imem(cfg.imemSibCfg, cfg.uartDbgBaudRate)

        // Peripherals
        val peripList  = ArrayBuffer[(Component, SibConfig, Sib)]()
        val aon_inst  = AON(PeripSibCfg.aonSibCfg)
        peripList.append((aon_inst, PeripSibCfg.aonSibCfg, aon_inst.io.aon_sib))

        val uart0_inst = if (cfg.USE_UART0) SibUart(PeripSibCfg.uart0SibCfg) else null
        if (cfg.USE_UART0) {
            peripList.append((uart0_inst, PeripSibCfg.uart0SibCfg, uart0_inst.io.uart_sib))
            uart0_inst.io.en := ~uart2imem_inst.io.downloading // only enable uart when we are not using uart as debug port
        }

        val gpio0_inst = if (cfg.USE_GPIO0) Gpio(cfg.gpio0Cfg, PeripSibCfg.gpio0SibCfg) else null
        if (cfg.USE_GPIO0) peripList.append((gpio0_inst, PeripSibCfg.gpio0SibCfg, gpio0_inst.io.gpio_sib))

        val pwm0_inst = if (cfg.USE_PWM0) PWM(8, PeripSibCfg.pwm0SibCfg) else null
        if (cfg.USE_PWM0) peripList.append((pwm0_inst, PeripSibCfg.pwm0SibCfg, pwm0_inst.io.pwm_sib))

        // ====================================
        // cpu core signal connection
        // ====================================

        // clock and reset
        cpu_rst  := aon_inst.io.corerst

        // connect interrupt to cpu core
        cpu.core.io.external_interrupt  := plic_inst.io.external_irq
        cpu.core.io.timer_interrupt     := clic_inst.io.timer_irq
        cpu.core.io.software_interrupt  := clic_inst.io.software_irq
        cpu.core.io.debug_interrupt     := False

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
        imem_switch.hostSib      <> cpu.core.io.imem_sib
        imem_switch.clientSib(0) <> imem_inst.io.port1

        // imem data mux connection
        imem_inst.io.port2  <> imem_data_mux.outputSib

        // dmem switch connection
        dmem_switch.hostSib      <> cpu.core.io.dmem_sib        // To CPU
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
        aon_inst.io.uartdbgrst_req <> uart2imem_inst.io.downloading

        // Imem debug bus
        uart2imem_inst.io.imem_dbg_sib <> imem_data_mux.inputSib(1)  // To imem data mux port 1

        // GPIO port
        if(cfg.USE_GPIO0) io.gpio0 <> gpio0_inst.io.gpio

        // Uart port
        io.uart0.rxd <> uart2imem_inst.io.uart.rxd
        if(cfg.USE_UART0) io.uart0 <> uart0_inst.io.uart

        // PWM0 port
        if (cfg.USE_PWM0) {io.pwm0cmpgpio <> pwm0_inst.io.pwmcmpgpio}

        // connect peripheral interrupt to PLIC
        plic_inst.io.plic_irq_in := 0
        val rtc_irq_base   = 2
        val uart0_irq_base = 3
        val gpio_irq_base  = 8
        val pwm0_irq_base  = 40
        plic_inst.io.plic_irq_in(rtc_irq_base) := aon_inst.io.rtc_irq
        plic_inst.io.plic_irq_in(uart0_irq_base) := uart0_inst.io.rxwm | uart0_inst.io.txwm
        if (cfg.USE_GPIO0) {
            val gpio_width = gpio0_inst.io.gpio_irq.getBitsWidth
            for (idx <- 0 until gpio_width) {
                plic_inst.io.plic_irq_in(gpio_irq_base+idx) := gpio0_inst.io.gpio_irq(idx)
            }
        }
        if (cfg.USE_PWM0) {
            val pwm_width = pwm0_inst.io.pwmcmpip.getBitsWidth
            for (idx <- 0 until pwm_width)
            plic_inst.io.plic_irq_in(pwm0_irq_base+idx) := pwm0_inst.io.pwmcmpip(idx)
        }
    }
}

object AppleSoC_artyMain{
    def main(args: Array[String]) {
        if (args.size > 0) {
            SoCCfg.INSTR_RAM_ADDR_WIDTH = args(0).toInt
            println("Generate with INSTR_RAM_ADDR_WIDTH = " + args(0))
        }
        AppleRISCVCfg.USE_RV32M    = true
        AppleRISCVCfg.USE_BPU      = false
        CsrCfg.USE_MHPMC3          = true
        CsrCfg.USE_MHPMC4          = true
        SpinalVerilog(InOutWrapper(AppleSoC_arty()))
    }
}