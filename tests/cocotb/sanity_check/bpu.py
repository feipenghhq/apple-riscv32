"""
 === Description ===

Test LUI/AUIPC

 === ASM ===

.text
main:
	li x1, 100
    li x2, 2

casea:
	sub x1, x1, x2
	bne x1, x0 casea

end:
	j end

"""

imem_data = [
0x06400093,
0x00200113,
0x402080B3,
0xFE009EE3,
0x0000006F,

]

expected_register = {
1 : 0,
}
