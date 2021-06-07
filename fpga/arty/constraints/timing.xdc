
# Main clock
create_clock -period 10.000 [get_ports io_CLK]
set_input_jitter [get_clocks -of_objects [get_ports io_CLK]] 0.100
set_property PHASESHIFT_MODE WAVEFORM [get_cells -hierarchical *adv*]




