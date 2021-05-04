package AppleRISCVSoC.ip

import AppleRISCVSoC.bus._
import AppleRISCVSoC._
import spinal.core._
import spinal.lib._

case class Imem() extends Component {

  val imem_instr_sib = slave(Sib(SIBCfg.imemSibCfg))
  val imem_data_sib = slave(Sib(SIBCfg.imemSibCfg))

  // We fetch  a word (4 bytes) at a time so reduce the size by 4
  val SIZE = 1 << (SOCCfg.INSTR_RAM_ADDR_WIDTH - 2)
  val ram = new Mem(Bits(SOCCfg.INSTR_RAM_DATA_WIDTH bits), SIZE)

  // == CPU side Port == //
  val cpu_word_addr = imem_instr_sib.addr(SOCCfg.INSTR_RAM_ADDR_WIDTH - 1 downto 2)
  val cpu_read_en = imem_instr_sib.enable & imem_instr_sib.sel & ~imem_instr_sib.write
  val cpu_write_en = imem_instr_sib.enable & imem_instr_sib.sel & imem_instr_sib.write
  imem_instr_sib.ready := True
  imem_instr_sib.resp := True
  ram.write(
    address = cpu_word_addr,
    data = imem_instr_sib.wdata,
    enable = cpu_write_en
  )
  imem_instr_sib.rdata := ram.readSync(
    address = cpu_word_addr,
    enable = cpu_read_en
  )

  // == DBG side Port == //
  val dbg_word_addr = imem_data_sib.addr(SOCCfg.INSTR_RAM_ADDR_WIDTH - 1 downto 2)
  val enable = imem_data_sib.sel & imem_data_sib.enable
  val dbg_read_en = enable
  imem_data_sib.ready := True
  imem_data_sib.resp := True
  ram.write(
    address = dbg_word_addr,
    data = imem_data_sib.wdata,
    enable = imem_data_sib.write & enable
  )
  imem_data_sib.rdata := ram.readSync(
    address = dbg_word_addr,
    enable = dbg_read_en
  )
}
