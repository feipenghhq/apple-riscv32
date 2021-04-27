///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: gpio
//
// Author: Heqing Huang
// Date Created: 04/22/2021
//
// ================== Description ==================
//
// Sib MM Uart Controller
//
// Design referenced from the spinalHDL document website
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package ip

import bus.sib._
import spinal.core._
import spinal.lib._
import spinal.lib.com.uart._

case class UartCfg(
  uartCfg: UartCtrlGenerics = UartCtrlGenerics(),
  rxFifoDepth: Int = 8,
  txFifoDepth: Int = 8
)

case class SibUart(cfg: UartCfg, sibCfg: SibConfig) extends Component {

  noIoPrefix()

  val io = new Bundle{
    val uart_interrupt = out Bool
    val uart_sib = slave(Sib(sibCfg))
  }
  val uart = master(Uart())

  val busCtrl = SibSlaveFactory(io.uart_sib)

  // Instantiate an simple uart controller
  val uartCtrl = new UartCtrl(cfg.uartCfg)
  uart <> uartCtrl.io.uart
  uartCtrl.io.writeBreak := False

  val frameDoc = "Uart frame control. [0:2] data length. [3] Stop bits. 0 - one bit, 1 - two bits. " +
    "[4:5] Parity bits. 0 - None, 1 - Even, 2 - Odd"

  // ==  register == //

  // Uart Control
  val rx_avail_int_en = busCtrl.driveAndRead(Bool, 0x0, 0, "Uart rx avail interrupt enable") init True
  val rx_full_int_en  = busCtrl.driveAndRead(Bool, 0x0, 1, "Uart rx full interrupt enable") init True
  val rx_en  = busCtrl.driveAndRead(Bool, 0x0, 4, "Uart rx enable") init True
  val tx_en  = busCtrl.driveAndRead(Bool, 0x0, 5, "Uart tx enable") init True

  // Uart Config
  busCtrl.driveAndRead(uartCtrl.io.config.frame       , 0x4, 0, frameDoc)
  busCtrl.driveAndRead(uartCtrl.io.config.clockDivider, 0x8, 0, "Uart Clock divider")

  // Uart TX/RX Data
  val (txQueue, txAvail) = busCtrl.createAndDriveFlow(Bits(cfg.uartCfg.dataWidthMax bits), 0x8, 0).
    toStream.queueWithAvailability(cfg.txFifoDepth)
  txQueue >> uartCtrl.io.write
  val (rxQueue, rxOccup) = uartCtrl.io.read.queueWithOccupancy(cfg.rxFifoDepth)
  busCtrl.readStreamNonBlocking(rxQueue, address = 0xC, validBitOffset = 31, payloadBitOffset = 0)
  busCtrl.read(rxOccup, 0xC, 8, "Uart remaining RX data in FIFO")

  // == Uart Interrupt and Status == //
  val txFull  = (txAvail === 0)
  val rxFull  = (rxOccup === cfg.rxFifoDepth)
  val rxEmpty = (rxOccup === 0)
  val rxAvail = ~rxEmpty
  busCtrl.read(rxAvail,0x10, 0, "Uart rx avail interrupt")
  busCtrl.read(rxFull  ,0x10, 1, "Uart rx full interrupt")
  busCtrl.read(txFull  ,0x10, 4, "Uart tx FIFO full")
  busCtrl.read(rxEmpty ,0x10, 5, "Uart rx FIFO empty")
  io.uart_interrupt := rxAvail | rxFull
}