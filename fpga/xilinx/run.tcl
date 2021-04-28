############################################################
# Tcl script for vivado
############################################################

# ========================================
# Step 1: Create project Design Setup
# ========================================

set PRJ_NAME $::env(PRJ_NAME)
set TOP      $::env(TOP)
set RTL_SRC  $::env(RTL_SRC)
set XDC      $::env(XDC)
set DEVICE   $::env(DEVICE)

set OUTPUT $PRJ_NAME/output
#exec mkdir -p $OUTPUT

create_project $PRJ_NAME -dir $PRJ_NAME -part $DEVICE

# ========================================
# Step 2: Read in source files
# ========================================
read_verilog $RTL_SRC
read_xdc     $XDC

# ========================================
# Step 3: synthesis
# ========================================
synth_design -part $DEVICE -top $TOP
write_checkpoint -force $OUTPUT/syn/syn_checkpoint
report_timing_summary -file $OUTPUT/syn/syn_timing.rpt
report_utilization -file $OUTPUT/syn/syn_utilization_all.rpt
report_utilization -hierarchical -file $OUTPUT/syn/syn_utilization_hier.rpt

# ========================================
# Step 4: opt, place and route
# ========================================

opt_design
place_design
phys_opt_design
write_checkpoint -force $OUTPUT/pnr/pnr_checkpoint
report_timing_summary -file $OUTPUT/pnr/pnr_timing.rpt
report_utilization -file $OUTPUT/pnr/pnr_utilization_all.rpt
report_utilization -hierarchical -file $OUTPUT/pnr/pnr_utilization_hier.rpt


