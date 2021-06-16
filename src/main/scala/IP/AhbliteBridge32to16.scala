///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: Generator
//
// Author: Heqing Huang
// Date Created: 06/06/2021
//
// ================== Description ==================
//
// Ahblite bridge 32 bit to 16 bit
//
// Support only non-seq transaction for now
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package IP

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.ahblite._
import spinal.lib.fsm._

case class AhbliteBridge32to16(ahblite3Cfg: AhbLite3Config) extends Component {
  require(ahblite3Cfg.dataWidth == 32)
  val ahblite3Cfg2 = AhbLite3Config(ahblite3Cfg.addressWidth, 16)
  val io = new Bundle {
    val ahb_in = slave(AhbLite3(ahblite3Cfg))
    val ahb_out = master(AhbLite3(ahblite3Cfg2))
  }

  val ctrl = new StateMachine {
    val IDLE = new State with EntryPoint
    val ACCESS1 = new State
    val ACCESS2 = new State

    val addr_ff = Reg(UInt(ahblite3Cfg.addressWidth bits))
    val rdata_15_0 = Reg(Bits(16 bits))
    val wdata_31_16 = Reg(Bits(16 bits))
    val hwrite_ff = RegInit(False)
    val sizeByte_ff = Reg(Bool)
    val sizeWord_ff = Reg(Bool)
    val rdataSel = Reg(Bool)

    val mask = io.ahb_in.writeMask()
    val sizeWord = io.ahb_in.HSIZE === 2
    val sizeByte = io.ahb_in.HSIZE === 0

    io.ahb_out.HSEL := False
    io.ahb_out.HTRANS := io.ahb_in.HTRANS
    io.ahb_out.HWRITE := io.ahb_in.HWRITE
    io.ahb_out.HBURST := io.ahb_in.HBURST
    io.ahb_out.HPROT  := io.ahb_in.HPROT
    io.ahb_out.HADDR  := io.ahb_in.HADDR
    io.ahb_out.HWDATA := io.ahb_in.HWDATA(15 downto 0)
    io.ahb_out.HSIZE  := B"2'b00" ## ~sizeByte
    io.ahb_out.HREADY := True
    io.ahb_out.HMASTLOCK := False
    io.ahb_in.HREADYOUT := io.ahb_out.HREADY
    io.ahb_in.HRESP := True
    io.ahb_in.HRDATA :=  rdataSel ? (rdata_15_0 ## io.ahb_out.HRDATA) | (io.ahb_out.HRDATA ## rdata_15_0)

    def addrPhase(): Unit = {
      addr_ff := io.ahb_in.HADDR + 2  // store the next address
      hwrite_ff := io.ahb_in.HWRITE
      sizeByte_ff := sizeByte
      sizeWord_ff := sizeWord
      io.ahb_out.HSEL := True
      io.ahb_out.HTRANS := B"2'b10"
      io.ahb_out.HMASTLOCK := io.ahb_in.HMASTLOCK
      // For byte or half-word access, the address is aligned to the specific word.
      // If HADDR(1) == 1 then the byte/half-word is in the upper half so we need to adjust the data position
      rdataSel := io.ahb_in.HADDR(1)
    }

    IDLE.whenIsActive{
      when(io.ahb_in.HSEL & io.ahb_out.HREADY) {
        addrPhase()
        goto(ACCESS1)
      }
    }

    ACCESS1.whenIsActive {
      io.ahb_out.HSEL   := sizeWord_ff
      io.ahb_out.HTRANS := B"2'b10"
      io.ahb_out.HADDR  := addr_ff
      io.ahb_out.HWRITE := hwrite_ff
      io.ahb_out.HSIZE  := B"2'b00" ## ~sizeByte_ff
      io.ahb_out.HWDATA := io.ahb_in.HWDATA(15 downto 0)
      io.ahb_in.HREADYOUT := False
      rdata_15_0 := io.ahb_out.HRDATA
      wdata_31_16 := io.ahb_in.HWDATA(31 downto 16)
      when(io.ahb_out.HREADY) {
        goto(ACCESS2)
      }
    }

    ACCESS2.whenIsActive {
      io.ahb_out.HWDATA := wdata_31_16
      when(io.ahb_out.HREADY) {
        when(io.ahb_in.HSEL) {
          addrPhase()
          goto(ACCESS1)
        }.otherwise{
          goto(IDLE)
        }
      }
    }
  }

}
