///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: SibSramCtrl
//
// Author: Heqing Huang
// Date Created: 06/15/2021
//
// ================== Description ==================
//
// AHB lite Cache
//
// Feature:
//  - Write Back
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package IP

import AppleRISCVSoC.AhbLite3Cfg
import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.ahblite.AhbLite3._
import spinal.lib.bus.amba3.ahblite._
import spinal.lib.fsm._
import scala.collection.mutable.ArrayBuffer

case class Ahblite3CacheSetAssociative(ahblite3Cfg: AhbLite3Config,
                                       cacheLineSize: Int,            // cache line size in byte
                                       setNum: Int,                   // number of set
                                       setSize: Int                   // size of each set
                                      ) extends Component {
  require(isPow2(setSize))
  require(isPow2(setNum))
  require(cacheLineSize >= 4)
  require(cacheLineSize % 4 == 0)

  val io = new Bundle {
    val cache_ahb = slave(AhbLite3(ahblite3Cfg))
    val mem_ahb = master(AhbLite3(ahblite3Cfg))
  }

  val wordCount = cacheLineSize / 4
  val cacheLineSizeBits = cacheLineSize * 8
  val byteAddrRange = 1 downto 0
  val wordAddrRange = log2Up(wordCount)+byteAddrRange.start downto byteAddrRange.start+1
  val idxAddrRange = log2Up(setSize)+wordAddrRange.start downto wordAddrRange.start+1
  val tagAddrRange = ahblite3Cfg.addressWidth-1 downto idxAddrRange.start+1
  val cacheLineAddrRange = ahblite3Cfg.addressWidth-1 downto wordAddrRange.start+1

  val tag: UInt = io.cache_ahb.HADDR(tagAddrRange)
  val setsIdx: UInt = io.cache_ahb.HADDR(idxAddrRange)
  val wordIdx: UInt = io.cache_ahb.HADDR(wordAddrRange)
  val cacheLineAddr: UInt = io.cache_ahb.HADDR(cacheLineAddrRange)

  // register the input information
  val newReq = Bool
  val tag_ff = RegNextWhen(tag, newReq)
  val setsIdx_ff = RegNextWhen(setsIdx, newReq)
  val wordIdx_ff = RegNextWhen(wordIdx, newReq)
  val cacheLineAddr_ff = RegNextWhen(cacheLineAddr, newReq)
  val hprot_ff = RegNextWhen(io.cache_ahb.HPROT, newReq)


  case class SetLogicBundle() extends Bundle {
    val fill = Bool
    val mask = Bits(cacheLineSize bits)
    val tag = UInt(tagAddrRange.size bits)
    val idx = UInt(idxAddrRange.size bits)
    val wdata = Bits(cacheLineSize * 8 bits)
    val updateTag = Bool

    val hit = Bool
    val vld = Bool
    val rdata = Bits(cacheLineSize * 8  bits)

    def defaultVal(): Unit = {
      fill := False
      mask  := 0
      tag   := 0
      idx   := 0
      wdata := 0
      updateTag := False
    }
  }

  val setPorts = Array.fill(setNum)(SetLogicBundle())

  /** Cache Set Logic  */
  def setsLogic(port: SetLogicBundle): Area = new Area {
    val tag_ram = Mem(UInt(tagAddrRange.size bits), setSize)
    val data_ram = Mem(Bits(cacheLineSizeBits bits), setSize)
    val valid = Reg(Bits(setSize bits)) init 0

    // Check tag
    val tag_out = tag_ram.readSync(port.idx)
    val valid_out = RegNext(valid(port.idx))
    port.hit := valid_out && (tag_out === port.tag)
    port.vld := valid_out

    // READ logic
    port.rdata := data_ram.readSync(port.idx)

    // Write logic
    data_ram.write(
      address = port.idx,
      data = port.wdata,
      enable = port.fill,
      mask = port.mask
    )

    tag_ram.write(
      address = port.idx,
      data = port.tag,
      enable = port.updateTag
    )

    when(port.updateTag) {
      valid(port.idx) := True
    }
  }

  /** Split the cache line into word */
  def cacheLineToWord(cacheLine: Bits) = {
    val wordNum = 1 << wordAddrRange.size
    val wordVec = Vec(Bits(32 bits), wordNum)
    for (i <- 0 until wordNum) {
      wordVec(i) := cacheLine(i*32 + 31 downto i*32)
    }
    wordVec
  }

  /**  memory access */
  def memAcc(write: Bool, addrInc: UInt): Unit = {
    val cacheLineAlignedByteAddr = cacheLineAddr_ff @@ U"0".resize(cacheLineAddrRange.end) // FIXME
    io.mem_ahb.HSEL := True
    io.mem_ahb.HWRITE := write
    io.mem_ahb.HADDR := cacheLineAlignedByteAddr + addrInc
    io.mem_ahb.HTRANS := NONSEQ
    io.mem_ahb.HPROT := hprot_ff
    io.mem_ahb.HSIZE := B"010"  // 4 byte
  }

  /** put the word to cache line */
  def wordToCacheLine(word: Bits) = {
    val wordNum = 1 << wordAddrRange.size
    val wordVec = Vec(Bits(32 bits), wordNum)
    for (i <- 0 until wordNum) {
      wordVec(i) := word
    }
    wordVec.reduce(_ ## _)
  }

  /** find the set to put the data */
  def findSet(vlds: Bits): Bits = {
    val inverted = ~vlds
    val masks = vlds.asUInt + 1
    val freeSpot = inverted & masks.asBits
    freeSpot
  }

  /** Instantiate Cache Sets */
  val sets = ArrayBuffer[Area]()
  for (i <- 0 until setNum) {
    sets.append(setsLogic(setPorts(i)))
    sets(i).setName("cacheSet" + i)
  }

  /** Cache Control StateMachine */
  val ctrlStateMachine = new StateMachine {
    val CIDLE = new State with EntryPoint
    val READ_CHECK = new State
    val WRITE_CHECK = new State
    val READ_MEMORY = new State
    val READ_WAIT_MEMORY = new State

    newReq := False

    io.cache_ahb.HREADYOUT := True
    io.cache_ahb.HRESP := False
    io.cache_ahb.HRDATA := 0

    io.mem_ahb.HSEL := False
    io.mem_ahb.HWRITE := False
    io.mem_ahb.HADDR := 0
    io.mem_ahb.HWDATA := 0
    io.mem_ahb.HTRANS := IDLE
    io.mem_ahb.HPROT := 0
    io.mem_ahb.HSIZE := 0
    io.mem_ahb.HBURST := 0
    io.mem_ahb.HMASTLOCK := False
    io.mem_ahb.HREADY := True

    val cacheLineHit = setPorts.map(_.hit).reduce(_ | _)
    val cacheLineData = MuxOH(setPorts.map(_.hit), setPorts.map(_.rdata))
    val cacheLineMask = Reg(Bits(cacheLineSize bits))
    val addrInc = Reg(UInt(wordAddrRange.size+2 bits)) init 0
    val accWordCnt = Reg(UInt(wordAddrRange.size+1 bits)) init 0
    //val complete = accWordCnt === wordCount
    val cacheLineVlds = Bits(setNum bits)
    val rdata_ff = Reg(Bits(ahblite3Cfg.dataWidth bits))
    val rdataCapture = RegNext(accWordCnt === wordIdx_ff)

    for ((p, idx) <- setPorts.zipWithIndex) {
      p.defaultVal()
      cacheLineVlds(idx) := p.vld
    }

    CIDLE.whenIsActive {
      // read the cache tag ram
      for (p <- setPorts) {
        p.tag := tag
        p.idx := setsIdx
      }
      // register the mask
      cacheLineMask := (io.cache_ahb.writeMask() << (wordIdx << 2)).resized
      newReq := io.cache_ahb.HSEL && io.cache_ahb.HTRANS(1)
      accWordCnt := 0
      addrInc := 0
      when(io.cache_ahb.HSEL && io.cache_ahb.HTRANS(1) && ~io.cache_ahb.HWRITE) {
        goto(READ_CHECK)
      }
      when(io.cache_ahb.HSEL && io.cache_ahb.HTRANS(1) && io.cache_ahb.HWRITE) {
        goto(WRITE_CHECK)
      }
    }

    READ_CHECK.whenIsActive {
      for (p <- setPorts) {
        p.tag := tag_ff
        p.idx := setsIdx_ff
      }
      io.cache_ahb.HRDATA := cacheLineToWord(cacheLineData)(wordIdx_ff)
      io.cache_ahb.HREADYOUT := cacheLineHit
      addrInc := addrInc + 4
      when(cacheLineHit) {
        goto(CIDLE)
      }.otherwise{
        when(io.mem_ahb.HREADY) {
          accWordCnt := accWordCnt + 1
          memAcc(write = False, addrInc)
          goto(READ_MEMORY)
        }.otherwise{
          goto(READ_WAIT_MEMORY)
        }
      }
    }

    READ_MEMORY.whenIsActive {
      io.cache_ahb.HREADYOUT := False
      val freeSpot = findSet(cacheLineVlds)
      when(io.mem_ahb.HREADYOUT) {
        accWordCnt := accWordCnt + 1
        addrInc := addrInc + 4
        cacheLineMask := (B"4'b1111" << (accWordCnt << 2)).resized
        for ((p, idx) <- setPorts.zipWithIndex) {
          p.fill := freeSpot(idx)
          p.mask  := cacheLineMask
          p.idx   := setsIdx_ff
          p.wdata := wordToCacheLine(io.mem_ahb.HRDATA)
        }

        when(rdataCapture) {
          rdata_ff := io.mem_ahb.HRDATA
        }

        when(accWordCnt === wordCount) {
          io.cache_ahb.HREADYOUT := True
          io.cache_ahb.HRDATA := rdataCapture ? io.mem_ahb.HRDATA | rdata_ff
          for ((p, idx) <- setPorts.zipWithIndex) {
            p.tag   := tag_ff
            p.updateTag := freeSpot(idx)
          }
          goto(CIDLE)
        }.otherwise{
          memAcc(write = False, addrInc)
        }
      }
    }

    WRITE_CHECK.whenIsActive {
      io.cache_ahb.HREADYOUT := cacheLineHit
      when(cacheLineHit) {
        // write the cache line
        for (p <- setPorts) {
          p.idx := setsIdx_ff
          p.wdata := wordToCacheLine(io.cache_ahb.HWDATA)
          p.mask := cacheLineMask
        }
        setPorts.foreach(x => x.fill := x.hit)
        goto(CIDLE)
      }
    }
  }
}

object CacheMain{
  def main(args: Array[String]) {
    SpinalVerilog(Ahblite3CacheSetAssociative(
      AhbLite3Cfg.dmemAhblite3Cfg(),
      cacheLineSize = 8,
      setNum = 2,
      setSize = 128
    )).printPruned()
  }
}

