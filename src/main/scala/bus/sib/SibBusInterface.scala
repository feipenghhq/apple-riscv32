///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Author: Heqing Huang
// Date Created: 04/22/2021
//
// ================== Description ==================
//
// Sib Slave Factory
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package bus.sib

import spinal.core._
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.bus.regif._

case class SibBusInterface(bus: Sib, sizeMap: SizeMapping, readSync: Boolean = true, regPre: String = "")
                          (implicit moduleName: ClassName) extends  BusIf{

  val askWrite = bus.sel && bus.enable && bus.write
  val askRead  = bus.sel && bus.enable && !bus.write
  val doWrite  = bus.sel && bus.enable && bus.ready && bus.write
  val doRead   = bus.sel && bus.enable && bus.ready && !bus.write

  override def getModuleName: String = moduleName.name
  override def writeHalt(): Unit = bus.ready := False
  override def readHalt(): Unit = bus.ready := False
  override def writeAddress(): UInt = bus.addr
  override def readAddress(): UInt = bus.addr
  override def busDataWidth: Int = bus.config.dataWidth

  val readData  = Bits(bus.config.dataWidth bits)
  val writeData = bus.wdata
  val readError = False

  if(readSync) {
    readData.setAsReg() init 0
  } else {
    readData := 0
  }

  bus.resp  := ~readError
  bus.ready := True
  bus.rdata := readData
}
