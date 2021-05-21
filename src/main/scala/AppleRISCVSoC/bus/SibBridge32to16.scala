///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: SibBridge32to16
//
// Author: Heqing Huang
// Date Created: 05/19/2021
//
// ================== Description ==================
//
// Sib Bridge that bridging between 32 bit data and 16 bit data
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC.bus

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._

case class SibBridge32to16(inCfg: SibConfig) extends Component {
  require(inCfg.dataWidth==32)
  val outCfg = SibConfig(inCfg.addressWidth, 16, inCfg.addr_lo, inCfg.addr_hi)
  val io = new Bundle{
    val sib_in   = slave(Sib(inCfg))
    val sib_out  = master(Sib(outCfg))
  }
  noIoPrefix()

  val ctrl = new StateMachine {
    val IDLE = new State with EntryPoint
    val ACCESS = new State

    val rdatalo = RegNext(io.sib_out.rdata)

    io.sib_out.sel    := io.sib_in.sel
    io.sib_out.enable := io.sib_in.enable
    io.sib_out.write  := io.sib_in.write
    io.sib_out.mask   := io.sib_in.mask(1 downto 0)
    io.sib_out.wdata  := io.sib_in.wdata(15 downto 0)
    io.sib_out.addr   := io.sib_in.addr
    io.sib_in.ready   := True

    io.sib_in.rdata   := io.sib_out.rdata ## rdatalo
    io.sib_in.resp    := io.sib_out.resp

    IDLE.whenIsActive {
      io.sib_in.ready   := ~io.sib_in.sel
      when (io.sib_in.sel) {
        goto(ACCESS)
      }
    }

    ACCESS.whenIsActive {
      io.sib_out.mask   := io.sib_in.mask(3 downto 2)
      io.sib_out.wdata  := io.sib_in.wdata(31 downto 16)
      io.sib_out.addr   := io.sib_in.addr + 2
      io.sib_in.ready   := True
      goto(IDLE)
    }


  }

}
