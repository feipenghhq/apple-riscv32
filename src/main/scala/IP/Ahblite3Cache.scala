///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: Ahblite3Cache
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
// Completed Function
// - [x] Read Miss/Read Hit
// - [x] Write Miss/Write Hit
// - [x] Read Set Replacement and Flushing
// - [x] Write Set Replacement and Flushing
// - [x] NRU replacement policy
///////////////////////////////////////////////////////////////////////////////////////////////////

package IP

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.ahblite.AhbLite3._
import spinal.lib.bus.amba3.ahblite._
import spinal.lib.fsm._
import scala.collection.mutable.ArrayBuffer

/** Cache Configuration */
case class CacheConfig(
                        ahblite3Cfg: AhbLite3Config,
                        cacheLineSize: Int,             // cache line size in byte
                        setNum: Int,                    // number of set
                        setSize: Int,                   // size of each set
                        ramType: String = "DISTRIBUTED",
                        replacement: String = "NRU"     // replacement policy
                      ) {
  def wordCount          = cacheLineSize / 4
  def cacheLineSizeBits  = cacheLineSize * 8
  def byteAddrRange      = 1 downto 0
  def wordAddrRange      = log2Up(wordCount)+byteAddrRange.start downto byteAddrRange.start+1
  def idxAddrRange       = log2Up(setSize)+wordAddrRange.start downto wordAddrRange.start+1
  def tagAddrRange       = ahblite3Cfg.addressWidth-1 downto idxAddrRange.start+1
  def cacheLineAddrRange = ahblite3Cfg.addressWidth-1 downto wordAddrRange.start+1

  println("word count = " + wordCount)
}

/** Ports for each cache set logic */
case class SetLogicPort(cacheConfig: CacheConfig) extends Bundle with IMasterSlave {

  val fill      = Bool    // fill the cache line from main memory
  val write     = Bool    // write the cache from cpu
  val mask      = Bits(cacheConfig.cacheLineSize bits)
  val tag       = UInt(cacheConfig.tagAddrRange.size bits)
  val rdIdx     = UInt(cacheConfig.idxAddrRange.size bits)
  val wrIdx     = UInt(cacheConfig.idxAddrRange.size bits)
  val wdata     = Bits(cacheConfig.cacheLineSize*8 bits)
  val updateTag = Bool    // write new tag
  val setnru    = if (cacheConfig.replacement == "NRU") Bool else null
  val clrnru    = if (cacheConfig.replacement == "NRU") Bool else null

  val hit    = Bool
  //val vld    = Bool
  val dty    = Bool
  val tagout = UInt(cacheConfig.tagAddrRange.size bits)
  val rdata  = Bits(cacheConfig.cacheLineSize * 8  bits)
  val nru    = if (cacheConfig.replacement == "NRU") Bool else null

  override def asMaster(): Unit = {
    in(fill, write, mask, tag, rdIdx, wrIdx, wdata, updateTag)
    out(hit, dty, tagout, rdata)
    if (cacheConfig.replacement == "NRU") {
      in(setnru, clrnru)
      out(nru)
    }
  }
}

