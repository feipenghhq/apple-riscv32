"""
 === Description ===

Test MUL

 === ASM ===

.text
main:
	li x1, 1
    li x2, 2
    mul x3, x1, x2 # x3 => 2
    add x4, x3, x0 # x4 => 2
    mul x5, x4, x4 # x5 => 4
    mul x6, x5, x5 # x6 => 16
end:
    j end


 === Machine Code ===

0x00100093
0x00200113
0x022081B3
0x00018233
0x024202B3
0x02528333
0x0000006F



"""

imem_data = [
0x00100093,
0x00200113,
0x022081B3,
0x00018233,
0x024202B3,
0x02528333,
0x0000006F,
]

expected_register = {
1 : 1,
2 : 2,
3 : 2,
4 : 2,
5 : 4,
6 : 16,
}
