"""
 === Description ===

Test DIV

 === ASM ===

.text
main:
	li x1,2
    li x2,128
    div x3, x2, x1 # x3 => 64
    add x4, x3, x0 # x4 => 64
    div x5, x3, x1 # x3 => 32
    div x6, x5, x1 # x3 => 16
    div x3, x3, x1 # x3 => 32
end:
    j end


 === Machine Code ===

0x00200093
0x08000113
0x021141B3
0x00018233
0x0211C2B3
0x0212C333
0x0211C1B3
0x0000006F




"""

imem_data = [
    0x00200093,
    0x08000113,
    0x021141B3,
    0x00018233,
    0x0211C2B3,
    0x0212C333,
    0x0211C1B3,
    0x0000006F,

]

expected_register = {
1 : 2,
2 : 128,
3 : 32,
4 : 64,
5 : 32,
6 : 16,
}
