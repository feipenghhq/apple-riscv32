module arty_tb ();

parameter DATA_RAM_ADDR_WIDTH = 16;
parameter DATA_RAM_SIZE = 1 << (DATA_RAM_ADDR_WIDTH);
parameter INSTR_RAM_ADDR_WIDTH = 16;
parameter INSTR_RAM_SIZE = 1 << (INSTR_RAM_ADDR_WIDTH);

reg clk;
reg reset;
reg [7:0] data_ram [0:DATA_RAM_SIZE-1];
reg [7:0] instr_ram [0:DATA_RAM_SIZE-1];

integer im = 0;
integer dm = 0;

AppleSoC_arty DUT_AppleRISCVSoC(.*);

`ifdef DUMP_VCD
initial begin
  $dumpfile ("DUT_arty.vcd");
  $dumpvars (0, DUT_AppleRISCVSoC);
end
`endif

`ifdef LOAD_INSTR_RAM
initial begin
  $display("Loading instruction ram verilog file");
  $readmemh("instr_ram.rom", instr_ram);
  $display("[INFO] Loading Instruction RAM Done");
  for (im = 0; im < INSTR_RAM_SIZE; im = im + 4) begin
    DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol3[im/4] = instr_ram[im+3];
    DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol2[im/4] = instr_ram[im+2];
    DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol1[im/4] = instr_ram[im+1];
    DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol0[im/4] = instr_ram[im];
  end
end
`endif

`ifdef LOAD_DATA_RAM
initial begin
  $display("Loading Data ram verilog file");
  $readmemh("data_ram.rom", data_ram);
  for (dm = 0; dm < DATA_RAM_SIZE; dm = dm + 4) begin
    DUT_AppleRISCVSoC.soc_dmem_inst.ram_symbol3[dm/4] = data_ram[dm+3];
    DUT_AppleRISCVSoC.soc_dmem_inst.ram_symbol2[dm/4] = data_ram[dm+2];
    DUT_AppleRISCVSoC.soc_dmem_inst.ram_symbol1[dm/4] = data_ram[dm+1];
    DUT_AppleRISCVSoC.soc_dmem_inst.ram_symbol0[dm/4] = data_ram[dm];
  end
  $display("[INFO] Loading Data RAM Done");
end
`endif

  wire     [11:0]   gpio0;
  wire     [3:0]    pwm0cmpgpio;
  wire              uart0_txd;
  reg               uart0_rxd = 'b1;
  reg               load_imem = 'b0;

// =====================================
// X checker
// =====================================

wire        aggr_rd_wdata;
wire [31:0] rd_wdata = DUT_AppleRISCVSoC.cpu_core.mem2wb_rd_wdata;
wire        rd_write = DUT_AppleRISCVSoC.cpu_core.mem2wb_rd_wr;

always @(*) begin
  if (^rd_wdata === 1'bX && rd_write) begin
    $display("Found X in rd wdata: %x at time %t", rd_wdata, $time);
  end
end

initial begin
  //$monitor("RD wdata: %x at time %t", rd_wdata, $time);
end

endmodule