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

`ifdef COCOTB_SIM
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
    DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol3[im/4] = instr_ram[im+3];
    DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol2[im/4] = instr_ram[im+2];
    DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol1[im/4] = instr_ram[im+1];
    DUT_AppleRISCVSoC.soc_imem_inst.ram_symbol0[im/4] = instr_ram[im];
  end
end
`endif

`ifdef LOAD_DATA_RAM
integer i;
reg [31:0] word;
initial begin
  $display("Loading Data ram verilog file");
  $readmemh("data_ram.rom", data_ram);
  $display("[INFO] Loading Data RAM Done");
  for (i = 0; i < 16; i = i + 4) begin
    $display("RAM value at address %d", i);
    word = {data_ram[i+3],data_ram[i+2],data_ram[i+1],data_ram[i]};
    $display("Data is %x", word);
  end
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

// SRAM model
wire     [18:0]   sram_byte_addr;
wire              read_enable;
assign            sram_byte_addr = {sram_addr, 1'b0};

// write
always @(*) begin
  if (!sram_ce_n && !sram_we_n) begin
    if (!sram_lb_n) data_ram[sram_byte_addr]   = sram_data[7:0];
    if (!sram_ub_n) data_ram[sram_byte_addr+1] = sram_data[15:8];
  end
end

// read
assign read_enable     = !sram_ce_n & !sram_oe_n & sram_we_n;
assign sram_data[7:0]  = read_enable ? data_ram[sram_byte_addr]   : 'bz;
assign sram_data[15:8] = read_enable ? data_ram[sram_byte_addr+1] : 'bz;


endmodule