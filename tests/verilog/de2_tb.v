module de2_tb ();

parameter DATA_RAM_ADDR_WIDTH = 19;
parameter DATA_RAM_SIZE = 1 << (DATA_RAM_ADDR_WIDTH);
parameter INSTR_RAM_ADDR_WIDTH = 16;
parameter INSTR_RAM_SIZE = 1 << (INSTR_RAM_ADDR_WIDTH);

reg clk;
reg reset;
reg [7:0] data_ram  [0:DATA_RAM_SIZE-1];
reg [7:0] instr_ram [0:DATA_RAM_SIZE-1];

integer im = 0;
integer dm = 0;

AppleSoC_de2 DUT_AppleRISCVSoC(.*);

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
    DUT_AppleRISCVSoC.soc_imem_inst.ram.altsyncram_component.mem_data[im/4] = {instr_ram[im+3],instr_ram[im+2],instr_ram[im+1],instr_ram[im+0]};
    DUT_AppleRISCVSoC.soc_imem_inst.ram.altsyncram_component.mem_data_b[im/4] = {instr_ram[im+3],instr_ram[im+2],instr_ram[im+1],instr_ram[im+0]};
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
end
`endif

wire     [11:0]   gpio0;
wire     [3:0]    pwm0cmpgpio;
wire              uart0_txd;
reg               uart0_rxd = 'b1;
reg               load_imem = 'b0;

wire     [17:0]   sram_addr;
wire              sram_we_n;
wire              sram_oe_n;
wire              sram_ub_n;
wire              sram_lb_n;
wire              sram_ce_n;
inout      [15:0] sram_data;

// =====================================
// SRAM model
// =====================================


IS61LV25616 IS61LV25616(
  .A(sram_addr),
  .IO(sram_data),
  .CE_n(sram_ce_n),
  .LB_n(sram_lb_n),
  .UB_n(sram_ub_n),
  .WE_n(sram_we_n),
  .OE_n(sram_oe_n)
);

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