///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: UartDebug
//
// Author: Heqing Huang
// Date Created: 05/02/2021
// Revision 1: 05/23/2021
//
// ================== Description ==================
//
// Uart Debug module.
//
// 1. Download Instruction into Instruction RAM.
//
// Revision 1:
//  - Changed to Ahblite3 bus
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package IP

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.ahblite.AhbLite3._
import spinal.lib.bus.amba3.ahblite._
import spinal.lib.com.uart.UartParityType._
import spinal.lib.com.uart.UartStopType._
import spinal.lib.com.uart._
import spinal.lib.fsm._


/**
 * uart debug logic.
 */
case class UartDebug(ahblite3Cfg: AhbLite3Config, baudrate: Int) extends Component {

  val io = new Bundle {
    val uart = master(Uart())
    val ahblite3 = master(AhbLite3Master(ahblite3Cfg))
    val load_imem = in Bool
    val downloading = out Bool
  }

  val uartCfg = UartCtrlGenerics(
    dataWidthMax = 8,
    clockDividerWidth = 8,
    preSamplingSize = 1,
    samplingSize = 5,
    postSamplingSize = 2,
    ctsGen = false,
    rtsGen = false
  )

  val uart = new UartCtrl(uartCfg)
  uart.io.uart <> io.uart
  // baudrate = Fclk / rxSamplePerBit / clockDivider
  // clockDivider = Fclk / rxSamplePerBit / baudrate
  uart.io.config.clockDivider := (clockDomain.frequency.getValue / uartCfg.rxSamplePerBit / baudrate).toInt
  uart.io.config.frame.parity := NONE
  uart.io.config.frame.stop := ONE
  uart.io.config.frame.dataLength := 7

  val writeCtrl = new Area {
    // tie-off the write port as we are only receiving data from uart
    uart.io.write.valid := False
    uart.io.writeBreak := False
    uart.io.write.payload := 0
  }

  val download = new Area {

    val read4ByteFsm = new StateMachine {
      val readData = Reg(Bits(32 bits))
      val captured = False

      val idle = new State with EntryPoint
      val getByte0 = new State
      val getByte1 = new State
      val getByte2 = new State
      val getByte3 = new State

      idle.whenIsActive {
        when(uart.io.read.valid) {
          readData(7 downto 0) := uart.io.read.payload
          goto(getByte0)
        }
      }

      getByte0.whenIsActive {
        when(uart.io.read.valid) {
          readData(15 downto 8) := uart.io.read.payload
          goto(getByte1)
        }
      }
      getByte1.whenIsActive {
        when(uart.io.read.valid) {
          readData(23 downto 16) := uart.io.read.payload
          goto(getByte2)
        }
      }

      getByte2.whenIsActive {
        when(uart.io.read.valid) {
          readData(31 downto 24) := uart.io.read.payload
          goto(getByte3)
        }
      }

      getByte3.whenIsActive {
        goto(idle)
        captured := True
      }
    }

    val startSignal = read4ByteFsm.readData === B"32'hFFFFFFFF"
    val stopSignal = read4ByteFsm.readData === B"32'hFFFFFFFE"

    val downloadCtrlFsm = new StateMachine {

      val addr = Reg(UInt(ahblite3Cfg.addressWidth bits)) init 0
      io.ahblite3.HTRANS := IDLE
      io.ahblite3.HWRITE := True
      io.ahblite3.HWDATA := read4ByteFsm.readData
      io.ahblite3.HADDR  := addr
      io.ahblite3.HMASTLOCK  := False
      io.ahblite3.HPROT(0)  := False      // Opcode fetch
      io.ahblite3.HPROT(1)  := True       // Privileged access (We only have machine mode right now)
      io.ahblite3.HPROT(2)  := False      // None Buffer-able
      io.ahblite3.HPROT(3)  := False      // None Cache-able
      io.ahblite3.HSIZE     := B"3'b010"  // Word
      io.ahblite3.HBURST    := B"3'b000"  // Single burst
      io.downloading := False

      val idle = new State with EntryPoint
      val downloading = new State

      idle.whenIsActive {
        addr := 0
        when(read4ByteFsm.captured & startSignal & io.load_imem) {
          goto(downloading)
        }
      }

      downloading.whenIsActive {
        io.downloading := True
        when(read4ByteFsm.captured && !stopSignal) {
          io.ahblite3.HTRANS := NONSEQ
          addr := addr + 4
        }
        when(read4ByteFsm.captured && stopSignal) {
          goto(idle)
        }
      }
    }
  }
}
