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
//
// ================== Description ==================
//
// Define basic parameters for the SOC
//
///////////////////////////////////////////////////////////////////////////////////////////////////


package soc

import bus.sib._

class SOC_PARAM {
  val NAME = "apple-riscv-soc"

  val XLEN = 32
  val ADDR_WIDTH = XLEN
  val DATA_WIDTH = XLEN

  val CLIC_TIMER_WIDTH = 64
  val TIMER_TIMER_WIDTH = 64

  ////////////////////////////////////////////////////////////////////////////
  //                              Bus Address                               //
  ////////////////////////////////////////////////////////////////////////////


  val INSTR_RAM_DATA_WIDTH = DATA_WIDTH
  val INSTR_RAM_ADDR_WIDTH = 16                // 64KB Instruction RAM for now
  val DATA_RAM_DATA_WIDTH  = DATA_WIDTH
  val DATA_RAM_ADDR_WIDTH  = 16                // 64KB Data RAM for now

  val SIB_CPU_LO  = Integer.parseInt("00000000", 16)
  val SIB_CPU_HI  = Integer.parseInt("7FFFFFFF", 16)  // Integer can only hold upto 7FFFFFFFF

  val SIB_IMEM_LO = Integer.parseInt("00000000", 16)
  val SIB_IMEM_HI = Integer.parseInt("00FFFFFF", 16)

  val SIB_DMEM_LO = Integer.parseInt("01000000", 16)
  val SIB_DMEM_HI = Integer.parseInt("01FFFFFF", 16)

  val SIB_CLIC_LO = Integer.parseInt("02000000", 16)
  val SIB_CLIC_HI = Integer.parseInt("02000FFF", 16)
  val CLIC_ADDR_WIDTH = 12

  val SIB_PLIC_LO = Integer.parseInt("02001000", 16)
  val SIB_PLIC_HI = Integer.parseInt("02001FFF", 16)
  val PLIC_ADDR_WIDTH = 12

  val SIB_PERIP_HOST_LO = Integer.parseInt("02002000", 16)
  val SIB_PERIP_HOST_HI = Integer.parseInt("02004FFF", 16)
  val PERIP_HOST_ADDR_WIDTH = 16

  val SIB_TIMER_LO  = Integer.parseInt("2000", 16)
  val SIB_TIMER_HI  = Integer.parseInt("2FFF", 16)
  val TIMER_ADDR_WIDTH = 12

  val SIB_UART_LO = Integer.parseInt("3000", 16)
  val SIB_UART_HI = Integer.parseInt("3FFF", 16)
  val UART_ADDR_WIDTH = 12

  val GPIO_ADDR_WIDTH = 12
  val GPIO_WIDTH      = 32
  val SIB_GPIO0_LO = Integer.parseInt("4000", 16)
  val SIB_GPIO0_HI = Integer.parseInt("4FFF", 16)
  val SIB_GPIO1_LO = Integer.parseInt("5000", 16)
  val SIB_GPIO1_HI = Integer.parseInt("5FFF", 16)


  // ========================== //
  //       SIB Bus Config       //
  // ========================== //
  val imemSibCfg = SibConfig(
    addressWidth = INSTR_RAM_ADDR_WIDTH,
    dataWidth    = XLEN,
    addr_lo      = SIB_IMEM_LO,
    addr_hi      = SIB_IMEM_HI
  )

  val dmemSibCfg = SibConfig(
    addressWidth = DATA_RAM_ADDR_WIDTH,
    dataWidth    = XLEN,
    addr_lo      = SIB_DMEM_LO,
    addr_hi      = SIB_DMEM_HI
  )

  val cpuSibCfg = SibConfig(
    addressWidth = XLEN,
    dataWidth    = XLEN,
    addr_lo      = SIB_CPU_LO,
    addr_hi      = SIB_CPU_HI
  )

  val clicSibCfg = SibConfig(
    addressWidth = CLIC_ADDR_WIDTH,
    dataWidth    = XLEN,
    addr_lo      = SIB_CLIC_LO,
    addr_hi      = SIB_CLIC_HI
  )

  val plicSibCfg = SibConfig(
    addressWidth = PLIC_ADDR_WIDTH,
    dataWidth    = XLEN,
    addr_lo      = SIB_PLIC_LO,
    addr_hi      = SIB_PLIC_HI
  )

  val peripHostSibCfg = SibConfig(
    addressWidth = PERIP_HOST_ADDR_WIDTH,
    dataWidth    = XLEN,
    addr_lo      = SIB_PERIP_HOST_LO,
    addr_hi      = SIB_PERIP_HOST_HI
  )

  val timerSibCfg = SibConfig(
    addressWidth = TIMER_ADDR_WIDTH,
    dataWidth    = XLEN,
    addr_lo      = SIB_TIMER_LO,
    addr_hi      = SIB_TIMER_HI
  )

  val uartSibCfg = SibConfig(
    addressWidth = UART_ADDR_WIDTH,
    dataWidth    = XLEN,
    addr_lo      = SIB_UART_LO,
    addr_hi      = SIB_UART_HI
  )

  val gpio0SibCfg = SibConfig(
    addressWidth = GPIO_ADDR_WIDTH,
    dataWidth    = XLEN,
    addr_lo      = SIB_GPIO0_LO,
    addr_hi      = SIB_GPIO0_HI
  )

  val gpio1SibCfg = SibConfig(
    addressWidth = GPIO_ADDR_WIDTH,
    dataWidth    = XLEN,
    addr_lo      = SIB_GPIO1_LO,
    addr_hi      = SIB_GPIO1_HI
  )
}


