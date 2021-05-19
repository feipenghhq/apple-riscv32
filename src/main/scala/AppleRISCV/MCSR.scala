///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: MCSR
//
// Author: Heqing Huang
// Date Created: 04/17/2021
//
// ================== Description ==================
//
// Machine Level CSR module.
//
// Some notes about interrupt.
// 1. mstatus:mpp is always set to 2'b11 since we only support machine mode
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._
import spinal.lib._
import spinal.lib.bus.misc._

case class MCSRIO() extends Bundle {
  val csr_bus = slave(CsrBus())

  // trap related input
  val mtrap_enter  = in Bool
  val mtrap_exit   = in Bool
  val mtrap_mepc   = in Bits(AppleRISCVCfg.XLEN bits)
  val mtrap_mcause = in Bits(AppleRISCVCfg.MXLEN bits)
  val mtrap_mtval  = in Bits(AppleRISCVCfg.MXLEN bits)
  val external_interrupt  = in Bool
  val timer_interrupt     = in Bool
  val software_interrupt  = in Bool

  // trap related output
  val mtvec        = out Bits(AppleRISCVCfg.XLEN bits)
  val mie_meie     = out Bool
  val mie_mtie     = out Bool
  val mie_msie     = out Bool
  val mstatus_mie  = out Bool
  val mepc         = out Bits(AppleRISCVCfg.MXLEN bits)

  // other
  val hartId       = in Bits(AppleRISCVCfg.MXLEN bits)

  // performance counter
  val inc_minstret  = in Bool
  val inc_br_cnt    = in Bool
  val inc_pred_good = in Bool
}

case class MCSR() extends Component {

  val MXLEN = AppleRISCVCfg.MXLEN

  val io = MCSRIO()
  noIoPrefix()

  val busCtrl = CsrBusSlaveFactory(io.csr_bus)
  val REG_T = Bits(MXLEN bits)

  // =========================================
  // Machine Information Registers
  // =========================================
  val mvendorid = busCtrl.read(B"0", 0xF11, 0, "Vendor ID")
  val marchid   = busCtrl.read(B"0", 0xF12, 0, "Architecture ID")
  val mimpid    = busCtrl.read(B"0", 0xF13, 0, "Implementation ID")
  val mhartid   = busCtrl.read(io.hartId, 0xF14, 0, "Hardware thread ID")


  // =========================================
  // Machine Trap Setup
  // =========================================
  val misa_val   = B"32'h0"
  val mstatus    = busCtrl.createReadAndWrite(REG_T, 0x300, 0, "Machine status register") init 0
  val misa       = busCtrl.read           (misa_val, 0x301, 0, "ISA and extensions")
  val mie        = busCtrl.createReadAndWrite(REG_T, 0x304, 0, "Machine interrupt-enable register") init 0
  val mtvec      = busCtrl.createReadAndWrite(REG_T, 0x305, 0, "Machine trap-handler base address")
  val mcounteren = busCtrl.read          (B"0", 0x306, 0, "Machine counter enable.")

  // mstatus register
  val mstatus_mie  = mstatus(3)
  val mstatus_mpie = mstatus(7)
  val mstatus_mpp  = mstatus(12 downto 11)
  mstatus_mpp := B"2'b11" // Since we only support Machine mode, we always set it to 11
  when(io.mtrap_enter) {
    mstatus_mie   := False
    mstatus_mpie  := mstatus_mie
  }.elsewhen(io.mtrap_exit) {
    mstatus_mie   := mstatus_mpie
    mstatus_mpie  := True
  }
  // misa
  misa_val(AppleRISCVCfg.MXLEN-1 downto AppleRISCVCfg.MXLEN-2) := 1
  // mie register
  val mie_meie = mie(11)
  val mie_mtie = mie(7)
  val mie_msie = mie(3)
  // mtvec register
  val mtvec_base = mtvec(MXLEN-1 downto 2)
  val mtvec_mode = mtvec(1 downto 0)


  // =========================================
  // Machine Trap Handling
  // =========================================
  val mscratch  = busCtrl.createReadAndWrite(REG_T, 0x340, 0, "Scratch register for machine trap handlers.")
  val mepc      = busCtrl.createReadAndWrite(REG_T, 0x341, 0, "Machine exception program counter.")
  val mcause    = busCtrl.createReadAndWrite(REG_T, 0x342, 0, "Machine trap cause.r")
  val mtval     = busCtrl.createReadAndWrite(REG_T, 0x343, 0, "Machine bad address or instruction.")
  val mip       = busCtrl.createReadAndWrite(REG_T, 0x344, 0, "Machine interrupt pending.") init 0

  // mepc
  val mepc_base = mepc(MXLEN-1 downto 2)
  val mepc_mode = mepc(1 downto 0)
  when(io.mtrap_enter) {mepc   := io.mtrap_mepc}
  // mcause
  when(io.mtrap_enter) {mcause := io.mtrap_mcause}
  // mtval
  when(io.mtrap_enter) {mtval  := io.mtrap_mtval}
  // mip register
  val mip_meip = mip(11)
  val mip_mtip = mip(7)
  val mip_msip = mip(3)
  mip_meip := io.external_interrupt &  mie_meie
  mip_mtip := io.timer_interrupt    &  mie_mtie
  mip_msip := io.software_interrupt &  mie_msie

  // =========================================
  // Machine Counter Setup
  // =========================================
  val mcountinhbit = if (CsrCfg.USE_MCOUNTINHIBIT) new Area {
    val value =  busCtrl.createReadAndWrite(REG_T, 0x320, 0, "Machine interrupt pending.") init 0
    val cy = value(0)
    val ir = value(2)
    val hpm = value(31 downto 3)
  } else null


  // =========================================
  // Machine Counter/Timers
  // =========================================
  val mcycle = if (CsrCfg.USE_MCYCLE) new Area
   {
    val cnt = Reg(UInt(64 bits)) init 0
     busCtrl.readAndWrite(cnt(MXLEN-1 downto 0), 0xB00, 0, "Machine cycle counter.")
     busCtrl.readAndWrite(cnt(63 downto MXLEN) , 0xB80, 0, "Upper 32 bits of mcycle, RV32I only.")
    when(mcountinhbit.cy) {cnt := cnt + 1}
  } else null

  val minstret = if (CsrCfg.USE_MINSTRET) new Area {
    val cnt = Reg(UInt(64 bits)) init 0
    busCtrl.readAndWrite(cnt(MXLEN-1 downto 0), 0xB02, 0, "Machine cycle counter.")
    busCtrl.readAndWrite(cnt(63 downto MXLEN) , 0xB82, 0, "Upper 32 bits of mcycle, RV32I only.")
    when(mcountinhbit.ir & io.inc_minstret) {cnt := cnt + 1}
  } else null

  val mhpmcounter3 = if (CsrCfg.USE_MHPMC3) new Area {
    // count number of branch instruction
    val cnt  = Reg(UInt(64 bits)) init 0
    busCtrl.readAndWrite(cnt(MXLEN-1 downto 0), 0xB03, 0, "Machine performance-monitoring counter.")
    busCtrl.readAndWrite(cnt(63 downto MXLEN) , 0xB83, 0, "Upper 32 bits of mhpmcounter3, RV32I only.")
    when(mcountinhbit.hpm(0) & io.inc_br_cnt) {cnt := cnt + 1}

  } else null

  val mhpmcounter4 = if (CsrCfg.USE_MHPMC4) new Area {
    // count number of correct predicted instruction
    val cnt  = Reg(UInt(64 bits)) init 0
    busCtrl.readAndWrite(cnt(MXLEN-1 downto 0), 0xB04, 0, "Machine performance-monitoring counter.")
    busCtrl.readAndWrite(cnt(63 downto MXLEN) , 0xB84, 0, "Upper 32 bits of mhpmcounter4, RV32I only.")
    when(mcountinhbit.hpm(1) & io.inc_pred_good) {cnt := cnt + 1}
  } else null

  // ============================================
  // Trap related Logic
  // ============================================
  io.mtvec       := mtvec
  io.mie_meie    := mie_meie
  io.mie_mtie    := mie_mtie
  io.mie_msie    := mie_msie
  io.mstatus_mie := mstatus_mie
  io.mepc        := mepc
}