/** Cache set logic */
case class SetsLogic(cacheConfig: CacheConfig) extends Component {

  val io = master(SetLogicPort(cacheConfig))

  val tag_ram   = Mem(UInt(cacheConfig.tagAddrRange.size bits), cacheConfig.setSize)
  val data_ram  = Mem(Bits(cacheConfig.cacheLineSizeBits bits), cacheConfig.setSize)
  val valid     = Reg(Bits(cacheConfig.setSize bits)) init 0  // valid bits for each entry
  val dirty     = Reg(Bits(cacheConfig.setSize bits)) init 0  // dirty bits for each entry
  val nrus      = if (cacheConfig.replacement == "NRU") Reg(Bits(cacheConfig.setSize bits)) else null // NRU bits

  // Additional logic for distributed ram
  val distr = if (cacheConfig.ramType == "DISTRIBUTED") new Area {
    val data = data_ram.readAsync(io.rdIdx)
    val tag  = tag_ram.readAsync(io.rdIdx)
    val data_ff = RegNext(data)
    val tag_ff  = RegNext(tag)
  } else null

  // Check tag, valid, dirty signal
  val tag_out   = if (cacheConfig.ramType == "BLOCK") tag_ram.readSync(io.rdIdx) else {distr.tag_ff}
  val data_out  = if (cacheConfig.ramType == "BLOCK") data_ram.readSync(io.rdIdx) else {distr.data_ff}

  val valid_out = RegNext(valid(io.rdIdx))
  val dirty_out = RegNext(dirty(io.rdIdx))
  val nru_out   = RegNext(nrus(io.rdIdx))
  val tag_s1    = RegNext(io.tag)

  io.hit    := valid_out && (tag_out === tag_s1)
  //io.vld    := valid_out
  io.dty    := dirty_out
  io.nru    := nru_out
  io.tagout := tag_out

  // Read
  io.rdata := data_out

  // Write
  data_ram.write(
    address = io.wrIdx,
    data    = io.wdata,
    enable  = io.fill | io.write,
    mask    = io.mask
  )

  // dirty bit update
  when(io.write) {
    dirty(io.wrIdx) := True
  }.elsewhen(io.fill) { // after filling from memory, the dirty bit should be zero
    dirty(io.wrIdx) := False
  }

  // Update tag
  // Tag is updated at the last cycle of filling from memory
  tag_ram.write(
    address = io.wrIdx,
    data    = io.tag,
    enable  = io.updateTag
  )

  // The entry is valid after updating the tag
  when(io.updateTag) {
    valid(io.wrIdx) := True
  }

  // NRU logic
  val aaa = SInt(cacheConfig.setSize bits)
  aaa := -1
  nrus init aaa.asBits

  when(io.clrnru) {
    nrus(io.rdIdx) := False
  }.elsewhen(io.setnru) {
    nrus(io.rdIdx) := True
  }

}

