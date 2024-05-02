# Thread.c에서 Context Switching 함수 분석을 해보자

다음 함수는 CPU Register를 흉내내어 만들어져 있는 구조체에서 `%rsp`를 포인터로 사용해 CPU Register로 올리는 함수이다.
```c
// thread.c
void do_iret(struct intr_frame *tf) {
    /* Structure -> CPU Register로 데이터 이동 (Load) */
    __asm __volatile(             // 입력한 그대로 사용
        "movq %0, %%rsp\n"        // 인자 *tf의 주소를 RSP에 저장
```
이 때, `%0`을 처음보게 되어 무슨 뜻인지 몰랐었는데, 이는 `input operands`로 들어온 **0번째 인자**를 가리키게 되며 컴파일러는 먼저 가서 확인 한 후 돌아와 할당하게 된다.
```c
        "movq 0(%%rsp),%%r15\n"   // rsp위치의 값(stack 시작)을 레지스터 r15에 저장
        "movq 8(%%rsp),%%r14\n"   // rsp+8위치의 값을 레지스터 r14에 저장
        "movq 16(%%rsp),%%r13\n"  // rsp+16위치의 값을 레지스터 r16에 저장
        "movq 24(%%rsp),%%r12\n"  // rsp+24 위치의 값을 레지스터 r12에 저장
        "movq 32(%%rsp),%%r11\n"
        "movq 40(%%rsp),%%r10\n"
        "movq 48(%%rsp),%%r9\n"
        "movq 56(%%rsp),%%r8\n"
        "movq 64(%%rsp),%%rsi\n"  // ...
        "movq 72(%%rsp),%%rdi\n"
        "movq 80(%%rsp),%%rbp\n"
        "movq 88(%%rsp),%%rdx\n"
        "movq 96(%%rsp),%%rcx\n"   // rsp+96 위치의 값을 레지스터 rcx에 저장
        "movq 104(%%rsp),%%rbx\n"  // rsp+104 위치의 값을 레지스터 rbx에 저장
        "movq 112(%%rsp),%%rax\n"  // rsp+112 위치의 값을 레지스터 rax에 저장

        "addq $120,%%rsp\n"     // rsp 위치를 정수 레지스터 다음으로 이동-> rsp->es
        "movw 8(%%rsp),%%ds\n"  // rsp+8위치의 값을 레지스터 ds(data segment)에 저장
        "movw (%%rsp),%%es\n"   // rsp 위치의 값을 레지스터 es(extra segment)에 저장

        "addq $32, %%rsp\n"  // rsp 위치를 rsp+32로 이동. rsp->rip
        "iretq"              // rip 이하(cs, eflags, rsp, ss) 인터럽트 프레임에서 CPU로 복원. (직접 ACCESS 불가능)
        :                    // 인터럽트 프레임의 rip 값을 복원함으로서 기존에 수행하던 스레드의 다음 명령 실행 ... ?
        : "g"((uint64_t)tf)  // g=인자. 0번 인자로 tf를 받음
        : "memory");
}
```
이 때, `iret`은 `%rsp`를 기준으로 나머지 정보들을 **`atomic`하게 register에 한번에 등록**해주고 `return`한다. 그렇지 않다면 복원 중간에 `rsp`나 `rip` 둘중 하나가 먼저 바뀔텐데, `rip`가 먼저 바뀌면 `rsp`가 복원되기 전에 유저 레벨 프로그램이 실행될 것이고, `rsp`가 먼저 복원되면 `rip`가 저장되어 있는 커널 스택이 아닌 유저 스택에서 값을 읽어오게 되어 틀린 행동을 하게 될 것이다.

<br>

