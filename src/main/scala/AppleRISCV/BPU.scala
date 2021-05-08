///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: CPU_AppleRISCVCfg
//
// Author: Heqing Huang
// Date Created: 05/07/2021
//
// ================== Description ==================
//
// Branch pred_take Unit.
//
// Provide BPB (branch pred_take buffer) and BTB (branch target buffer)
// For simplicity, use direct map
//
///////////////////////////////////////////////////////////////////////////////////////////////////


package AppleRISCV

import spinal.core._
import AppleRISCVSoC._

case class BPU() extends Component {

  require(isPow2(AppleRISCVCfg.BPB_DEPTH))
  val BPB_ETR_WIDTH = log2Up(AppleRISCVCfg.BPB_DEPTH)

  val io = new Bundle {
    val pc            = in UInt(AppleRISCVCfg.XLEN bits)
    val if_stage_valid = in Bool
    val pred_take     = out Bool
    val pred_pc       = out UInt(AppleRISCVCfg.XLEN bits)
    val branch_update = in Bool
    val branch_taken  = in Bool
    val branch_instr_pc = in UInt(AppleRISCVCfg.XLEN bits)
  }
  //noIoPrefix()

  val bpb_init = Array.fill[UInt](AppleRISCVCfg.BPB_DEPTH)(0)
  val bpb_ram = Mem(UInt(2 bits), bpb_init)
  val bpb_tag_ram = Mem(UInt(AppleRISCVCfg.XLEN - BPB_ETR_WIDTH bits), AppleRISCVCfg.BPB_DEPTH)
  val btb_ram = Mem(UInt(SOCCfg.INSTR_RAM_ADDR_WIDTH bits), AppleRISCVCfg.BPB_DEPTH)
  val entry_valid = Reg(Bits(AppleRISCVCfg.BPB_DEPTH bit)) init 0

  val pc_idx = io.pc(BPB_ETR_WIDTH-1 downto 0)
  val bpc_idx = io.branch_instr_pc(BPB_ETR_WIDTH-1 downto 0)

  // pred_take
  val bpb_ram_pred_out = bpb_ram.readAsync(address = pc_idx)
  val bpb_tag_ram_pred_out = bpb_tag_ram.readAsync(address = pc_idx)
  val hit = (bpb_tag_ram_pred_out === io.pc(AppleRISCVCfg.XLEN-1 downto BPB_ETR_WIDTH)) & entry_valid(pc_idx)
  io.pred_take := (bpb_ram_pred_out === 2 | bpb_ram_pred_out === 3) & hit & io.if_stage_valid
  io.pred_pc   := btb_ram.readAsync(address = pc_idx).resized

  // Update
  val bpb_ram_update_out = bpb_ram.readAsync(
    address = bpc_idx
  )
  val updated_entry = UInt(2 bits)
  when (io.branch_taken) {
    updated_entry := (bpb_ram_update_out === 3) ? U"2'h3" | (bpb_ram_update_out + 1)
  }.otherwise{
    updated_entry := (bpb_ram_update_out === 0) ? U"2'h0" | (bpb_ram_update_out - 1)
  }
  bpb_ram.write(
    address = bpc_idx,
    data    = updated_entry,
    enable  = io.branch_update
  )
  bpb_tag_ram.write(
    address = bpc_idx,
    data    = io.branch_instr_pc(AppleRISCVCfg.XLEN-1 downto BPB_ETR_WIDTH),
    enable  = io.branch_update
  )
  btb_ram.write(
    address = bpc_idx,
    data    = io.branch_instr_pc(SOCCfg.INSTR_RAM_ADDR_WIDTH -1 downto 0),
    enable  = io.branch_update
  )
  entry_valid(bpc_idx) := io.branch_update
}
