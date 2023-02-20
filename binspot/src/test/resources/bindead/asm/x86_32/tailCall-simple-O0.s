	.file	"tailCall-simple.c"
	.intel_syntax noprefix
	.text
	.globl	f
	.type	f, @function
f:
.LFB0:
	.cfi_startproc
	push	ebp
	.cfi_def_cfa_offset 8
	.cfi_offset 5, -8
	mov	ebp, esp
	.cfi_def_cfa_register 5
	push	esi
	push	ebx
	sub	esp, 48
	.cfi_offset 6, -12
	.cfi_offset 3, -16
	mov	eax, esp
	mov	esi, eax
	mov	eax, DWORD PTR [ebp+8]
	lea	edx, [eax-1]
	mov	DWORD PTR [ebp-24], edx
	sal	eax, 2
	lea	edx, [eax+3]
	mov	eax, 16
	sub	eax, 1
	add	eax, edx
	mov	DWORD PTR [ebp-44], 16
	mov	edx, 0
	div	DWORD PTR [ebp-44]
	imul	eax, eax, 16
	sub	esp, eax
	mov	eax, esp
	add	eax, 3
# replaced shifts used for stack pointer alignment below with and instruction
# 	shr	eax, 2
# 	sal	eax, 2
	and	eax, -4
	mov	DWORD PTR [ebp-20], eax
	mov	DWORD PTR [ebp-16], 0
	mov	DWORD PTR [ebp-12], 0
	mov	DWORD PTR [ebp-28], 0
	jmp	.L2
.L3:
	mov	eax, DWORD PTR [ebp-20]
	mov	edx, DWORD PTR [ebp-28]
	mov	ecx, DWORD PTR [ebp-28]
	mov	DWORD PTR [eax+edx*4], ecx
	mov	eax, DWORD PTR [ebp-28]
	mov	DWORD PTR [ebp-16], eax
	add	DWORD PTR [ebp-28], 1
.L2:
	mov	eax, DWORD PTR [ebp-28]
	cmp	eax, DWORD PTR [ebp+8]
	jl	.L3
	cmp	DWORD PTR [ebp+8], 2
	jle	.L4
	mov	ebx, DWORD PTR [ebp-16]
	mov	eax, 0
	jmp	.L5
.L4:
	mov	eax, 1
.L5:
	mov	esp, esi
	cmp	eax, 1
	jne	.L8
.L7:
	jmp	.L1
.L8:
	mov	eax, ebx
.L1:
	lea	esp, [ebp-8]
	pop	ebx
	.cfi_restore 3
	pop	esi
	.cfi_restore 6
	pop	ebp
	.cfi_restore 5
	.cfi_def_cfa 4, 4
	ret
	.cfi_endproc
.LFE0:
	.size	f, .-f
	.globl	g
	.type	g, @function
g:
.LFB1:
	.cfi_startproc
	push	ebp
	.cfi_def_cfa_offset 8
	.cfi_offset 5, -8
	mov	ebp, esp
	.cfi_def_cfa_register 5
	sub	esp, 24
	mov	eax, DWORD PTR [ebp+8]
	add	eax, 1
	mov	DWORD PTR [esp], eax
	call	f
	mov	eax, DWORD PTR [ebp+8]
	add	eax, 1
	mov	DWORD PTR [esp], eax
	call	h
	leave
	.cfi_restore 5
	.cfi_def_cfa 4, 4
	ret
	.cfi_endproc
.LFE1:
	.size	g, .-g
	.globl	h
	.type	h, @function
h:
.LFB2:
	.cfi_startproc
	push	ebp
	.cfi_def_cfa_offset 8
	.cfi_offset 5, -8
	mov	ebp, esp
	.cfi_def_cfa_register 5
	sub	esp, 40
	mov	DWORD PTR [ebp-12], 0
	jmp	.L12
.L13:
	mov	eax, DWORD PTR [ebp+8]
	and	eax, 17719
	sub	eax, DWORD PTR [ebp-12]
	mov	DWORD PTR [ebp+8], eax
	add	DWORD PTR [ebp-12], 1
.L12:
	mov	eax, DWORD PTR [ebp-12]
	cmp	eax, DWORD PTR [ebp+8]
	jl	.L13
	mov	eax, DWORD PTR [ebp+8]
	mov	DWORD PTR [esp], eax
	call	f
	leave
	.cfi_restore 5
	.cfi_def_cfa 4, 4
	ret
	.cfi_endproc
.LFE2:
	.size	h, .-h
	.globl	main
	.type	main, @function
main:
.LFB3:
	.cfi_startproc
	push	ebp
	.cfi_def_cfa_offset 8
	.cfi_offset 5, -8
	mov	ebp, esp
	.cfi_def_cfa_register 5
	and	esp, -16
	sub	esp, 32
	mov	DWORD PTR [esp+28], 20
	mov	eax, DWORD PTR [esp+28]
	mov	DWORD PTR [esp], eax
	call	g
	leave
	.cfi_restore 5
	.cfi_def_cfa 4, 4
	ret
	.cfi_endproc
.LFE3:
	.size	main, .-main
	.ident	"GCC: (Ubuntu/Linaro 4.7.2-2ubuntu1) 4.7.2"
	.section	.note.GNU-stack,"",@progbits
