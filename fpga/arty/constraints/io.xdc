# IO Constraint

##############################
# Clock
##############################
set_property PACKAGE_PIN E3 [get_ports io_CLK]
set_property IOSTANDARD LVCMOS33 [get_ports io_CLK]

##############################
# Reset
##############################
set_property PACKAGE_PIN C2 [get_ports io_RESET]
set_property IOSTANDARD LVCMOS33 [get_ports io_RESET]

##############################
# Uart 0
##############################
set_property PACKAGE_PIN A9 [get_ports io_UART0_rxd]
set_property PACKAGE_PIN D10 [get_ports io_UART0_txd]
set_property IOSTANDARD LVCMOS33 [get_ports io_UART0_rxd]
set_property IOSTANDARD LVCMOS33 [get_ports io_UART0_txd]

##############################
# GPIO 0
##############################
# 0 - 3 => LEDs
set_property PACKAGE_PIN H5 [get_ports {io_GPIO[0]}]
set_property PACKAGE_PIN J5 [get_ports {io_GPIO[1]}]
set_property PACKAGE_PIN T9 [get_ports {io_GPIO[2]}]
set_property PACKAGE_PIN T10 [get_ports {io_GPIO[3]}]
# 4 - 7 => Buttons
set_property PACKAGE_PIN D9 [get_ports {io_GPIO[4]}]
set_property PACKAGE_PIN C9 [get_ports {io_GPIO[5]}]
set_property PACKAGE_PIN B9 [get_ports {io_GPIO[6]}]
set_property PACKAGE_PIN B8 [get_ports {io_GPIO[7]}]
# 8 - 11 => Slide Switches
set_property PACKAGE_PIN A8 [get_ports {io_GPIO[8]}]
set_property PACKAGE_PIN C11 [get_ports {io_GPIO[9]}]
set_property PACKAGE_PIN C10 [get_ports {io_GPIO[10]}]
set_property PACKAGE_PIN A10 [get_ports {io_GPIO[11]}]

# 12 => 31
# Not used

set_property IOSTANDARD LVCMOS33 [get_ports {io_GPIO[*]}]

##############################
# PWM 0
##############################

# The first one goes to the Arduino/chipKIT Shield IO0
set_property PACKAGE_PIN U16 [get_ports {io_PWM0[0]}]
set_property PACKAGE_PIN G6 [get_ports {io_PWM0[1]}]
set_property PACKAGE_PIN F6 [get_ports {io_PWM0[2]}]
set_property PACKAGE_PIN E1 [get_ports {io_PWM0[3]}]

set_property IOSTANDARD LVCMOS33 [get_ports {io_PWM0[*]}]






