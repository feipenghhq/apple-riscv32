############################################################
# Tcl script for quartus sta
############################################################


# ========================================
# Step 1: Design Setup
# ========================================
set PRJ_NAME    $::env(PRJ_NAME)
set TOP         $::env(TOP)
set REPO_ROOT   [exec git rev-parse --show-toplevel]
set OUTPUT output

set DE2_SDC_PATH $REPO_ROOT/fpga/de2/constraints

package require ::quartus::project
project_open -revision $TOP $PRJ_NAME

set_global_assignment -name PROJECT_OUTPUT_DIRECTORY $OUTPUT
export_assignments

# ========================================
# Step 2: Run STA
# ========================================


# Always create the netlist first
package require ::quartus::sta

create_timing_netlist
read_sdc $DE2_SDC_PATH/DE2_top.sdc
update_timing_netlist

set timing_file "timing.rpt"
# Run a setup analysis between nodes "foo" and "bar",
# reporting the worst-case slack if a path is found.
report_clocks -file $timing_file
create_timing_summary -panel_name "Setup Summary" -file $timing_file -append
create_timing_summary -hold -panel_name "Hold Summary" -file $timing_file -append
report_timing -to_clock { clk } -setup -npaths 10 -detail full_path -panel_name {Setup: clk} -file $timing_file -append

project_close