///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: Plic
//
// Author: Heqing Huang
// Date Created: 04/19/2021
//
// ================== Description ==================
//
// Platform Level Interrupt Controller. Compatible with SiFive Freedom E310 SoC
//
//
//
// Address    Width   Attr.   Description Notes                            Implemented
// 0x0C000004 4B      RW      source 1 priority                                N
// 0x0C000008 4B      RW      source 2 priority                                N
// ...                                                                         N
// 0x0C0000CC 4B      RW      source 51 priority                               N
// 0x0C001000 4B      RO      Start of pending array                           Y
// 0x0C001004 4B      RO      Last word of pending array                       Y
// 0x0C002000 4B      RW      Start Hart 0 M-Mode interrupt enables            Y
// 0x0C002004 4B      RW      End Hart 0 M-Mode interrupt enables              Y
// 0x0C200000 4B      RW      Hart 0 M-Mode priority threshold                 N
// 0x0C200004 4B      RW      Hart 0 M-Mode claim/complete                     N
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC.ip

import AppleRISCVSoC.bus._
import spinal.core._
import spinal.lib._


case class Plic(sibCfg: SibConfig) extends Component {

  val io = new Bundle {
    val plic_sib     = slave(Sib(sibCfg))
    val plic_irq_in  = in Bits(64 bits)
    val external_irq = out Bool
  }
  noIoPrefix()

  val busCtrl  = SibSlaveFactory(io.plic_sib)


  // 0x0C001000 4B      RO      Start of pending array
  // 0x0C001004 4B      RO      Last word of pending array
  val pending1 = busCtrl.createReadOnly(Bits(32 bits), 0x1000, 0, "PLIC Interrupt Pending Register 1") init 0
  val pending2 = busCtrl.createReadOnly(Bits(32 bits), 0x1004, 0, "PLIC Interrupt Pending Register 2") init 0


  // 0x0C002000 4B      RW      Start Hart 0 M-Mode interrupt enables
  // 0x0C002004 4B      RW      End Hart 0 M-Mode interrupt enables
  val enable1 = busCtrl.createReadAndWrite(Bits(31 bits), 0x2000, 1, "PLIC Interrupt Enable Register 1") init 0
  val enable2 = busCtrl.createReadAndWrite(Bits(32 bits), 0x2004, 0, "PLIC Interrupt Enable Register 2") init 0


  // Interrupt logic
  pending1(0) := False  // Non-existent global interrupt 0 is hardwired to zero
  pending1(31 downto 1) := io.plic_irq_in(31 downto 1) & enable1
  pending2 := io.plic_irq_in(63 downto 32) & enable2

  io.external_irq := pending1.orR | pending2.orR
}