다음은 CPU Register에서 갖고 있던 Register를 구조체로 옮기는 작업을 하는 함수이다. 다른 쓰레드로 전환을 하기 때문에 현재 Register의 정보를 백업하는 것이다.
```c
// thread.c
static void thread_launch(struct thread *th) {
    uint64_t tf_cur = (uint64_t)&running_thread()->tf;
    uint64_t tf = (uint64_t)&th->tf;
    ASSERT(intr_get_level() == INTR_OFF);

    /* 주요 스위칭 로직. */
    __asm __volatile(
        /* 레지스터 정보를 Stack에 임시로 저장. */
        "push %%rax\n"  // Stack에 rax위치의 값 저장
        "push %%rbx\n"  // Stack에 rbx위치의 값 저장
        "push %%rcx\n"  // Stack에 rcs위치의 값 저장
```
`rax`, `rbx`, `rcx`를 포인터로 사용하기 위해 미리 값들을 `stack`에 넣어 놓는다.
```c
        /* 현재 CPU Register -> Structure 로 데이터 이동 (Backup) */
        "movq %0, %%rax\n"          // 0번 인자의 주소를 레지스터 rax에 저장
        "movq %1, %%rcx\n"          // 1번 인자의 주소를 레지스터 rcx에 저장
        "movq %%r15, 0(%%rax)\n"    // 레지스터 r15의 값을 rax+0 위치에 저장
        "movq %%r14, 8(%%rax)\n"    // 레지스터 r14의 값을 rax+8 위치에 저장
        "movq %%r13, 16(%%rax)\n"   // 레지스터 r13의 값을 rax+16 위치에 저장
        "movq %%r12, 24(%%rax)\n"   // 레지스터 r12의 값을 rax+24 위치에 저장
        "movq %%r11, 32(%%rax)\n"   // 레지스터 r11의 값을 rax+32 위치에 저장
        "movq %%r10, 40(%%rax)\n"   // 레지스터 r10의 값을 rax+40 위치에 저장
        "movq %%r9, 48(%%rax)\n"    // 레지스터 r9의 값을 rax+48 위치에 저장
        "movq %%r8, 56(%%rax)\n"    // 레지스터 r8의 값을 rax+56 위치에 저장
        "movq %%rsi, 64(%%rax)\n"   // 레지스터 rsi의 값을 rax+64 위치에 저장
        "movq %%rdi, 72(%%rax)\n"   // 레지스터 rdi의 값을 rax+72 위치에 저장
        "movq %%rbp, 80(%%rax)\n"   // 레지스터 rbp의 값을 rax+80 위치에 저장
        "movq %%rdx, 88(%%rax)\n"   // 레지스터 rdx의 값을 rax+88 위치에 저장
        "pop %%rbx\n"               // Stack에 저장된 rcx의 값을 rbx 위치에 복원
        "movq %%rbx, 96(%%rax)\n"   // 레지스터 rbx의 값을 rax+96 위치에 저장
        "pop %%rbx\n"               // Stack에 저장된 rbx의 값을 rbx 위치에 복원
        "movq %%rbx, 104(%%rax)\n"  // 레지스터 rbx의 값을 rax+104 위치에 저장
        "pop %%rbx\n"               // Stack에 저장된 rax의 값을 rbx 위치에 복원
        "movq %%rbx, 112(%%rax)\n"  // 레지스터 rbx의 값을 rax+112 위치에 저장
```
`stack`에 넣어 놓았던 정보를 다시 `rbx`에 담아서 구조체에 담는 작업을 한다. **왜 굳이** 이렇게 했냐고 한다면 이는 코드 분석 후에 따로 하겠다.
```c
        "addq $120, %%rax\n"        // 레지스터 rax의 위치를 정수 레지스터 다음으로 이동 rax->es
        "movw %%es, (%%rax)\n"      // es값을 rax의 위치(es)에 저장
        "movw %%ds, 8(%%rax)\n"     // ds값을 rax+8의 위치(ds)에 저장
        "addq $32, %%rax\n"         // 레지스터 rax를 rip 위치로 이동

        "call __next\n"  // "__next"로 레이블된 위치를 스택에 콜
```
컴파일러는 `call`할 때 해당 위치로 이동하며 `stack`에 해당 라인의 주소를 저장한다. 이로써 `__next:`로 명명된 라인으로 이동하고 그 뒤에 `stack`에서 `rbx`에 해당 라인의 주소를 저장할 수 있다.
```c
        "__next:\n"  // "__next" 레이블: 다음으로 이동할 레이블

        "pop %%rbx\n"                          // Stack에 저장한 위치를 rbx에 복원
        "addq $(out_iret -  __next), %%rbx\n"
        "movq %%rbx, 0(%%rax)\n"               // rbx의 위치를 rax+0(rip)에 저장
```
`rbx`의 위치에 `(out_iret - __next)`의 값을 더해주게 되는데 `out_iret` 또한 레이블이기 때문에 `rbx`에는 `out_iret` 이후 `line`의 `address`가 들어가게 된다. 이를 **백업하는 구조체의 `rip`에 담아 놓으면 이 쓰레드가 재개되었을 때 `out_iret` 이후부터 재개**할 것이다. 이 또한 왜 굳이 이렇게 만들었나에 대한 의문이 생길 수 있는데, 이것도 앞의 것과 같이 설명하겠다.
```c
        
        "movw %%cs, 8(%%rax)\n"                // 레지스터 cs의 값을 rax+8(cs)에 저장

        "pushfq\n" 
```
`pushf`는 `flags`를 `stack`에 복사하는 인스트럭션이다. `Software`가 직접 플래그 레지스터에 접근할 수 없기 때문에 이 인스터럭션을 통해 받아와야 한다.
```c
        "popq %%rbx\n"            // Stack에 저장한 내용을 rbx에 복원
        "mov %%rbx, 16(%%rax)\n"  // 레지스터 rbx(eflags)의 값을 rax+16(eflags)에 저장
        "mov %%rsp, 24(%%rax)\n"  // 레지스터 rsp의 값을 rax+24(rsp)에 저장
        "movw %%ss, 32(%%rax)\n"  // 레지스터 ss의 값을 rax+32(rax)에 저장

        "mov %%rcx, %%rdi\n"  // 레지스터 rcx의 값(인자 1번 tf의 주소)을 레지스터 레지스터 rdi로 복사
        "call do_iret\n"      // rdi를 인자로 받아 do_iret 함수 호출
```
`do_iret`함수는 인자를 1개 필요로 하는데, `call`을 하게 되면 `rdi`값을 갖고 함수를 호출하게 된다. 이를 위해 `rcx`에 할당을 해놨던 것.
```c
        "out_iret:\n"           // "out_iret" 레이블: 다음으로 이동할 레이블
        :                       // output operands
        : "g"(tf_cur), "g"(tf)  // input operands
        : "memory");            // list of clobbered registers -> memory의 register들이 asm 실행 전/후 갱신되어야 함
}
```
컴파일러는 `call`할 때 해당 위치로 이동하며 `stack`에 해당 라인의 주소를 저장한다. 이로써 `__next:`로 명명된 라인으로 이동하고 그 뒤에 `stack`에서 `rbx`에 해당 라인의 주소를 저장할 수 있다.

