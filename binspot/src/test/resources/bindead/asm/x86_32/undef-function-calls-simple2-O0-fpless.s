	.file	"undef-function-calls-simple2.c"
	.text
	.globl	f
	.type	f, @function
f:
.LFB0:
	.cfi_startproc
	movl	4(%esp), %eax
	ret
	.cfi_endproc
.LFE0:
	.size	f, .-f
	.globl	a
	.type	a, @function
a:
.LFB1:
	.cfi_startproc
	subl	$28, %esp
	.cfi_def_cfa_offset 32
	movl	32(%esp), %eax
	movl	%eax, (%esp)
	call	b
	movl	32(%esp), %eax
	addl	$28, %esp
	.cfi_def_cfa_offset 4
	ret
	.cfi_endproc
.LFE1:
	.size	a, .-a
	.globl	b
	.type	b, @function
b:
.LFB2:
	.cfi_startproc
	subl	$28, %esp
	.cfi_def_cfa_offset 32
	movl	32(%esp), %eax
	movl	%eax, (%esp)
	call	c
	movl	32(%esp), %eax
	addl	$28, %esp
	.cfi_def_cfa_offset 4
	ret
	.cfi_endproc
.LFE2:
	.size	b, .-b
	.globl	c
	.type	c, @function
c:
.LFB3:
	.cfi_startproc
	subl	$4, %esp
	.cfi_def_cfa_offset 8
	movl	8(%esp), %eax
	movl	%eax, (%esp)
	call	f
	movl	8(%esp), %eax
	addl	$4, %esp
	.cfi_def_cfa_offset 4
	ret
	.cfi_endproc
.LFE3:
	.size	c, .-c
	.globl	u
	.type	u, @function
u:
.LFB4:
	.cfi_startproc
	subl	$28, %esp
	.cfi_def_cfa_offset 32
	movl	32(%esp), %eax
	movl	%eax, (%esp)
	call	v
	movl	32(%esp), %eax
	addl	$28, %esp
	.cfi_def_cfa_offset 4
	ret
	.cfi_endproc
.LFE4:
	.size	u, .-u
	.globl	v
	.type	v, @function
v:
.LFB5:
	.cfi_startproc
	subl	$28, %esp
	.cfi_def_cfa_offset 32
	movl	32(%esp), %eax
	movl	%eax, (%esp)
	call	w
	movl	32(%esp), %eax
	addl	$28, %esp
	.cfi_def_cfa_offset 4
	ret
	.cfi_endproc
.LFE5:
	.size	v, .-v
	.globl	w
	.type	w, @function
w:
.LFB6:
	.cfi_startproc
	subl	$4, %esp
	.cfi_def_cfa_offset 8
	movl	8(%esp), %eax
	movl	%eax, (%esp)
	call	f
	movl	8(%esp), %eax
	addl	$4, %esp
	.cfi_def_cfa_offset 4
	ret
	.cfi_endproc
.LFE6:
	.size	w, .-w
	.globl	main
	.type	main, @function
main:
.LFB7:
    .cfi_startproc
    subl    $20, %esp
    .cfi_def_cfa_offset 24
    movl    $0, 16(%esp)
    movl    $0, (%esp)
    call    a
    movl    $1, (%esp)
    call    u
    movl    16(%esp), %eax
    addl    $20, %esp
    .cfi_def_cfa_offset 4
    ret
    .cfi_endproc

.LFE7:
	.size	main, .-main
	.ident	"GCC: (Ubuntu/Linaro 4.7.3-1ubuntu1) 4.7.3"
	.section	.note.GNU-stack,"",@progbits
