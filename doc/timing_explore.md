# Timing Explore

## 5 Stage Pipeline

Clock Freq 100 MHz

| ID  | Name                   | WNS    | TNS      | Comment                       | LUT/FF    | Note                    |
| --- | ---------------------- | ------ | -------- | ----------------------------- | --------- | ----------------------- |
| 0   | Baseline               | -2.183 | -415.783 |                               |           |
| 1   | One Hot ALUOP          | -1.717 | -399.944 | Use onehot encoding for aluop |           |
| 2   | 1+New AUIPC/LUI        | -1.630 | -367.656 | Use ADD in ALU for AUIPC/LUI  | 2373/1531 |                         |
| 3   | 2+moved mem to EX end  | -1.150 | -373.927 |                               |           | Have bugs in the design |
| 4   | 3+moved csr to MEM end | -1.150 | -373.927 |                               | 2083/1443 | Good                    |