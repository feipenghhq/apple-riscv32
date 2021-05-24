///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: SOC_PARAM
//
// Author: Heqing Huang
// Date Created: 04/19/2021
// Revision V1: 05/23/2021
//
// ================== Description ==================
//
// SoC Address Mapping
//
///////////////////////////////////////////////////////////////////////////////////////////////////


package AppleRISCVSoC

import AppleRISCV.AppleRISCVCfg
import spinal.core.log2Up
import spinal.lib.bus.amba3.ahblite.AhbLite3Config
import spinal.lib.bus.amba3.apb.Apb3Config

case class AddressMap (var base: Long, var top: Long) {
  def size(): Long = top - base + 1
  def addrWidth(): Int = log2Up(size())
  def sizeMapping(): (BigInt, BigInt) = (BigInt(base), BigInt(size()))
  def update(_base: Long, _top: Long): Unit = {
    this.base = _base
    this.top  = _top
  }
}

// Trying to be compatible with Freedom E310
object SoCAddrMapping {

  // Full Memory Space
  val FULL_BASE = 0x00000000L
  val FULL_TOP  = 0xFFFFFFFFL
  val FULL = AddressMap(FULL_BASE, FULL_TOP)

  // ** Debug Address Space - Skip
  // ** On-Chip Non-Volatile Memory - Skip

  // ** On-Chip Peripherals
  val PERIP_BASE = 0x02000000L
  val PERIP_TOP  = 0x1FFFFFFFL
  val PERIP = AddressMap(PERIP_BASE, PERIP_TOP)

  // CLIC
  val CLIC_BASE = 0x02000000L
  val CLIC_TOP  = 0x0200FFFFL
  val CLIC = AddressMap(CLIC_BASE, CLIC_TOP)

  // PLIC
  val PLIC_BASE = 0x0C000000L
  val PLIC_TOP  = 0x0FFFFFFFL
  val PLIC = AddressMap(PLIC_BASE, PLIC_TOP)

  // Always-On (AON)
  val AON_BASE = 0x10000000L
  val AON_TOP  = 0x10007FFFL
  val AON = AddressMap(AON_BASE, AON_TOP)

  // GPIO
  val GPIO_BASE = 0x10012000L
  val GPIO_TOP  = 0x10012FFFL
  val GPIO = AddressMap(GPIO_BASE, GPIO_TOP)

  // UART0
  val UART0_BASE = 0x10013000L
  val UART0_TOP  = 0x10013FFFL
  val UART0 = AddressMap(UART0_BASE, UART0_TOP)

  // PWM0
  val PWM0_BASE = 0x10015000L
  val PWM0_TOP  = 0x10015FFFL
  val PWM0 = AddressMap(PWM0_BASE, PWM0_TOP)

  /*
  // Off-Chip Non-Volatile Memory
  // Notes: We use this part as the Dedicated Instruction RAM
  val QSPI0_BASE = 0x20000000L
  val QSPI0_TOP  = 0x3000FFFFL  // 64KB

  // On-Chip Volatile Memory
  // Notes: We use this part as the Dedicated Data RAM
  val DTIM_BASE  = 0x80000000L
  val DTIM_TOP   = 0x8000FFFFL  // 64KB
  */

  // Instruction Memory
  val IMEM_BASE = 0x20000000L
  val IMEM_TOP  = 0x2000FFFFL
  val IMEM = AddressMap(IMEM_BASE, IMEM_TOP)

  // Data memory
  val DMEM_BASE = 0x80000000L
  val DMEM_TOP  = 0x8000FFFFL
  val DMEM = AddressMap(DMEM_BASE, DMEM_TOP)
}

object AhbLite3Cfg {
  val ahblite3Cfg = AhbLite3Config(AppleRISCVCfg.XLEN, AppleRISCVCfg.XLEN)
  def imemAhblite3Cfg(): AhbLite3Config = AhbLite3Config(SoCAddrMapping.IMEM.addrWidth(), AppleRISCVCfg.XLEN)
  def dmemAhblite3Cfg(): AhbLite3Config = AhbLite3Config(SoCAddrMapping.DMEM.addrWidth(), AppleRISCVCfg.XLEN)
  def peripAhblite3Cfg(): AhbLite3Config = AhbLite3Config(SoCAddrMapping.PERIP.addrWidth(), AppleRISCVCfg.XLEN)
}

object ApbCfg {
  def peripApbCfg(): Apb3Config = Apb3Config(SoCAddrMapping.PERIP.addrWidth(), AppleRISCVCfg.XLEN)
  def clicApbCfg(): Apb3Config = Apb3Config(SoCAddrMapping.CLIC.addrWidth(), AppleRISCVCfg.XLEN)
  def plicApbCfg(): Apb3Config = Apb3Config(SoCAddrMapping.PLIC.addrWidth(), AppleRISCVCfg.XLEN)
  def aonApbCfg(): Apb3Config = Apb3Config(SoCAddrMapping.AON.addrWidth(), AppleRISCVCfg.XLEN)
  def gpioApbCfg(): Apb3Config = Apb3Config(SoCAddrMapping.GPIO.addrWidth(), AppleRISCVCfg.XLEN)
  def uart0ApbCfg(): Apb3Config = Apb3Config(SoCAddrMapping.UART0.addrWidth(), AppleRISCVCfg.XLEN)
  def pwm0ApbCfg(): Apb3Config = Apb3Config(SoCAddrMapping.PWM0.addrWidth(), AppleRISCVCfg.XLEN)
}