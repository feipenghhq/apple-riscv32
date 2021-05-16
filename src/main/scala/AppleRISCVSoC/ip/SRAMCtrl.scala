///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: SibSRAM
//
// Author: Heqing Huang
// Date Created: 05/14/2021
//
// ================== Description ==================
//
// DE2 SRAM Model
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC.ip

import AppleRISCVSoC.bus._
import spinal.core._
import spinal.lib._
import spinal.lib.fsm._
import spinal.lib.io._

case class SRAMIO() extends Bundle with IMasterSlave{
  val addr = UInt(18 bits)
  val data = TriStateArray(16 bits)
  val we_n = Bool
  val oe_n = Bool
  val ub_n = Bool
  val lb_n = Bool
  val ce_n = Bool
  override def asMaster(): Unit = {
    out(addr, we_n, oe_n, ub_n, lb_n, ce_n)
    master(data)
  }
}

case class SRAMCtrl(sibCfg: SibConfig) extends Component {

  val io = new Bundle{
    val sram_sib = slave(Sib(sibCfg))
    val sram  = master(SRAMIO())
  }
  noIoPrefix()

  val ctrl = new StateMachine {
    val IDLE   = new State with EntryPoint
    val ACCESS = new State
    val DONE   = new State

    val write_ff = RegInit(False)
    val mask_ff  = Reg(Bits(4 bits))
    val addr_ff  = Reg(UInt(18 bits))
    val wdata_ff = Reg(Bits(32 bits))
    val rdatalo_ff = Reg(Bits(16 bits))

    def newReq(): Any = new Area{
      write_ff := io.sram_sib.write
      mask_ff  := io.sram_sib.mask
      // ignore the lower two bit as we need to align the address to 4 byte and
      // twick the lb/ub_n to enable byte access
      addr_ff  := io.sram_sib.addr(18 downto 2) @@ False
      wdata_ff := io.sram_sib.wdata
      io.sram_sib.ready := False
    }

    // Default value
    io.sram_sib.resp  := True
    io.sram_sib.ready := True
    io.sram_sib.rdata := io.sram.data.read ## rdatalo_ff

    io.sram.we_n := True
    io.sram.oe_n := True
    io.sram.ce_n := True
    io.sram.lb_n := True
    io.sram.ub_n := True
    io.sram.addr := addr_ff
    io.sram.data.write := wdata_ff(15 downto 0)
    io.sram.data.writeEnable.setAllTo(write_ff)

    IDLE.whenIsActive {
      when(io.sram_sib.sel) {
        newReq()
        goto(ACCESS)
      }
    }

    ACCESS.whenIsActive {
      io.sram.ce_n := False
      io.sram.lb_n := ~mask_ff(0)
      io.sram.ub_n := ~mask_ff(1)
      io.sram.we_n := ~write_ff
      io.sram.oe_n := write_ff
      io.sram.addr := addr_ff
      io.sram.data.write := wdata_ff(15 downto 0)
      // Advance the address by 1, this is the next address in the sram
      // which is 2 byte aligned.
      addr_ff      := addr_ff + 1
      rdatalo_ff   := io.sram.data.read
      io.sram_sib.ready := True
      goto(DONE)
    }

    DONE.whenIsActive {
      io.sram.ce_n := False
      io.sram.lb_n := ~mask_ff(2)
      io.sram.ub_n := ~mask_ff(3)
      io.sram.we_n := ~write_ff
      io.sram.oe_n := write_ff
      io.sram.addr := addr_ff
      io.sram.data.write := wdata_ff(31 downto 16)
      when(io.sram_sib.sel) {
        newReq()
        goto(ACCESS)
      }.otherwise{
        goto(IDLE)
      }
    }
  }
}
