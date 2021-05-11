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
// Revision V2: 05/10/2021
//
// ================== Description ==================
//
// Configuration for the common module between different SoC
//
///////////////////////////////////////////////////////////////////////////////////////////////////


package AppleRISCVSoC

import bus._
import AppleRISCV._

// Trying to be compatible with Freedom E310
object SoCAddrMap {

  // ** Entire CPU
  val CPU_BASE = 0x00000000L
  val CPU_TOP  = 0xFFFFFFFFL

  // ** Debug Address Space - Skip
  // ** On-Chip Non-Volatile Memory - Skip

  // ** On-Chip Peripherals
  val PERIP_BASE = 0x02000000L
  val PERIP_TOP  = 0x1FFFFFFFL
  val PERIP_ADDR_WIDTH = 32

  // CLIC
  val CLIC_BASE = 0x02000000L
  val CLIC_TOP  = 0x0200FFFFL
  val CLIC_ADDR_WIDTH = 16

  // PLIC
  val PLIC_BASE = 0x0C000000L
  val PLIC_TOP  = 0x0FFFFFFFL
  val PLIC_ADDR_WIDTH = 28

  // Always-On (AON)
  val AON_BASE  = 0x10000000L
  val AON_TOP   = 0x10007FFFL
  val AON_ADDR_WIDTH = 15

  // GPIO0
  val GPIO0_BASE = 0x10012000L
  val GPIO0_TOP  = 0x10012FFFL
  val GPIO0_ADDR_WIDTH = 12

  // UART0
  val UART0_BASE = 0x10013000L
  val UART0_TOP  = 0x10013FFFL
  val UART0_ADDR_WIDTH = 12

  // Off-Chip Non-Volatile Memory
  // Notes: We use this part as the Dedicated Instruction RAM
  val QSPI0_BASE = 0x20000000L
  val QSPI0_TOP  = 0x3FFFFFFFL
  val QSPI0_ADDR_WIDTH = 32

  // On-Chip Volatile Memory
  val DTIM_BASE  = 0x80000000L
  val DTIM_TOP   = 0x80003FFFL
  val DTIM_ADDR_WIDTH = 14  // 16K Byte RAM
}

object PeripSibCfg {

  val peripSibCfg = SibConfig(
    addressWidth = SoCAddrMap.PERIP_ADDR_WIDTH,
    dataWidth    = AppleRISCVCfg.XLEN,
    addr_lo      = SoCAddrMap.PERIP_BASE,
    addr_hi      = SoCAddrMap.PERIP_TOP
  )

  val clicSibCfg = SibConfig(
    addressWidth = SoCAddrMap.CLIC_ADDR_WIDTH,
    dataWidth    = AppleRISCVCfg.XLEN,
    addr_lo      = SoCAddrMap.CLIC_BASE,
    addr_hi      = SoCAddrMap.CLIC_TOP
  )

  val plicSibCfg = SibConfig(
    addressWidth = SoCAddrMap.PLIC_ADDR_WIDTH,
    dataWidth    = AppleRISCVCfg.XLEN,
    addr_lo      = SoCAddrMap.PLIC_BASE,
    addr_hi      = SoCAddrMap.PLIC_TOP
  )

  // ** On-Chip Peripherals
  val aonSibCfg = SibConfig(
    addressWidth = SoCAddrMap.AON_ADDR_WIDTH,
    dataWidth    = AppleRISCVCfg.XLEN,
    addr_lo      = SoCAddrMap.AON_BASE,
    addr_hi      = SoCAddrMap.AON_TOP
  )

  val uart0SibCfg = SibConfig(
    addressWidth = SoCAddrMap.UART0_ADDR_WIDTH,
    dataWidth    = AppleRISCVCfg.XLEN,
    addr_lo      = SoCAddrMap.UART0_BASE,
    addr_hi      = SoCAddrMap.UART0_TOP
  )

  val gpio0SibCfg = SibConfig(
    addressWidth = SoCAddrMap.GPIO0_ADDR_WIDTH,
    dataWidth    = AppleRISCVCfg.XLEN,
    addr_lo      = SoCAddrMap.GPIO0_BASE,
    addr_hi      = SoCAddrMap.GPIO0_TOP
  )
}


