///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: uart2imem
//
// Author: Heqing Huang
// Date Created: 04/22/2021
//
// ================== Description ==================
//
// Uart RX to IMEM. Used to download instruction from uart to instruction mem
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package debug

import bus.sib._
import spinal.core._
import spinal.lib.com.uart._
import spinal.lib._
import spinal.lib.com.uart.UartParityType._
import spinal.lib.com.uart.UartStopType._
import spinal.lib.fsm.{EntryPoint, State, StateMachine}


/**
 * uart rx to imem logic.
 * Used to download instruction from uart to instruction mem
 */
case class uart2imem(sibConfig: SibConfig, buardrate: Int) extends Component {

  noIoPrefix()

  val io = new Bundle {
    val uart         = master(Uart())
    val imem_dbg_sib = master(Sib(sibConfig))
    val load_imem    = in Bool
    val downloading  = out Bool
  }

  val uartCfg = UartCtrlGenerics(
      dataWidthMax      = 8,
      clockDividerWidth = 8,
      preSamplingSize   = 1,
      samplingSize      = 5,
      postSamplingSize  = 2,
      ctsGen            = false,
      rtsGen            = false
  )

  val uart = new UartCtrl(uartCfg)
  uart.io.uart <> io.uart
  // baudrate = Fclk / rxSamplePerBit / clockDivider
  // clockDivider = Fclk / rxSamplePerBit / buardrate
  uart.io.config.clockDivider     := (clockDomain.frequency.getValue / uartCfg.rxSamplePerBit / buardrate).toInt
  uart.io.config.frame.parity     := NONE
  uart.io.config.frame.stop       := ONE
  uart.io.config.frame.dataLength := 7


  val writeCtrl = new Area {
    // tie-off the write port as we are only receiving data from uart
    uart.io.write.valid   := False
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
    val stopSignal  = read4ByteFsm.readData === B"32'hFFFFFFFE"

    val downloadCtrlFsm = new StateMachine {

      val addr = Reg(UInt(sibConfig.addressWidth bits)) init 0
      io.imem_dbg_sib.sel    := False
      io.imem_dbg_sib.enable := True
      io.imem_dbg_sib.write  := True
      io.imem_dbg_sib.wdata  := read4ByteFsm.readData
      io.imem_dbg_sib.mask   := B"1111"
      io.imem_dbg_sib.addr   := addr
      io.downloading         := False

      val idle        = new State with EntryPoint
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
          io.imem_dbg_sib.sel := True
          addr := addr + 4
        }
        when(read4ByteFsm.captured && stopSignal) {
          goto(idle)
        }
      }
    }
  }
}
