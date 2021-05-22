////////////////////////////////////////////
// Simulation model for IS61LV25616 Memory
// Note: Not timing accurate, only
// mimic the function
///////////////////////////////////////////

`timescale 1ns/10ps

module IS61LV25616 (
    input [17:0]    A,
    inout [15:0]    IO,
    input           CE_n,
    input           OE_n,
    input           WE_n,
    input           LB_n,
    input           UB_n
);


reg [7:0] RAM_0 [1<<18:0];
reg [7:0] RAM_1 [1<<18:0];
reg [15:0] dout;

wire ren;
wire wen;

assign ren = ~CE_n & ~OE_n & WE_n;
assign wen = ~CE_n & ~WE_n & (~LB_n | ~UB_n);

assign IO[15:8] = ren & ~UB_n ? RAM_1[A] : 8'bz;
assign IO[7:0]  = ren & ~LB_n ? RAM_0[A] : 8'bz;

always @(*) begin
   if (wen & ~UB_n) #1 RAM_1[A] <= IO[15:8];
   if (wen & ~LB_n) #1 RAM_0[A] <= IO[7:0];
end

endmodule