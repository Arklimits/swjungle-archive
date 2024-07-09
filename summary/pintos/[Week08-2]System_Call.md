# 과제 설명
우선 project 2를 진행하기 위해 순서를 정해야한다.

추천하는 방식은

> 1️⃣ #3 System Call -> #5 File Descriptor -> #4. Hierarchical Process Structure
> 2️⃣ File related -> Process related

1번은 한양대 강의자료의 순서를 구현하기 쉽게 임의로 바꾼 것이고,
2번은 카이스트 강의자료의 순서이다.

2번으로 하면 file related에서 분량이 길어지기에, 1번으로 진행하려 한다.

핀토스는 시스템 콜 핸들러가 구현되어 있지 않아 시스템 콜이 처리되지 않는다.

> 🔍 System Call이란?
> 시스템 콜은 사용자가 커널 영역에 접근하고 싶을 때, 원하는 목적을 대신해서 작업하는 프로그래밍 인터페이스이다. 그렇기 때문에 시스템 콜은 커널 모드에서 실행되고, 작업 후 사용자 모드로 복귀한다. PintOS에서는 이를 시스템 콜 핸들러를 통해 시스템 콜을 호출한다.

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/305de4fd-04c8-4757-b1d9-f333489682cd)

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/bef3305f-2a6f-4503-9524-1ebe111364e5)

시스템 콜(halt, exit,create, remove)을 구현하고 시스템 콜 핸들러를 통해 호출해야한다.

> 💡시스템 콜 핸들러 및 시스템 콜 (halt, exit, create, remove) 구현하자!!

Gitbook에서 시스템 콜 과제 이전에 나오는 User memory access는 시스템 콜을 구현할 때 메모리에 접근할 텐데, 이때 접근하는 메모리 주소가 유저 영역인지 커널 영역인지를 체크하라는 과제이다. 이는 한양대 강의자로 pdf 시스템 콜 과제에 있는 check_address()에 해당하므로 이 함수부터 구현하고 시작한다.

# 구현
1. syscall_handler() 함수 구현
* 시스템 콜을 호출할 때, 원하는 기능에 해당하는 시스템 콜 번호를 rax에 담는다. 그리고 시스템 콜 핸들러는 rax의 숫자로 시스템 콜을 호출하고, 해당 콜의 반환값을 다시 rax에 담아서 intr frame(인터럽트 프레임)에 저장한다.

> 💡 중요
> void 형식에 return 추가해야함. (디버깅하다 발견한 사실)

> 💡 중요
> fork에 인자 개수를 변경하여 구현할 수 있는데, 임의로 변경 하면 안된다!!!

```c
// syscall.c
void syscall_handler (struct intr_frame *f UNUSED) {
	// TODO: Your implementation goes here.
	/** project2-System Call */
  int sys_number = f->R.rax;

    // Argument 순서
    // %rdi %rsi %rdx %r10 %r8 %r9

    switch (sys_number) {
        case SYS_HALT:
            halt();
            break;
        case SYS_EXIT:
            exit(f->R.rdi);
            break;
        case SYS_FORK:
            f->R.rax = fork(f->R.rdi);
            break;
        case SYS_EXEC:
            f->R.rax = exec(f->R.rdi);
            break;
        case SYS_WAIT:
            f->R.rax = process_wait(f->R.rdi);
            break;
        case SYS_CREATE:
            f->R.rax = create(f->R.rdi, f->R.rsi);
            break;
        case SYS_REMOVE:
            f->R.rax = remove(f->R.rdi);
            break;
        case SYS_OPEN:
            f->R.rax = open(f->R.rdi);
            break;
        case SYS_FILESIZE:
            f->R.rax = filesize(f->R.rdi);
            break;
        case SYS_READ:
            f->R.rax = read(f->R.rdi, f->R.rsi, f->R.rdx);
            break;
        case SYS_WRITE:
            f->R.rax = write(f->R.rdi, f->R.rsi, f->R.rdx);
            break;
        case SYS_SEEK:
            seek(f->R.rdi, f->R.rsi);
            break;
        case SYS_TELL:
            f->R.rax = tell(f->R.rdi);
            break;
        case SYS_CLOSE:
            close(f->R.rdi);
            break;
        default:
            exit(-1);
    }
}
```

2. check_address() 함수 구현
* 주소 값이 유저 영역에서 사용하는 주소 값인지 확인
* 유저 영역을 벗어난 영역일 경우 프로세스 종료(exit(-1))

```c
// syscall.c

/** project2-System Call */
void check_address (void *addr) {
    if (is_kernel_vaddr(addr) || addr == NULL || pml4_get_page(thread_current()->pml4, addr) == NULL)
        exit(-1);
}
```

3. System Call 함수 선언
```c
// syscall.h

/** project2-System Call */
#include <stdbool.h>
void check_address(void *addr);
void halt(void);
void exit(int status);
bool create(const char *file, unsigned initial_size);
bool remove(const char *file);
```

4. halt() 함수 구현
* 핀토스를 종료시키는 시스템 콜

```c
// syscall.c

void halt(void) {
    power_off();
}
```

5. exit_status 자료 구조 추가 및 초기화
* exit(), wait() 구현 때 사용될 exit_status를 추가하고 초기화 해주어야한다.

```c
// thread.h

#define USERPROG 
#ifdef USERPROG
	/* Owned by userprog/process.c. */
	uint64_t *pml4;                     /* Page map level 4 */
	/** project2-System Call */
	int exit_status;
#endif
```

```c
// thread.c

    t->original_priority = t->priority;
    t->niceness = NICE_DEFAULT;
    t->recent_cpu = RECENT_CPU_DEFAULT;

	/** project2-System Call */
	t->exit_status = 0;
}
```

6. exit() 함수 구현
- 현재 프로세스를 종료시키는 시스템 콜
- 종료 시 “프로세스 이름: exit(status)” 출력 (Process Termination Message)
- 정상적으로 종료 시 status는 0
- status : 프로그램이 정상적으로 종료됐는지 확인

```c
// syscall.c

void exit(int status) {
    struct thread *t = thread_current();
    t->exit_status = status;
    printf("%s: exit(%d)\n", t->name, t->exit_status); // Process Termination Message
    thread_exit();
}
```

- Process Termination Message를 미리 구현해두자.


7. create() 함수 구현
- 파일을 생성하는 시스템 콜
- 성공 일 경우 true, 실패 일 경우 false 리턴
- file : 생성할 파일의 이름 및 경로 정보
- initial_size : 생성할 파일의 크기

```c
// syscall.c

bool create(const char *file, unsigned initial_size) {
    check_address(file);

    return filesys_create(file, initial_size);
}
```

8. remove() 함수 구현

- 파일을 삭제하는 시스템 콜
- file : 제거할 파일의 이름 및 경로 정보
- 성공 일 경우 true, 실패 일 경우 false 리턴

```c
// syscall.c

bool remove(const char *file) {
    check_address(file);

    return filesys_remove(file);
}
