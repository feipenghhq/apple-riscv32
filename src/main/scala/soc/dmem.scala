package soc

import bus.sib.Sib
import spinal.core._
import spinal.lib.slave

case class dmem(soc_param: SOC_PARAM) extends Component {

  val dmem_sib = slave(Sib(soc_param.dmemSibCfg))

  // We fetch  a word (4 bytes) at a time so reduce the size by 4
  val SIZE = 1 << (soc_param.DATA_RAM_ADDR_WIDTH - 2)
  val ram = new Mem(Bits(soc_param.INSTR_RAM_DATA_WIDTH bits), SIZE)

  // == CPU side == //
  val word_addr = dmem_sib.addr(soc_param.DATA_RAM_ADDR_WIDTH - 1 downto 2)
  // Here we use HSEL to indicate we want to stall the data
  val read_en = dmem_sib.sel

  // Decode logic for write byte enable
  val byte_sel = dmem_sib.addr(1 downto 0)
  val word_sel = dmem_sib.addr(1)

  dmem_sib.ready := True
  dmem_sib.resp := True

  ram.write(
    address = word_addr,
    data = dmem_sib.wdata,
    enable = dmem_sib.write,
    mask = dmem_sib.mask
  )

  dmem_sib.rdata := ram.readSync(
    address = word_addr,
    enable = ~dmem_sib.write & read_en
  )
}