/**
 * Csr Bus interface
 */
case class CsrBus() extends Bundle with IMasterSlave {

  val addr   = UInt(AppleRISCVCfg.CSR_ADDR_WIDTH bits)
  val wdata  = Bits(AppleRISCVCfg.MXLEN bits)
  val wen    = Bool
  val wtype  = CsrSelEnum()
  val rdata  = Bits(AppleRISCVCfg.MXLEN bits)
  val decerr = Bool

  override def asMaster(): Unit = {
    out(addr, wen, wdata, wtype)
    in(rdata, decerr)
  }
}

object CsrBusSlaveFactory {
  def apply(bus: CsrBus) = new CsrBusSlaveFactory(bus)
}

class CsrBusSlaveFactory(bus: CsrBus) extends BusSlaveFactoryDelayed {

  //val askWrite = bus.wen
  //val askRead  = True
  val doWrite  = bus.wen
  val doRead   = True
  val wdata    = Bits(AppleRISCVCfg.MXLEN bits)

  val mcsr_masked_set    = bus.rdata | bus.wdata
  val mcsr_masked_clear  = bus.rdata & ~bus.wdata
  switch(bus.wtype) {
    is(CsrSelEnum.DATA)  {wdata := bus.wdata}
    is(CsrSelEnum.SET)   {wdata := mcsr_masked_set}
    is(CsrSelEnum.CLEAR) {wdata := mcsr_masked_clear}
  }

  bus.decerr := True
  bus.rdata  := 0

  override def writeHalt() =  False
  override def readHalt()  = False

  override def writeAddress() = bus.addr
  override def readAddress()  = bus.addr

  override def busDataWidth   = AppleRISCVCfg.MXLEN
  override def wordAddressInc = AppleRISCVCfg.MXLEN

  override def build(): Unit = {
    super.doNonStopWrite(wdata)

    def doMappedElements(jobs: Seq[BusSlaveFactoryElement]): Unit = super.doMappedElements(
      jobs = jobs,
      // askWrite = askWrite,
      // askRead  = askRead,
      askWrite = False,
      askRead  = False,
      doWrite  = doWrite,
      doRead   = doRead,
      writeData = wdata,
      readData  = bus.rdata
    )

    switch(bus.addr) {
      for ((address, jobs) <- elementsPerAddress if address.isInstanceOf[SingleMapping]) {
        is(address.asInstanceOf[SingleMapping].address) {
          doMappedElements(jobs)
          bus.decerr := False
        }
      }
    }

    for ((address, jobs) <- elementsPerAddress if !address.isInstanceOf[SingleMapping]) {
      when(address.hit(bus.addr)) {
        doMappedElements(jobs)
        bus.decerr := False
      }
    }
  }
}
