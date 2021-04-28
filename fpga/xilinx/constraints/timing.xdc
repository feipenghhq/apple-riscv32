
# Main clock
create_clock -period 10.000 [get_ports clk]
set_input_jitter [get_clocks -of_objects [get_ports clk]] 0.100
set_property PHASESHIFT_MODE WAVEFORM [get_cells -hierarchical *adv*]

# Input/Output delay

create_clock -period 20.000 -name VIRTUAL_clk_out1_mmcm -waveform {0.000 10.000}
set_input_delay -clock [get_clocks VIRTUAL_clk_out1_mmcm] -min -add_delay 0.000 [get_ports {gpio0_port_gpio[*]}]
set_input_delay -clock [get_clocks VIRTUAL_clk_out1_mmcm] -max -add_delay 2.000 [get_ports {gpio0_port_gpio[*]}]
set_input_delay -clock [get_clocks VIRTUAL_clk_out1_mmcm] -min -add_delay 0.000 [get_ports {gpio1_port_gpio[*]}]
set_input_delay -clock [get_clocks VIRTUAL_clk_out1_mmcm] -max -add_delay 2.000 [get_ports {gpio1_port_gpio[*]}]
set_input_delay -clock [get_clocks VIRTUAL_clk_out1_mmcm] -min -add_delay 0.000 [get_ports uart_port_rxd]
set_input_delay -clock [get_clocks VIRTUAL_clk_out1_mmcm] -max -add_delay 2.000 [get_ports uart_port_rxd]
set_output_delay -clock [get_clocks VIRTUAL_clk_out1_mmcm] -min -add_delay 0.000 [get_ports {gpio0_port_gpio[*]}]
set_output_delay -clock [get_clocks VIRTUAL_clk_out1_mmcm] -max -add_delay 2.000 [get_ports {gpio0_port_gpio[*]}]
set_output_delay -clock [get_clocks VIRTUAL_clk_out1_mmcm] -min -add_delay 0.000 [get_ports {gpio1_port_gpio[*]}]
set_output_delay -clock [get_clocks VIRTUAL_clk_out1_mmcm] -max -add_delay 2.000 [get_ports {gpio1_port_gpio[*]}]
set_output_delay -clock [get_clocks VIRTUAL_clk_out1_mmcm] -min -add_delay 0.000 [get_ports uart_port_txd]
set_output_delay -clock [get_clocks VIRTUAL_clk_out1_mmcm] -max -add_delay 2.000 [get_ports uart_port_txd]
