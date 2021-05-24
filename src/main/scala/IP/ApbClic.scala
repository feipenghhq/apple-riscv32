///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: Clic
//
// Author: Heqing Huang
// Date Created: 04/19/2021
// Revision 1: 05/10/2021
// Revision 2: 05/23/2021
//
// ================== Description ==================
//
// Core Level Interrupt Controller. Compatible with SiFive Freedom E310 SoC
//
// The CLINT block holds memory-mapped control and status registers associated with software
// and timer interrupts.
//
//   ---   CLINT Register Map   ---
// Address      Width   Attr.   Description Notes
// 0x02000000   4B      RW      msip for hart 0 MSIP Registers
// 0x02004000   8B      RW      mtimecmp for hart 0 Timer compare register
// 0x0200BFF8   8B      RO      mtime Timer register
// * Other address are all reserved
//
// Revision 1:
//  - Match registers with SiFive Freedom E310 SoC
//
// Revision 2:
//  - Use APB as bus interface
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package IP

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._

case class ApbClic(apbCfg: Apb3Config) extends Component {

  val io = new Bundle {
    val apb          = slave(Apb3(apbCfg))
    val software_irq = out Bool
    val timer_irq    = out Bool
  }
  noIoPrefix()

  val busCtrl  = Apb3SlaveFactory(io.apb)

  // 0x02000000   4B      RW      msip for hart 0 MSIP Registers
  val msip = busCtrl.createReadAndWrite(Bool, 0, 0, "msip for hart 0 MSIP Registers") init False

  // 0x02004000   8B      RW      mtimecmp for hart 0 Timer compare register
  val mtimecmp = busCtrl.createWriteAndReadMultiWord(UInt(64 bits), 0x4000, "mtimecmp for hart 0 Timer compare register") init 0

  // 0x0200BFF8   8B      RO      mtime Timer register
  val mtime = busCtrl.createWriteAndReadMultiWord(UInt(64 bits), 0xBFF8, "mtime Timer register") init 0

  // == timer logic == //
  mtime := mtime + 1

  // == interrupt generation logic == //
  io.software_irq := msip
  io.timer_irq    := (mtime >= mtimecmp) & (mtimecmp =/= 0)
}
