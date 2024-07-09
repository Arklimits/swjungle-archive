# 과제 설명
핀토스에는 파일 디스크립터 부분이 누락되어있다. 파일 입출력을 위해서는 파일 디스크립터의 구현이 필요하다.

- 구현할 시스템 콜
  `open()` : 파일을 열 때 사용하는 시스템 콜
  `filesize()` : 파일의 크기를 알려주는 시스템 콜
  `read()` : 열린 파일의 데이터를 읽는 시스템 콜
  `write()` : 열린 파일의 데이터를 기록 시스템 콜
  `seek()` : 열린 파일의 위치(offset)를 이동하는 시스템 콜
  `tell()` : 열린 파일의 위치(offset)를 알려주는 시스템 콜
  `close()` : 열린 파일을 닫는 시스템 콜
  
# 수정 및 추가 함수
- `tid_t thread_create (const char *name, int priority, thread_func *function, void *aux)` : function 함수를 수행하는 스레드 생성
- `void process_exit (void)` : 프로세스 종료 시 호출되어 프로세스의 자원을 해제
- `int process_add_file (struct file *f)` : 파일 객체에 대한 파일 디스크립터 생성
- `struct file *process_get_file (int fd)` : 프로세스의 파일 디스크립터 테이블을 검색하여 파일 객체의 주소를 리턴
- `void process_close_file (int fd)` : 파일 디스크립터에 해당하는 파일을 닫고 해당 엔트리 초기화

# 구현
## 1. thread 자료구조 수정
> 1.1. 페이지 할당

```c
// thread.h

/** project2-System Call */
#define FDT_PAGES     3                     // test `multi-oom` 테스트용
#define FDCOUNT_LIMIT FDT_PAGES * (1 << 9)  // 엔트리가 512개 인 이유: 페이지 크기 4kb / 파일 포인터 8byte
```

> 1.2. fd_idx, fdt 선언
```c
// thread.h

/** project2-System Call */
int exit_status;

int fd_idx;              // 파일 디스크립터 인덱스
struct file **fdt;       // 파일 디스크립터 테이블
```

> 1.3. thread_create() 수정
```c
// thread.c

tid_t thread_create (const char *name, int priority, thread_func *function, void *aux) {
	struct thread *t;
	tid_t tid;

	ASSERT (function != NULL);

	/* Allocate thread. */
	t = palloc_get_page (PAL_ZERO);
	if (t == NULL)
		return TID_ERROR;

	/* Initialize thread. */
	init_thread (t, name, priority);
	tid = t->tid = allocate_tid ();

	#ifdef USERPROG
	/** project2-System Call */
  t->fdt = palloc_get_multiple(PAL_ZERO, FDT_PAGES);
  if (t->fdt == NULL)
    return TID_ERROR;

  t->exit_status = 0;  // exit_status 초기화

  t->fd_idx = 3;
  t->fdt[0] = 0;  // stdin 예약된 자리 (dummy)
  t->fdt[1] = 1;  // stdout 예약된 자리 (dummy)
  t->fdt[2] = 2;  // stderr 예약된 자리 (dummy)
  
  // ...

	return tid;
}
```

## 2. File Descriptor 구현
> 2.1. 구현할 함수 선언
```c
// process.h

/** project2-System Call */
int process_add_file(struct file *f);
struct file *process_get_file(int fd);
int process_close_file(int fd);
```

> 2.2. process_add_file() 함수 구현
```c
// process.c

// 현재 스레드 fdt에 파일 추가
int process_add_file(struct file *f) {
    struct thread *curr = thread_current();
    struct file **fdt = curr->fdt;

    if (curr->fd_idx >= FDCOUNT_LIMIT)
        return -1;

    fdt[curr->fd_idx++] = f;

    return curr->fd_idx - 1;
}
```

> 2.3. process_get_file() 함수 구현
```c
// process.c

// 현재 스레드의 fd번째 파일 정보 얻기
struct file *process_get_file(int fd) {
    struct thread *curr = thread_current();

    if (fd >= FDCOUNT_LIMIT)
        return NULL;

    return curr->fdt[fd];
}
```

