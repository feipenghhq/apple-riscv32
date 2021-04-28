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
import spinal.lib.bus.misc.{BusSlaveFactoryDelayed, BusSlaveFactoryElement, SingleMapping}

object SibSlaveFactory {
  def apply(bus: Sib) = new SibSlaveFactory(bus)
}

class SibSlaveFactory(bus: Sib) extends BusSlaveFactoryDelayed {

  //val askWrite = bus.sel && bus.enable && bus.write
  //val askRead  = bus.sel && bus.enable && !bus.write
  val doWrite  = bus.sel && bus.enable && bus.ready && bus.write
  val doRead   = bus.sel && bus.enable && bus.ready && !bus.write
  val rdata    = B(0, bus.config.dataWidth bits)

  bus.resp  := False
  bus.ready := True
  bus.rdata := RegNext(rdata)

  override def writeHalt(): Unit = bus.ready := False
  override def readHalt(): Unit = bus.ready := False

  override def writeAddress(): UInt = bus.addr
  override def readAddress(): UInt = bus.addr

  override def busDataWidth: Int = bus.config.dataWidth
  override def wordAddressInc: Int = bus.config.bytePerWord

  override def build(): Unit = {
    super.doNonStopWrite(bus.wdata)

    def doMappedElements(jobs: Seq[BusSlaveFactoryElement]): Unit = super.doMappedElements(
      jobs = jobs,
      // askWrite = askWrite,
      // askRead  = askRead,
      askWrite = False,
      askRead  = False,
      doWrite  = doWrite,
      doRead   = doRead,
      writeData = bus.wdata,
      readData  = rdata
    )

    switch(bus.addr) {
      for ((address, jobs) <- elementsPerAddress if address.isInstanceOf[SingleMapping]) {
        is(address.asInstanceOf[SingleMapping].address) {
          doMappedElements(jobs)
          bus.resp := True
        }
      }
    }

    for ((address, jobs) <- elementsPerAddress if !address.isInstanceOf[SingleMapping]) {
      when(address.hit(bus.addr)) {
        doMappedElements(jobs)
        bus.resp := True
      }
    }
  }


}
