module cache_tb ();

  reg      [15:0]   io_cache_ahb_HADDR;
  reg               io_cache_ahb_HSEL;
  reg               io_cache_ahb_HREADY;
  reg               io_cache_ahb_HWRITE;
  reg      [2:0]    io_cache_ahb_HSIZE;
  reg      [2:0]    io_cache_ahb_HBURST;
  reg      [3:0]    io_cache_ahb_HPROT;
  reg      [1:0]    io_cache_ahb_HTRANS;
  reg               io_cache_ahb_HMASTLOCK;
  reg      [31:0]   io_cache_ahb_HWDATA;
  wire     [31:0]   io_cache_ahb_HRDATA;
  wire              io_cache_ahb_HREADYOUT;
  wire              io_cache_ahb_HRESP;
  wire     [15:0]   io_mem_ahb_HADDR;
  wire              io_mem_ahb_HSEL;
  wire              io_mem_ahb_HREADY;
  wire              io_mem_ahb_HWRITE;
  wire     [2:0]    io_mem_ahb_HSIZE;
  wire     [2:0]    io_mem_ahb_HBURST;
  wire     [3:0]    io_mem_ahb_HPROT;
  wire     [1:0]    io_mem_ahb_HTRANS;
  wire              io_mem_ahb_HMASTLOCK;
  wire     [31:0]   io_mem_ahb_HWDATA;
  reg      [31:0]   io_mem_ahb_HRDATA;
  reg               io_mem_ahb_HREADYOUT;
  reg               io_mem_ahb_HRESP;
  reg               clk;
  reg               reset;

Ahblite3Cache DUT(.*);

`ifdef DUMP_VCD
initial begin
  $dumpfile ("DUT.vcd");
  $dumpvars (0, DUT);
end
`endif

endmodule