///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: SibUart
//
// Author: Heqing Huang
// Date Created: 04/22/2021
// Revision V2: 05/10/2021
//
// ================== Description ==================
//
// Sib MM Uart Controller. Compatible with SiFive Freedom E310 SoC
//
// • 8-N-1 and 8-N-2 formats: 8 data bits, no parity bit, 1 start bit, 1 or 2 stop bits
// • 8-entry transmit and receive FIFO buffers with programmable watermark interrupts
// • 15 x Rx oversampling (instead of the original 16 from Freedom)
//
//
//  ---   UART Register Offsets ---
// Offset   Name      Description
// 0x000    txdata    Transmit data register
// 0x004    rxdata    Receive data register
// 0x008    txctrl    Transmit control register
// 0x00C    rxctrl    Receive control register
// 0x010    ie        UART interrupt enable
// 0x014    ip        UART Interrupt pending
// 0x018    div       Baud rate divisor
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC.ip

import AppleRISCVSoC.bus._
import spinal.core._
import spinal.lib._
import spinal.lib.com.uart.UartParityType._
import spinal.lib.com.uart.UartStopType._
import spinal.lib.com.uart._

object SibUartCfg {
  val uartCfg = UartCtrlGenerics(
    dataWidthMax      = 8,
    clockDividerWidth = 20,
    preSamplingSize   = 2,
    samplingSize      = 9,
    postSamplingSize  = 4
  )
  val rxFifoDepth = 16
  val txFifoDepth = 8
}

case class SibUart(sibCfg: SibConfig) extends Component {

  noIoPrefix()

  val io = new Bundle{
    val uart_sib = slave(Sib(sibCfg))
    val uart = master(Uart())
    val txwm = out Bool
    val rxwm = out Bool
  }

  val busCtrl = SibSlaveFactory(io.uart_sib)
  val uartCtrl = new UartCtrl(SibUartCfg.uartCfg)

  // 0x000    txdata    Transmit data register
  val dataType = Bits(SibUartCfg.uartCfg.dataWidthMax bits)
  val (tx_data, tx_avail) = busCtrl.createAndDriveFlow(dataType, 0x000, 0).queueWithAvailability(SibUartCfg.txFifoDepth)
  tx_data >> uartCtrl.io.write
  val tx_full = (tx_avail === 0)
  busCtrl.read(tx_full, 0x0000, 31, "Transmit FIFO full")

  // 0x004    rxdata    Receive data register
  val (rx_data, rx_occup) = uartCtrl.io.read.queueWithOccupancy(SibUartCfg.rxFifoDepth)
  busCtrl.readStreamNonBlocking(rx_data, address = 0x004, validBitOffset = 30, payloadBitOffset = 0)
  val rx_empty = (rx_occup === 0)
  busCtrl.read(rx_empty, 0x0004, 31, "Receive FIFO empty")

  // 0x008    txctrl    Transmit control register
  val txen  = busCtrl.createReadAndWrite(Bool, 0x008, 0, "Transmit enable") init False
  val nstop = busCtrl.createReadAndWrite(Bool, 0x008, 1, "Number of Stop bits") init False
  val txcnt = busCtrl.createReadAndWrite(UInt(3 bits), 0x008, 16, "Transmit water mark level") init 0

  // 0x008    rxctrl    Transmit control register
  val rxen  = busCtrl.createReadAndWrite(Bool, 0x00C, 0, "Receive enable") init False
  val rxcnt = busCtrl.createReadAndWrite(UInt(3 bits), 0x00C, 16, "Receive water mark level") init 0

  // 0x010    ie        UART interrupt enable
  val txwmen  = busCtrl.createReadAndWrite(Bool, 0x010, 0, "Transmit watermark interrupt enable") init False
  val rxwmen  = busCtrl.createReadAndWrite(Bool, 0x010, 1, "Receive watermark interrupt enable") init False

  // 0x014    ip        UART Interrupt pending
  val txwm_int = (tx_avail < txcnt) & txwmen
  val rxwm_int = (rx_occup > rxcnt) & rxwmen
  busCtrl.read(txwm_int, 0x014, 0, "Transmit watermark interrupt pending")
  busCtrl.read(rxwm_int, 0x014, 1, "Receive watermark interrupt pending")

  // 0x018    div       Baud rate divisor
  val div = busCtrl.createReadAndWrite(UInt(16 bits), 0x018, 0, "Baud rate divisor")
  // Note: div = Clk Freq / Sample Size / Baud Rate

  // Uart Config
  uartCtrl.io.config.frame.dataLength := 7
  uartCtrl.io.config.frame.stop       := Mux(nstop, ONE, TWO)
  uartCtrl.io.config.frame.parity     := NONE
  uartCtrl.io.config.clockDivider     := 0

  // Uart RX/TX control
  uartCtrl.io.writeBreak := ~txen
  uartCtrl.io.uart.rxd   := io.uart.rxd | ~rxen
  io.uart.txd            := uartCtrl.io.uart.txd | ~txen

  // Interrupt
  io.txwm := txwm_int
  io.rxwm := rxwm_int

}