/** AHB3 Cache */
case class Ahblite3Cache(cacheConfig: CacheConfig) extends Component {

  //###############################################################//

  // -------------------------------------------
  // Utility Function
  // -------------------------------------------

  /** Split the cache line into word */
  def cacheLineToWord(cacheLine: Bits): Vec[Bits] = {
    val wordNum = 1 << cacheConfig.wordAddrRange.size
    val wordVec = Vec(Bits(32 bits), wordNum)
    for (i <- 0 until wordNum) {
      wordVec(i) := cacheLine(i*32 + 31 downto i*32)
    }
    wordVec
  }

  /** Fill the word to cache line */
  def wordToCacheLine(word: Bits): Bits = {
    val wordNum = 1 << cacheConfig.wordAddrRange.size
    val wordVec = Vec(Bits(32 bits), wordNum)
    for (i <- 0 until wordNum) {
      wordVec(i) := word
    }
    wordVec.reduce(_ ## _)
  }

  /** find the free set */
  def findFreeSet(vlds: Bits): Bits = {
    // find the least empty spot
    val masks = vlds.asUInt + 1
    val idx = ~vlds & masks.asBits
    idx
  }

  /** find the lowest 1 */
  def bitMask(b: Bits): Bits = {
    b & (~b.asUInt + 1).asBits
  }

  /**
   * Find the Set to fill the data
   * Using NRU logic
   */
  def findNewSetNRU(nrus: Bits, dirtys: Bits): Bits = {
    val noNru = ~nrus.orR
    val noDirty = ~dirtys.orR
    val allDirty = dirtys.andR
    val setIds = Bits(nrus.getBitsWidth bits)
    // No NRU found and all Dirty or No Dirty, choose set 0
    when(noNru && (allDirty || noDirty)) {
      setIds := B"1".resized
    // No NRU found and not all Dirty, choose the lowest non-dirty set
    }.elsewhen(noNru && !allDirty) {
      setIds := bitMask(dirtys)
    // Choose the lowest NRU
    }.otherwise{
      setIds := bitMask(nrus)
    }
    setIds
  }

  /** Convert Array of Bool to bits */
  def arrayToBit(in: Array[Bool]): Bits = {
    val in1 = in.map(_.asBits)
    in1.reduce((x, y) => y ## x)
  }

  //###############################################################//

  val io = new Bundle {
    val cache_ahb = slave(AhbLite3(cacheConfig.ahblite3Cfg))
    val mem_ahb   = master(AhbLite3Master(cacheConfig.ahblite3Cfg))
  }

  // ----------------------------------------
  // Check input configuration
  // ----------------------------------------
  require(isPow2(cacheConfig.setSize))
  require(isPow2(cacheConfig.setNum))
  require(cacheConfig.cacheLineSize >= 4)
  require(cacheConfig.cacheLineSize % 4 == 0)

  // ----------------------------------------
  // Extract the address into different field
  // ----------------------------------------
  val tag           = io.cache_ahb.HADDR(cacheConfig.tagAddrRange)
  val setIdx        = io.cache_ahb.HADDR(cacheConfig.idxAddrRange)
  val wordIdx       = io.cache_ahb.HADDR(cacheConfig.wordAddrRange)
  val cacheLineAddr = io.cache_ahb.HADDR(cacheConfig.cacheLineAddrRange)
  val word0Addr     = U"0".resize(cacheConfig.cacheLineAddrRange.end)     // Address to word 0

  // ----------------------------------------
  // register the input information
  // ----------------------------------------
  val request           = io.cache_ahb.HSEL & io.cache_ahb.HTRANS(1)  // new cache request
  val request_s1        = RegNext(request) init False
  val cacheTag_ff       = RegNextWhen(tag, request)
  val cacheSetIdx_ff    = RegNextWhen(setIdx, request) init 0
  val cacheLineAddr_ff  = RegNextWhen(cacheLineAddr, request)
  val wordIdx_ff        = RegNextWhen(wordIdx, request) init 0
  val hprot_ff          = RegNextWhen(io.cache_ahb.HPROT, request)
  val hwrite_ff         = RegNextWhen(io.cache_ahb.HWRITE, request) init False
  val hwdata_ff         = RegNextWhen(io.cache_ahb.HWDATA, request_s1)
  val writeMask_ff      = RegNextWhen(io.cache_ahb.writeMask(), request)

  // ----------------------------------------
  // Instantiate Cache Sets
  // ----------------------------------------
  val cacheSets = ArrayBuffer[Component]()
  val setPorts  = Array.fill(cacheConfig.setNum)(SetLogicPort(cacheConfig))
  for (i <- 0 until cacheConfig.setNum) {
    val set = SetsLogic(cacheConfig)
    set.io <> setPorts(i)
    set.setName("cacheSet" + i)
    cacheSets.append(set)
  }

  //###############################################################//

  // ----------------------------------------
  // Cache Control StateMachine
  // ----------------------------------------
  val CacheCtrl = new StateMachine {

    // ----------------------------------------
    // State
    // ----------------------------------------
    val CACHE_IDLE  = new State with EntryPoint
    val TAG_CHECK   = new State
    val READ_MEMORY = new State
    val WRITE_CACHE = new State
    val FLUSH       = new State
    //val READ_WAIT_MEMORY = new State

    // ----------------------------------------
    // Variable
    // ----------------------------------------
    val cacheHit       = setPorts.map(_.hit).reduce(_ | _)                  // overall cache hit
    //val cacheVlds      = arrayToBit(setPorts.map(_.vld))                  // cache valid for each set
    val cacheNrus      = arrayToBit(setPorts.map(_.nru))                    // cache nru for each set
    val cacheDirtys    = arrayToBit(setPorts.map(_.dty))                    // cache dirty for each set
    val cacheLineData  = MuxOH(setPorts.map(_.hit), setPorts.map(_.rdata))  // cache line data from the hit set
    val newSetId       = findNewSetNRU(cacheNrus, cacheDirtys)              // the set to store the new data from memory
    val cacheLineTag   = MuxOH(newSetId, setPorts.map(_.tagout))            // cache line tag from the hit set
    val flushAddr      = cacheLineTag @@ cacheSetIdx_ff @@ word0Addr        // the address for flushing data to memory
    val noNru          = ~cacheNrus.orR                                     // No available NRUs, need to reset NRU
    val newSetDirty    = (newSetId & cacheDirtys).orR
    val cacheWriteMask = Reg(Bits(cacheConfig.cacheLineSize bits))                // cache line mask for cache write, determine which word to write
    val addrInc        = Reg(UInt(cacheConfig.wordAddrRange.size+2 bits)) init 0  // address increment on base addr
    val memWordCnt     = Reg(UInt(cacheConfig.wordAddrRange.size+1 bits)) init 0  // number of access word from/to memory
    val memWordCnt_s1  = Reg(UInt(cacheConfig.wordAddrRange.size+1 bits)) init 0  // delayed version of memWordCnt
    val rdataCapture   = Reg(Bool)                                                // capture the data from memory for the cache read data
    val rdata_ff       = Reg(Bits(cacheConfig.ahblite3Cfg.dataWidth bits))        // store the read data from memory to forward to the cache AHB

    // ----------------------------------------
    // Default value
    // ----------------------------------------
    io.cache_ahb.HREADYOUT := False
    io.cache_ahb.HRESP     := False
    io.cache_ahb.HRDATA    := 0

    io.mem_ahb.HWRITE      := False
    io.mem_ahb.HADDR       := 0
    io.mem_ahb.HWDATA      := 0
    io.mem_ahb.HTRANS      := IDLE
    io.mem_ahb.HPROT       := 0
    io.mem_ahb.HSIZE       := 0
    io.mem_ahb.HBURST      := 0
    io.mem_ahb.HMASTLOCK   := False

    rdataCapture           := memWordCnt === wordIdx_ff

    // default port connection for each set
    for (p <- setPorts) {
      p.fill     := False
      p.write    := False
      p.tag      := cacheTag_ff
      p.rdIdx    := cacheSetIdx_ff
      p.wrIdx    := cacheSetIdx_ff
      p.mask     := cacheWriteMask
      p.wdata    := wordToCacheLine(io.cache_ahb.HWDATA)
      p.updateTag := False
      if (cacheConfig.replacement == "NRU") {
        p.setnru := False
        p.clrnru := False
      }
    }

    /** group logic for new cache request */
    def newRequest(): Unit = {
      memWordCnt  := 0
      addrInc     := 0
      setPorts.foreach(_.tag := tag)
      setPorts.foreach(_.rdIdx := setIdx)
      // place the write mask to the correct location
      cacheWriteMask := (io.cache_ahb.writeMask() << (wordIdx << 2)).resized
      goto(TAG_CHECK)
    }

    /**
     * Functions to wrap around memory access operation
     * We use NONSEQ instead of burst operation to simplify logic
     */
    def memAccess(write: Bool, addrInc: UInt, flush: Bool): Unit = {
      // The base address should be aligned to the first word of the cache line
      val cacheLineAddrWord0 = cacheLineAddr_ff @@ word0Addr
      // for flush operation, the address should come from cache tag
      val baseAddr = flush ? flushAddr | cacheLineAddrWord0
      io.mem_ahb.HWRITE := write
      io.mem_ahb.HADDR  := baseAddr + addrInc
      io.mem_ahb.HTRANS := NONSEQ
      io.mem_ahb.HPROT  := hprot_ff
      io.mem_ahb.HSIZE  := B"010"  // Always do 4 byte access
    }

    // ----------------------------------------
    // State Machine Main Logic
    // ----------------------------------------

    CACHE_IDLE.whenIsActive {
      io.cache_ahb.HREADYOUT := True
      when(request) {
        newRequest()
      }
    }

    TAG_CHECK.whenIsActive {
      io.cache_ahb.HRDATA    := cacheLineToWord(cacheLineData)(wordIdx_ff) // If hit then data will be used
      addrInc                := 4
      memWordCnt             := 1
      memWordCnt_s1          := memWordCnt
      setPorts.foreach(x => x.write := x.hit & hwrite_ff)
      // reset the cache line mask to the first word in preparation of grabbing the entire cache line from memory
      cacheWriteMask         := B"4'hf".resized
      // Cache Hit
      when(cacheHit) {
        io.cache_ahb.HREADYOUT := True
        when(request) {
          newRequest()
        }.otherwise{
          goto(CACHE_IDLE)
        }
        // clr the nru bit as we just used this set
        if (cacheConfig.replacement == "NRU") {for ((p, idx) <- setPorts.zipWithIndex) {p.clrnru := newSetId(idx)}}
      // Cache Miss - Grab the data from main memory
      }.otherwise{
        when(io.mem_ahb.HREADY) {
          // Flush the dirty cache line to Main Memory
          when(newSetDirty) {
            memAccess(True, addrInc, True)
            goto(FLUSH)
          // Nothing to send back, read the data from memory
          }.otherwise {
            memAccess(False, addrInc, False)
            goto(READ_MEMORY)
          }
        }.otherwise{
          //goto(READ_WAIT_MEMORY)
        }
        // If no NRU bit found, reset NRU
        if (cacheConfig.replacement == "NRU") {setPorts.foreach(_.setnru := noNru)}
      }
    }

    READ_MEMORY.whenIsActive {
      when(io.mem_ahb.HREADY) {
        memWordCnt     := memWordCnt + 1
        addrInc        := addrInc + 4
        cacheWriteMask := (B"4'hf" << (memWordCnt << 2)).resized
        for ((p, idx) <- setPorts.zipWithIndex) {
          p.fill  := newSetId(idx)
          p.wdata := wordToCacheLine(io.mem_ahb.HRDATA)
        }
        when(rdataCapture) {
          rdata_ff := io.mem_ahb.HRDATA
        }
        when(memWordCnt === cacheConfig.wordCount) {
          io.cache_ahb.HRDATA := rdataCapture ? io.mem_ahb.HRDATA | rdata_ff
          for ((p, idx) <- setPorts.zipWithIndex) {
            p.updateTag := newSetId(idx)
          }
          when (hwrite_ff) {
            // Update the write mask for the upcoming write
            cacheWriteMask := (writeMask_ff << (wordIdx_ff << 2)).resized
            goto(WRITE_CACHE)
          }.otherwise{
            io.cache_ahb.HREADYOUT := True
            when(request) {
              newRequest()
            }.otherwise{
              goto(CACHE_IDLE)
            }
          }
          // clr the nru bit as we just used this set
          if (cacheConfig.replacement == "NRU") {for ((p, idx) <- setPorts.zipWithIndex) {p.clrnru := newSetId(idx)}}
        }.otherwise{
          memAccess(write = False, addrInc, False)
        }
      }
    }

    WRITE_CACHE.whenIsActive {
      for ((p, idx) <- setPorts.zipWithIndex) {
        p.write := newSetId(idx)
        p.wdata := wordToCacheLine(hwdata_ff)
      }
      // If there is new request and the request is not to the same cache line, then we can check the cache line
      when(request && (cacheLineAddr_ff =/= cacheLineAddr_ff)) {
        io.cache_ahb.HREADYOUT := True
        newRequest()
      // If there is no new request, go to IDLE state
      // If there is new request and the request is to the same cache line, then we need to wait for a cycle,
      // this is because at this clock, we are writing the cache line and reading the same cache line may not return
      // the new write data (depends read-on-write behavior of the ram)
      }.otherwise{
        goto(CACHE_IDLE)
      }
    }

    FLUSH.whenIsActive {
      memWordCnt_s1     := memWordCnt
      // Need to use the value of memWordCnt from previous clock because the data is one clock delayed
      io.mem_ahb.HWDATA := cacheLineToWord(MuxOH(newSetId, setPorts.map(_.rdata)))(memWordCnt_s1(memWordCnt_s1.getBitsWidth-2 downto 0))
      when(io.mem_ahb.HREADY) {
        // FLUSH complete, Start reading the memory
        when(memWordCnt === cacheConfig.wordCount) {
          addrInc      := 4
          memWordCnt   := 1
          rdataCapture := (0 === wordIdx_ff)  // special case for rdataCapture
          memAccess(False, 0, False)
          goto(READ_MEMORY)
        }.otherwise {
          memAccess(True, addrInc, True)
          addrInc    := addrInc + 4
          memWordCnt := memWordCnt + 1
        }
      }
    }
  }
}

object CacheMain{
  def main(args: Array[String]) {
    val cacheConfig = CacheConfig(
      AhbLite3Config(24, 32),
      cacheLineSize = 16,
      setNum = 4,
      setSize = 128
    )
    SpinalVerilog(Ahblite3Cache(cacheConfig)).printPruned()
  }
}

