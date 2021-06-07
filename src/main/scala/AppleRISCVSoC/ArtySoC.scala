///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: ArtySoC
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
import IP._
import spinal.core._
import spinal.lib._
import spinal.lib.com.uart._
import spinal.lib.io._

/** Arty SoC */
case class ArtySoC() extends Component {

    val io = new Bundle {
        val clk         = in Bool
        val reset       = in Bool
        val load_imem   = in Bool
        val uart0       = master(Uart())
        val gpio        = if (SoCCfg.USE_GPIO) master(TriStateArray(12 bits)) else null
        val pwm0        = if (SoCCfg.USE_PWM0) out Bits(4 bits) else null
    }

    val cpu_rst = Bool
    val perip_rst = Bool

    val socClkDomain = ClockDomain(
        clock = io.clk,
        reset = io.reset,
        frequency = FixedFrequency(50 MHz),
        config = ClockDomainConfig(
            clockEdge        = RISING,
            resetKind        = SYNC,
            resetActiveLevel = HIGH
        )
    )

    val soc = new ClockingArea(socClkDomain) {

        val cpu_rst_area = new ResetArea(cpu_rst, true) {
            val core = AppleRISCV()
            core.io.debug_interrupt := False
            core.setName("core")
            newReset.setName("io_core_rst")
        }

        val uartdbg = UartDebug(AhbLite3Cfg.ahblite3Cfg, SoCCfg.uartDbgBaudRate)
        val imem = Ahblite3Bram_1rw(AhbLite3Cfg.imemAhblite3Cfg)
        val dmem = Ahblite3Bram_1rw(AhbLite3Cfg.dmemAhblite3Cfg)
        val ahblite3corssbar = Ahblite3crossbar(AhbLite3Cfg.ahblite3Cfg)
        ahblite3corssbar.io.dbg_ahb  <> uartdbg.io.ahblite3
        ahblite3corssbar.io.ibus_ahb <> cpu_rst_area.core.io.ibus_ahb
        ahblite3corssbar.io.dbus_ahb <> cpu_rst_area.core.io.dbus_ahb
        imem.io.port1 <> ahblite3corssbar.io.imem_ahb.remapAddress(addr => addr.resize(imem.ahblite3Cfg.addressWidth))
        dmem.io.port1 <> ahblite3corssbar.io.dmem_ahb.remapAddress(addr => addr.resize(dmem.ahblite3Cfg.addressWidth))
        uartdbg.io.load_imem <> io.load_imem
        uartdbg.io.uart.rxd <> io.uart0.rxd

        Peripherals (
            cpu_rst = cpu_rst,
            uart_en = ~uartdbg.io.downloading,
            uartdbgrst_req = uartdbg.io.downloading,
            external_interrupt = cpu_rst_area.core.io.external_interrupt,
            timer_interrupt = cpu_rst_area.core.io.timer_interrupt,
            software_interrupt = cpu_rst_area.core.io.software_interrupt,
            _uart0 = io.uart0,
            _pwm0 = io.pwm0,
            _gpio = io.gpio,
            ahb2apb_ahb = ahblite3corssbar.io.ahb2apb_ahb
        )
    }
}

object ArtySoCMain{
    def main(args: Array[String]) {
        // FIXME
        if (args.length > 0) {
            //AddrMapping.INSTR_RAM_ADDR_WIDTH = args(0).toInt
            println("Generate with INSTR_RAM_ADDR_WIDTH = " + args(0))
        }
        AppleRISCVCfg.USE_RV32M    = true
        AppleRISCVCfg.USE_BPU      = false
        CsrCfg.USE_MHPMC3          = true
        CsrCfg.USE_MHPMC4          = true
        SpinalVerilog(InOutWrapper(ArtySoC()))
    }
}