# Syscall Entry Shell 내용을 뜯어보자

syscall_entry.S는 시스템콜이 호출되었을 때 `user mode`에서 작동하고 있던 프로세스를 `kernel mode`로 이동하여 시스템 콜을 수행하고 다시 ret하기까지 수행하는 shell 파일이다.
```
// syscall-entry.S
#include "threads/loader.h"

.text
.globl syscall_entry
.type syscall_entry, @function
syscall_entry:
	movq %rbx, temp1(%rip)
	movq %r12, temp2(%rip)     /* 저장된 레지스터 호출 */
	movq %rsp, %rbx            /* Store userland rsp */
	movabs $tss, %r12
	movq (%r12), %r12
	movq 4(%r12), %rsp         /* Read ring0 rsp from the tss */
```

시스템 콜이 발생하면 `rsp`를 `rbx`에 저장해놓는다. 먼저 `tss`를 이용하여 `ring 0 rsp`의 위치를 읽어 커널 모드로 넘어간다. 64 bit에서는 기본적으로 8단위로 이동하는데 tss에서 왜 4만 이동하는지 의아할 수 있다. 이는 `tss`의 구조를 확인하여 알 수 있다.

```c
// tss.h
struct task_state {
	uint32_t res1;
	uint64_t rsp0;
	uint64_t rsp1;
	uint64_t rsp2;
	uint64_t res2;
	uint64_t ist1;
	uint64_t ist2;
	uint64_t ist3;
	uint64_t ist4;
	uint64_t ist5;
	uint64_t ist6;
	uint64_t ist7;
	uint64_t res3;
	uint16_t res4;
	uint16_t iomb;
}__attribute__ ((packed));
```

`res1`의 크기가 4byte이기 때문이다. 마지막 줄의 `__attribute__ ((packed))` 라는 지시사항은 구조체 내의 각 field에 alignment를 무시하고 붙여서 메모리 안에 집어넣으라는 명령이어서 이를 가능하게 해준다.

```
	/* Now we are in the kernel stack */
	push $(SEL_UDSEG)      /* if->ss */
	push %rbx              /* if->rsp */
	push %r11              /* if->eflags */
	push $(SEL_UCSEG)      /* if->cs */
	push %rcx              /* if->rip */
	subq $16, %rsp         /* skip error_code, vec_no */
	push $(SEL_UDSEG)      /* if->ds */
	push $(SEL_UDSEG)      /* if->es */
```

시스템 콜이 호출되었을 때 범용 레지스터의 상태는 다음과 같다.

>`rax` - 시스템 호출 번호 포함  
`rcx` - 사용자 공간의 반환 주소 포함  
`r11` - 레지스터 플래그 포함  
`rdi` - 시스템 호출 처리기의 첫 번째 인자 포함  
`rsi` - 시스템 호출 처리기의 두 번째 인자 포함  
`rdx` - 시스템 호출 처리기의 세 번째 인자 포함  
`r10` - 시스템 호출 처리기의 네 번째 인자 포함  
`r8`  - 시스템 호출 처리기의 다섯 번째 인자 포함  
`r9`  - 시스템 호출 처리기의 여섯 번째 인자 포함  

따라서 시스템 콜 후에 수행해야 하는 `rip` 위치에 `rcx`를, 그리고 `eflags` 위치에 `r11`의 데이터를 대신 `push`하게 된다. `UDSEG`는 유저 메모리의 데이터, `UCSEG`는 코드 세그먼트를 가리키는 주소값이다.

```
	push %rax
	movq temp1(%rip), %rbx
	push %rbx
	pushq $0
	push %rdx
	push %rbp
	push %rdi
	push %rsi
	push %r8
	push %r9
	push %r10
	pushq $0 /* skip r11 */
	movq temp2(%rip), %r12
	push %r12
	push %r13
	push %r14
	push %r15
	movq %rsp, %rdi
```

그 후 커널 스택에 현재 레지스터의 정보를 모두 역순으로 담아준다. 그래야 우리가 원하는 모양의 구조체가 나올 것이기 때문. 그 후 `rdi`에 현재 커널 스택의 `rsp`를 담아 syscall을 호출할 준비를 한다.

```
check_intr:
	btsq $9, %r11          /* Check whether we recover the interrupt */
	jnb no_sti
	sti                    /* restore interrupt */
```

호출전에 인터럽트의 상태를 확인한다. 만약 시스템 콜이 호출되면 진입하게 되면 인터럽트는 무조건 off되고 그 전의 상태를 알 수 없기 때문에 `eflags`를 확인한다. 이 때 `r11`을 확인하는 이유는 시스템콜이 호출되면 자동으로 레지스터들에 해당하는 값이 들어간다. 인터럽트가 막혀있었다면 놔두고 인터럽트가 활성화 된 상태였다면 인터럽트를 활성화시켜준다.

```
no_sti:
	movabs $syscall_handler, %r12
	call *%r12
```

드디어 시스템콜 핸들러를 호출할 때가 왔다. `r12`에 시스템콜 핸들러 함수의 주소를 담아 실행한다. 시스템콜 핸들러는 커널 스택의 `intr_frame`의 모양으로 담겨져있는 것을 받아 수행한다.

```
	popq %r15
	popq %r14
	popq %r13
	popq %r12
	popq %r11
	popq %r10
	popq %r9
	popq %r8
	popq %rsi
	popq %rdi
	popq %rbp
	popq %rdx
	popq %rcx
	popq %rbx
	popq %rax
	addq $32, %rsp
	popq %rcx              /* if->rip */
	addq $8, %rsp
	popq %r11              /* if->eflags */
	popq %rsp              /* if->rsp */
	sysretq

.section .data
.globl temp1
temp1:
.quad	0
.globl temp2
temp2:
.quad	0
```

시스템콜 핸들러의 수행이 끝나고나면 해당 정보를 다시 레지스터에 담아 이전에 하던 작업을 `sysret`으로 돌아가서 수행하게 된다.