############################################################
# Tcl script for quartus
############################################################

# ========================================
# Step 1: Create project Design Setup
# ========================================

set PRJ_NAME    $::env(PRJ_NAME)
set TOP         $::env(TOP)
set DEVICE      $::env(DEVICE)
set FAMILY      $::env(FAMILY)

set SOC_RTL_PATH gen/rtl
set REPO_ROOT [exec git rev-parse --show-toplevel]
set OUTPUT output
#exec mkdir -p $OUTPUT

# Load Quartus II Tcl Project package
package require ::quartus::project
project_new $PRJ_NAME -revision $TOP -overwrite -part $DEVICE -family $FAMILY


# ========================================
# Step 2: Porject Assignment
# ========================================

set_global_assignment -name PROJECT_OUTPUT_DIRECTORY $OUTPUT

# Commit assignments
export_assignments

# ========================================
# Step 3: Read in RTL
# ========================================

set SOC_RTL  $REPO_ROOT/$SOC_RTL_PATH/apple_riscv_soc.v
set_global_assignment -name VERILOG_FILE $SOC_RTL
export_assignments

# ========================================
# Step 4: Synthesis
# ========================================
package require ::quartus::flow
execute_module -tool map

# ========================================
# Step 4: place and route
# ========================================
execute_module -tool fit

# ========================================
# Step 5: Reporting
# ========================================
package require ::quartus::report
load_report

# Print All Report Panel Names
# set panel_names [get_report_panel_names]
# foreach panel_name $panel_names {
# post_message "$panel_name"
#
# }

# Saving Report Data in csv Format
# This is the name of the report panel to save as a CSV file
set panel_name "Analysis & Synthesis||Analysis & Synthesis Resource Utilization by Entity"
set csv_file "synthesis_resource_utilization_by_entity.csv"
set fh [open $csv_file w]
set num_rows [get_number_of_rows -name $panel_name]
# Go through all the rows in the report file, including the
# row with headings, and write out the comma-separated data
for { set i 0 } { $i < $num_rows } { incr i } {
	set row_data [get_report_panel_row -name $panel_name \
		-row $i]
	puts $fh [join $row_data ","]
}
close $fh
unload_report

project_close