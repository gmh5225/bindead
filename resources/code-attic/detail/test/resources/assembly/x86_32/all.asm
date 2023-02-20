
format elf
	
section '.data' writable

foo		dw 0x2342
msg		db 'Hello World64!', 0x0A
msg_size	= $-msg

section '.text' executable

public main

main:
   mov eax, ebx
   mov ah, al
   mov eax, [ebx]
   mov eax, [ebx+edi]
   mov eax, [ebx+edi*4]
   mov eax, [edi*4]
   mov eax, [edi*4+0xff]
   mov eax, [ebx+edi*4+0xff]
   mov eax, [ebx+edi*4+0xffff]
   mov eax, [ebx+edi*4-1]
   mov eax, [ebx+edi*4-255]
   mov eax, [ebx+edi*4-65535]
   mov al, [ebx+edi*4-1]

   sub eax, 4294967295
   sub eax, 2147483648
   sub eax, 1
   sub eax, ebx
   sub eax, eax

   add eax, 1
   add eax, 2147483648
   add eax, ebx
   add eax, eax
   
   cmp eax, 4294967295
   cmp eax, 2147483648
   cmp eax, 1
   cmp eax, ebx
   cmp eax, eax
   
   cmpsb
   ; exit
   xor     edi, edi
   mov     eax, 60
   syscall
