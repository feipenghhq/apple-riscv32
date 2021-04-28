package soc

import bus.sib._
import spinal.core._
import spinal.lib.slave

case class imem(soc_param: SOC_PARAM) extends Component {

  val imem_cpu_sib = slave(Sib(soc_param.imemSibCfg))
  val imem_dbg_sib = slave(Sib(soc_param.imemSibCfg))

  // We fetch  a word (4 bytes) at a time so reduce the size by 4
  val SIZE = 1 << (soc_param.INSTR_RAM_ADDR_WIDTH - 2)
  val ram = new Mem(Bits(soc_param.INSTR_RAM_DATA_WIDTH bits), SIZE)

  // == CPU side Port == //
  val cpu_word_addr = imem_cpu_sib.addr(soc_param.INSTR_RAM_ADDR_WIDTH - 1 downto 2)
  val cpu_read_en = imem_cpu_sib.enable & imem_cpu_sib.sel & ~imem_cpu_sib.write
  val cpu_write_en = imem_cpu_sib.enable & imem_cpu_sib.sel & imem_cpu_sib.write
  imem_cpu_sib.ready := True
  imem_cpu_sib.resp := True
  ram.write(
    address = cpu_word_addr,
    data = imem_cpu_sib.wdata,
    enable = cpu_write_en
  )
  imem_cpu_sib.rdata := ram.readSync(
    address = cpu_word_addr,
    enable = cpu_read_en
  )

  // == DBG side Port == //
  val dbg_word_addr = imem_dbg_sib.addr(soc_param.INSTR_RAM_ADDR_WIDTH - 1 downto 2)
  val enable = imem_dbg_sib.sel & imem_dbg_sib.enable
  val dbg_read_en = enable
  imem_dbg_sib.ready := True
  imem_dbg_sib.resp := True
  ram.write(
    address = dbg_word_addr,
    data = imem_dbg_sib.wdata,
    enable = imem_dbg_sib.write & enable
  )
  imem_dbg_sib.rdata := ram.readSync(
    address = dbg_word_addr,
    enable = dbg_read_en
  )
}
