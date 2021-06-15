module de2_tb ();

parameter DATA_RAM_ADDR_WIDTH = 19;
parameter DATA_RAM_SIZE = 1 << (DATA_RAM_ADDR_WIDTH);

// The riscv-arch-test need a larger instruction memory then the actual hardware
`ifdef SIM_LARGE_INSTR_RAM
parameter INSTR_RAM_ADDR_WIDTH = 20;
`else
parameter INSTR_RAM_ADDR_WIDTH = 15;
`endif
parameter INSTR_RAM_SIZE = 1 << (INSTR_RAM_ADDR_WIDTH);

reg io_clk;
reg io_reset;
reg [7:0] data_ram  [0:DATA_RAM_SIZE-1];
reg [7:0] instr_ram [0:INSTR_RAM_SIZE-1];

integer im = 0;
integer dm = 0;

De2SoC DUT_AppleRISCVSoC(.*);

`ifdef DUMP_VCD
initial begin
  $dumpfile ("DUT_de2.vcd");
  $dumpvars (0, DUT_AppleRISCVSoC);
end
`endif

`ifdef LOAD_INSTR_RAM
initial begin
  $display("Loading instruction ram verilog file");
  $readmemh("instr_ram.rom", instr_ram);
  $display("[INFO] Loading Instruction RAM Done");
  for (im = 0; im < INSTR_RAM_SIZE; im = im + 4) begin
    DUT_AppleRISCVSoC.soc_imem.ram_symbol3[im/4] = instr_ram[im+3];
    DUT_AppleRISCVSoC.soc_imem.ram_symbol2[im/4] = instr_ram[im+2];
    DUT_AppleRISCVSoC.soc_imem.ram_symbol1[im/4] = instr_ram[im+1];
    DUT_AppleRISCVSoC.soc_imem.ram_symbol0[im/4] = instr_ram[im];
  end
end
`endif

`ifdef LOAD_DATA_RAM
integer i;
reg [31:0] word;
initial begin
  $display("Loading Data ram verilog file");
  $readmemh("data_ram.rom", data_ram);
  for (im = 0; im < DATA_RAM_SIZE; im = im + 2) begin
    IS61LV25616.RAM_0[im/2] = data_ram[im];
    IS61LV25616.RAM_1[im/2] = data_ram[im+1];
  end
  $display("[INFO] Loading Data RAM Done");
  //$display("[INFO] ram0 %x", IS61LV25616.RAM_0[0]);
  //$display("[INFO] ram0 %x", IS61LV25616.RAM_1[0]);
end
`endif

wire     [31:0]   io_gpio;
wire     [3:0]    io_pwm0;
wire              io_uart0_txd;
reg               io_uart0_rxd = 'b1;
reg               io_load_imem = 'b0;

wire     [17:0]   io_sram_addr;
wire              io_sram_we_n;
wire              io_sram_oe_n;
wire              io_sram_ub_n;
wire              io_sram_lb_n;
wire              io_sram_ce_n;
inout      [15:0] io_sram_data;

// =====================================
// SRAM model
// =====================================

IS61LV25616 IS61LV25616(
  .A(io_sram_addr),
  .IO(io_sram_data),
  .CE_n(io_sram_ce_n),
  .LB_n(io_sram_lb_n),
  .UB_n(io_sram_ub_n),
  .WE_n(io_sram_we_n),
  .OE_n(io_sram_oe_n)
);

// =====================================
// X checker
// =====================================

`ifdef X_CHECKER
wire        aggr_rd_wdata;
wire [31:0] rd_wdata = DUT_AppleRISCVSoC.cpu_core.mem2wb_rd_wdata;
wire        rd_write = DUT_AppleRISCVSoC.cpu_core.mem2wb_rd_wr;

always @(*) begin
  if (^rd_wdata === 1'bX && rd_write) begin
    $display("Found X in rd wdata: %x at time %t", rd_wdata, $time);
  end
end
`endif

endmodule