> 2.4. process_close_file() 함수 구현
```c
// process.c

// 현재 스레드의 fdt에서 파일 삭제
int process_close_file(int fd) {
    struct thread *curr = thread_current();

    if (fd >= FDCOUNT_LIMIT)
        return -1;

    curr->fdt[fd] = NULL;
    return 0;
}
```

> 2.5. process_exit() 함수 수정
```c
// process.c

void process_exit (void) {
	struct thread *curr = thread_current ();
	/* TODO: Your code goes here.
	 * TODO: Implement process termination message (see
	 * TODO: project2/process_termination.html).
	 * TODO: We recommend you to implement process resource cleanup here. */

	    for (int fd = 0; fd < curr->fd_idx; fd++)  // FDT 비우기
        close(fd);

    file_close(curr->runn_file);  // 현재 프로세스가 실행중인 파일 종료

    palloc_free_multiple(curr->fdt, FDT_PAGES);

    process_cleanup();

    sema_up(&curr->wait_sema);  // 자식 프로세스가 종료될 때까지 대기하는 부모에게 signal

    sema_down(&curr->exit_sema);  // 부모 프로세스가 종료될 떄까지 대기
}
```

## 3. lock 사용
- Read, write시 파일에 대한 동시접근이 일어날 수 있으므로 Lock 사용
  - syscall.h 에 global lock으로 선언 (struct lock filesys_lock)
  - syscall_init()에서 lock 초기화 (lock_init() 호출)
  - 각 시스템 콜에서 lock 획득 후 시스템 콜 처리, 시스템 콜 완료 시 lock 반납
```c
// syscall.c

/** project2-System Call */
struct lock filesys_lock;  // 파일 읽기/쓰기 용 lock

// ...

void syscall_init (void) {
	write_msr(MSR_STAR, ((uint64_t)SEL_UCSEG - 0x10) << 48  |
			((uint64_t)SEL_KCSEG) << 32);
	write_msr(MSR_LSTAR, (uint64_t) syscall_entry);

	/* The interrupt service rountine should not serve any interrupts
	 * until the syscall_entry swaps the userland stack to the kernel
	 * mode stack. Therefore, we masked the FLAG_FL. */
	write_msr(MSR_SYSCALL_MASK,	FLAG_IF | FLAG_TF | FLAG_DF | FLAG_IOPL | FLAG_AC | FLAG_NT);
    
  /** project2-System Call */
  // read & write 용 lock 초기화
  lock_init(&filesys_lock);
}
```

## 4. 시스템 콜 구현
> 4.1. 구현할 시스템 콜 선언

```c
// syscall.h

int open(const char *file);
int filesize(int fd);
int read(int fd, void *buffer, unsigned length);
int write(int fd, const void *buffer, unsigned length);
void seek(int fd, unsigned position);
int tell(int fd);
void close(int fd);
```

> 4.1. open() 구현
> - 파일을 열 때 사용하는 시스템 콜
> - 파일이 없을 경우 실패
> - 성공 시 fd를 생성하고 반환, 실패 시-1 반환
> - File : 파일의 이름 및 경로 정보

```c
// syscall.c

int open(const char *file) {
    check_address(file);
    struct file *newfile = filesys_open(file);

    if (newfile == NULL)
        return -1;

    int fd = process_add_file(newfile);

    if (fd == -1)
        file_close(newfile);

    return fd;
}

> 4.2. filesize() 구현
> 파일의 크기를 알려주는 시스템 콜
> 성공 시 파일의 크기를 반환, 실패 시-1 반환
```c
// syscall.c

int filesize(int fd) {
    struct file *file = process_get_file(fd);

    if (file == NULL)
        return -1;

    return file_length(file);
}
```

> 4.3. read() 구현
> - 열린 파일의 데이터를 읽는 시스템 콜
> - 성공 시 읽은 바이트 수를 반환, 실패 시-1 반환
> - buffer : 읽은 데이터를 저장할 버퍼의 주소 값
> - size : 읽을 데이터 크기
> - fd 값이 0일 때 키보드의 데이터를 읽어 버퍼에 저장. (input_getc() 이용)

```c
// syscall.c

