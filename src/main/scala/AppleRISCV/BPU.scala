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

  require(isPow2(AppleRISCVCfg.BPU_DEPTH))
  val BPU_ETR_WIDTH = log2Up(AppleRISCVCfg.BPU_DEPTH)
  val PC_OFFSET     = 2
  val PC_USED_WIDTH = SOCCfg.INSTR_RAM_ADDR_WIDTH
  val BPU_TAG_WIDTH = PC_USED_WIDTH - BPU_ETR_WIDTH - PC_OFFSET
  val BTB_WIDTH     = PC_USED_WIDTH - PC_OFFSET
  // PC is aligned to 4 bytes lower 2 bits are always zero, so ignore.
  val IDX_RANGE = BPU_ETR_WIDTH+PC_OFFSET-1 downto PC_OFFSET
  val TAG_RANGE = PC_USED_WIDTH-1 downto BPU_ETR_WIDTH+PC_OFFSET
  val TGT_RANGE = PC_USED_WIDTH-1 downto PC_OFFSET


  val io = new Bundle {
    val pc            = in UInt(AppleRISCVCfg.XLEN bits)
    val pred_take     = out Bool
    val pred_pc       = out UInt(AppleRISCVCfg.XLEN bits)
    val branch_update = in Bool
    val branch_should_take = in Bool
    val branch_instr_pc  = in UInt(AppleRISCVCfg.XLEN bits)
    val branch_target_pc = in UInt(AppleRISCVCfg.XLEN bits)
    val if_stage_valid   = in Bool
  }
  noIoPrefix()

  val bpb_init    = Array.fill[UInt](AppleRISCVCfg.BPU_DEPTH)(0)
  val bpb_ram     = Mem(UInt(2 bits), bpb_init)                       // branch prediction buffer
  val bpb_tag_ram = Mem(UInt(BPU_TAG_WIDTH bits), AppleRISCVCfg.BPU_DEPTH)  // branch prediction buffer tag
  val btb_ram     = Mem(UInt(BTB_WIDTH bits), AppleRISCVCfg.BPU_DEPTH)  // branch target buffer
  val entry_valid = Reg(Bits(AppleRISCVCfg.BPU_DEPTH bits)) init 0

  val pc_idx  = io.pc(IDX_RANGE)
  val pc_tag  = io.pc(TAG_RANGE)
  val bpc_idx = io.branch_instr_pc(IDX_RANGE)
  val bpc_tag = io.branch_instr_pc(TAG_RANGE)

  // Prediction
  val bpb_ram_pred_out     = bpb_ram.readAsync(address = pc_idx)
  val bpb_tag_ram_pred_out = bpb_tag_ram.readAsync(address = pc_idx)
  val pred_hit = (bpb_tag_ram_pred_out === pc_tag) & entry_valid(pc_idx)
  io.pred_take := (bpb_ram_pred_out === 2 | bpb_ram_pred_out === 3) & pred_hit & io.if_stage_valid
  io.pred_pc   := (btb_ram.readAsync(address = pc_idx) @@ U"00").resized

  // Update
  val bpb_ram_update_out     = bpb_ram.readAsync(address = bpc_idx)
  val bpb_tag_ram_update_out = bpb_tag_ram.readAsync(address = bpc_idx)
  val update_hit = (bpb_tag_ram_update_out === bpc_tag) & entry_valid(bpc_idx)
  val updated_entry = UInt(2 bits)
  val update_sel = update_hit ## io.branch_should_take
  switch(update_sel) {
    is(B"2'b00"){updated_entry := 1} // weak not take
    is(B"2'b01"){updated_entry := 2} // weak take
    is(B"2'b10"){updated_entry := (bpb_ram_update_out === 0) ? U"2'h0" | (bpb_ram_update_out - 1)}
    is(B"2'b11"){updated_entry := (bpb_ram_update_out === 3) ? U"2'h3" | (bpb_ram_update_out + 1)}
  }
  when(io.branch_update) {entry_valid(bpc_idx) := True}
  bpb_ram.write(
    address = bpc_idx,
    data    = updated_entry,
    enable  = io.branch_update
  )
  bpb_tag_ram.write(
    address = bpc_idx,
    data    = io.branch_instr_pc(TAG_RANGE),
    enable  = io.branch_update
  )
  btb_ram.write(
    address = bpc_idx,
    data    = io.branch_target_pc(TGT_RANGE),
    enable  = io.branch_update
  )
}