**********************

자, 이제 데이터를 백업할 때 왜 저런 모양인지 살펴보자.
```c
"pop %%rbx\n"               // Stack에 저장된 rcx의 값을 rbx 위치에 복원
"movq %%rbx, 96(%%rax)\n"   // 레지스터 rbx의 값을 rax+96 위치에 저장
```
```c
"addq $(out_iret -  __next), %%rbx\n"
```
이 형태가 굳이 이런 형태를 띄고 있는 이유는 **1가지** 밖에 없다.

`interrupt.h` 파일에 들어가 보면 구조체의 모양을 알 수 있다.

```c
// interrupt.h
struct gp_registers {
    uint64_t r15;
    uint64_t r14;
    uint64_t r13;
    uint64_t r12;
    uint64_t r11;
    uint64_t r10;
    uint64_t r9;
    uint64_t r8;

    /* 인덱스 레지스터 */
    uint64_t rsi;  // Source Index - 출발지 주소 저장
    uint64_t rdi;  // Destination Index - 목적지 주소 저장

    /* 포인터 레지스터 */
    uint64_t rbp;  // Base Pointer - Stack Pointer의 바닥 주소
                   // 눈썰미가 좋다면 rsp가 밖에 있다는 것을 알 수 있다.

    /* 범용 레지스터 */
    uint64_t rdx;  // Data Register - 연산 시 rax를 보조하는 역할 
    uint64_t rcx;  // Counter Register - Count 역할 수행
    uint64_t rbx;  // Base Register - 주소 지정에 사용 및 산수 변수 저장
    uint64_t rax;  // Accumulator - 산술 연산 및 함수 반환값 처리
} __attribute__((packed));
```
```c
// interrupt.h
struct intr_frame {
    struct gp_registers R;  // 정수 레지스터 구간
    uint16_t es;            // Extra Segment - Extra Data 영역
    uint16_t __pad1;
    uint32_t __pad2;
    uint16_t ds;  // Data Segment - 데이터 영역
    uint16_t __pad3;
    uint32_t __pad4;
    /* intr-stubs.S의 intrNN_stub에 의해 푸시됨. */
    uint64_t vec_no; /* Interrupt vector number. */
                     /* CPU는 이를 'EIP' (Extended) Instruction Pointer
                        바로 아래에 두지만 우리는 여기로 옮깁니다. */
    uint64_t error_code;
    /* CPU에 의해 푸시됨.
       중단된 작업의 저장된 레지스터입니다.. */
    uintptr_t rip;  // Instruction Pointer - 다음에 실행될 명령의 주소
    uint16_t cs;    // Code Segment - 명령어 영역
    uint16_t __pad5;
    uint32_t __pad6;
    uint64_t eflags;  // Extended Flags - 플래그
    uintptr_t rsp;    // Stack Pointer - 스택 포인터
    uint16_t ss;      // Stack Segment - 임시 Stack 영역
    uint16_t __pad7;
    uint32_t __pad8;
} __attribute__((packed));
```
눈썰미가 좋다면 알아챘겠지만, 어셈블리 코드를 구현할 때, 이 레지스터의 모양대로 코딩한 것이다. 이렇게 했을 때 `kernel`이 `struct intr_frame`의 값을 읽거나 씀으로써 유저레벨에서 사용하던 레지스터 값을 직접 접근할 수 있다는 이점이 생긴다.

또한 `rsp`가 밖에 있는 이유는 `iret` 수행 시 `qemu`가 시뮬레이션하는 `x86 Arcitecture`의 매뉴얼에 현재 `stack`에 `rip,cs,eflags,rsp,ss`가 순서대로 있어야 한다고 적혀있기 때문에 그와 맞춰주기 때문이다.