int read(int fd, void *buffer, unsigned length) {
    check_address(buffer);

    if (fd == 0) {  // 0(stdin) -> keyboard로 직접 입력
        int i = 0;  // 쓰레기 값 return 방지
        char c;
        unsigned char *buf = buffer;

        for (; i < length; i++) {
            c = input_getc();
            *buf++ = c;
            if (c == '\0')
                break;
        }

        return i;
    }
    // 그 외의 경우
    if (fd < 3)  // stdout, stderr를 읽으려고 할 경우 & fd가 음수일 경우
        return -1;

    struct file *file = process_get_file(fd);
    off_t bytes = -1;

    if (file == NULL)  // 파일이 비어있을 경우
        return -1;

    lock_acquire(&filesys_lock);
    bytes = file_read(file, buffer, length);
    lock_release(&filesys_lock);

    return bytes;
}
```

> 4.4. write() 구현
> - 열린 파일의 데이터를 기록 시스템 콜
> - 성공 시 기록한 데이터의 바이트 수를 반환, 실패시-1 반환
> - buffer : 기록 할 데이터를 저장한 버퍼의 주소 값
> - size : 기록할 데이터 크기
> - fd 값이 1일 때 버퍼에 저장된 데이터를 화면에 출력. (putbuf() 이용)

```c
// syscall.c

int write(int fd, const void *buffer, unsigned length) {
    check_address(buffer);

    off_t bytes = -1;

    if (fd <= 0)  // stdin에 쓰려고 할 경우 & fd 음수일 경우
        return -1;

    if (fd < 3) {  // 1(stdout) * 2(stderr) -> console로 출력
        putbuf(buffer, length);
        return length;
    }

    struct file *file = process_get_file(fd);

    if (file == NULL)
        return -1;

    lock_acquire(&filesys_lock);
    bytes = file_write(file, buffer, length);
    lock_release(&filesys_lock);

    return bytes;
}
```

> 4.5. seek() 구현
> - 열린 파일의 위치(offset)를 이동하는 시스템 콜
> - Position : 현재 위치(offset)를 기준으로 이동할 거리

```c
// syscall.c

void seek(int fd, unsigned position) {
    struct file *file = process_get_file(fd);

    if (fd < 3 || file == NULL)
        return;

    file_seek(file, position);
}
```

> 4.6. tell() 구현
> - 열린 파일의 위치(offset)를 알려주는 시스템 콜
> - 성공 시 파일의 위치(offset)를 반환, 실패 시 -1 반환

```c
// syscall.c

int tell(int fd) {
    struct file *file = process_get_file(fd);

    if (fd < 3 || file == NULL)
        return -1;

    return file_tell(file);
}
```

> 4.7. close() 구현
> - 열린 파일을 닫는 시스템 콜
> - 파일을 닫고 File Descriptor를 제거

```c
// syscall.c

void close(int fd) {
    struct file *file = process_get_file(fd);

    if (fd < 3 || file == NULL)
        return;

    process_close_file(fd);

    file_close(file);
}
```

## 5. page_fault() 수정
- create-bad-ptr, read-bad-ptr 등 15개의 test case에서 Page fault 발생
  - Page fault 에러 메시지 출력으로 인해 test case가 fail 처리 됨
  - 에러 메시지 출력을 방지하기 위해 exit(-1) 을 호출 하도록 수정

```c
// exception.c

static void page_fault (struct intr_frame *f) {
	bool not_present;  /* True: not-present page, false: writing r/o page. */
	bool write;        /* True: access was write, false: access was read. */
	bool user;         /* True: access by user, false: access by kernel. */
	void *fault_addr;  /* Fault address. */

	/* Obtain faulting address, the virtual address that was
	   accessed to cause the fault.  It may point to code or to
	   data.  It is not necessarily the address of the instruction
	   that caused the fault (that's f->rip). */

	fault_addr = (void *) rcr2();

	/* Turn interrupts back on (they were only off so that we could
	   be assured of reading CR2 before it changed). */
	intr_enable ();


	/* Determine cause. */
	not_present = (f->error_code & PF_P) == 0;
	write = (f->error_code & PF_W) != 0;
	user = (f->error_code & PF_U) != 0;

	/** project2-System Call */
	exit(-1);

  // ...
}
```
