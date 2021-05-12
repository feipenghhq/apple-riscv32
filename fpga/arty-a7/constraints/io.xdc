# IO Constraint

##############################
# Clock
##############################
set_property PACKAGE_PIN E3 [get_ports clk]
set_property IOSTANDARD LVCMOS33 [get_ports clk]

##############################
# Reset
##############################
set_property PACKAGE_PIN C2 [get_ports reset]
set_property IOSTANDARD LVCMOS33 [get_ports reset]

##############################
# Uart 0
##############################
set_property PACKAGE_PIN A9 [get_ports uart0_rxd]
set_property PACKAGE_PIN D10 [get_ports uart0_txd]
set_property IOSTANDARD LVCMOS33 [get_ports uart0_rxd]
set_property IOSTANDARD LVCMOS33 [get_ports uart0_txd]

##############################
# GPIO 0
##############################
# 0 - 3 => LEDs
set_property PACKAGE_PIN H5 [get_ports {gpio0[0]}]
set_property PACKAGE_PIN J5 [get_ports {gpio0[1]}]
set_property PACKAGE_PIN T9 [get_ports {gpio0[2]}]
set_property PACKAGE_PIN T10 [get_ports {gpio0[3]}]
# 4 - 7 => Buttons
set_property PACKAGE_PIN D9 [get_ports {gpio0[4]}]
set_property PACKAGE_PIN C9 [get_ports {gpio0[5]}]
set_property PACKAGE_PIN B9 [get_ports {gpio0[6]}]
set_property PACKAGE_PIN B8 [get_ports {gpio0[7]}]

# 8 - 11 => Slide Switches
set_property PACKAGE_PIN A8  [get_ports {gpio0[8]}]
set_property PACKAGE_PIN C11 [get_ports {gpio0[9]}]
set_property PACKAGE_PIN C10 [get_ports {gpio0[10]}]
set_property PACKAGE_PIN A10 [get_ports {gpio0[11]}]

# 12 => 31
Not used

set_property IOSTANDARD LVCMOS33 [get_ports {gpio0[*]}]




