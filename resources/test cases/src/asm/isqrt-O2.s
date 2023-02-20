	.file	"isqrt.c"
	.section	.text.startup,"ax",@progbits
	.p2align 4,,15
	.globl	main
	.type	main, @function
main:
.LFB7:
	.cfi_startproc
	movl	4(%esp), %ecx
	xorl	%eax, %eax
	cmpl	$10, %ecx
	ja	.L2
	cmpl	$0, %ecx
	mov	$1, %eax
	jle	.L2
	.p2align 4,,7
	.p2align 3
.L3:
	addl	$1, %eax
	movl	%eax, %edx
	imull	%eax, %edx
	cmpl	%edx, %ecx
	jge	.L3
.L2:
	rep
	ret
	.cfi_endproc
.LFE7:
	.size	main, .-main
	.ident	"GCC: (Debian 4.6.0-10) 4.6.1 20110526 (prerelease)"
	.section	.note.GNU-stack,"",@progbits
