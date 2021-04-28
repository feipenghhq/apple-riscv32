# IO Constraint

# Clock
set_property PACKAGE_PIN E3 [get_ports clk]

# Uart
set_property PACKAGE_PIN A9 [get_ports uart_port_rxd]
set_property PACKAGE_PIN D10 [get_ports uart_port_txd]

# GPIO 0
# 0 - 3 => LEDs
set_property PACKAGE_PIN H5 [get_ports {gpio0_port_gpio[0]}]
set_property PACKAGE_PIN J5 [get_ports {gpio0_port_gpio[1]}]
set_property PACKAGE_PIN T9 [get_ports {gpio0_port_gpio[2]}]
set_property PACKAGE_PIN T10 [get_ports {gpio0_port_gpio[3]}]
# 4 - 7 => Buttons
set_property PACKAGE_PIN D9 [get_ports {gpio0_port_gpio[4]}]
set_property PACKAGE_PIN C9 [get_ports {gpio0_port_gpio[5]}]
set_property PACKAGE_PIN B9 [get_ports {gpio0_port_gpio[6]}]
set_property PACKAGE_PIN B8 [get_ports {gpio0_port_gpio[7]}]
# 8 - 11 => Slide Switches
set_property PACKAGE_PIN A8 [get_ports {gpio0_port_gpio[8]}]
set_property PACKAGE_PIN C11 [get_ports {gpio0_port_gpio[9]}]
set_property PACKAGE_PIN C10 [get_ports {gpio0_port_gpio[10]}]
set_property PACKAGE_PIN A10 [get_ports {gpio0_port_gpio[11]}]
