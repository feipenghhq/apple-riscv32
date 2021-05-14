
# Main clock
create_clock -period 10.000 [get_ports clk]
set_input_jitter [get_clocks -of_objects [get_ports clk]] 0.100
set_property PHASESHIFT_MODE WAVEFORM [get_cells -hierarchical *adv*]



