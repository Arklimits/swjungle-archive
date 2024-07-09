# 과제 설명
> 💡 여러분의 핀토스가 리눅스의 stdin, stdout 를 닫는 기능과 dup2 시스템 콜을 지원하도록 만들어보세요.
> 
> - 현재 구현된 핀토스에서는 stdin과 stdout 파일 디스크립터를 닫는 것이 금지되어있다. 이번 Extend File Descriptor 과제에서 여러분의 핀토스가 리눅스처럼 유저가 stdin과 stdout을 닫을 수 있도록 해야한다. 즉, 프로세스가 stdin 를 닫으면 절대 input을 읽을 수 없고, stdout을 닫으면 어떤 것도 출력할 수 없게 만들어야 한다.
> - 또한 dup2()라는 시스템 콜을 구현해야 하는데, dup2() 시스템 콜은 인자로 받은 oldfd 파일 디스크립터의 복사본을 생성하고, 이 복사본의 파일디스크립터 값은 인자로 받은 newfd 값이 되게 한다. dup2() 시스템 콜이 파일 디스크립터를 복사해서 새 파일 디스크립터를 생성하는 데 성공한다면 newfd를 리턴한다. 만약 newfd 값이 이전에 이미 열려있었다면, newfd는 재사용되기 전에 조용히 닫힌다.
> - 아래 사항들을 기억해야한다.
>   - 만약 oldfd 가 유효한 파일 디스크립터가 아니라면, dup2() 콜은 실패하여 1을 반환하고, newfd 는 닫히지 않는다.
>   - 만약 oldfd 가 유효한 파일 디스크립터이고, newfd는 oldfd와 같은 값을 가지고 있다면 dup2()가 할일은 따로 없고 (*이미 같으므로) newfd 값을 리턴한다.
>   - 이 시스템콜로부터 성공적으로 값을 반환받은 후에, oldfd와 newfd는 호환해서 사용이 가능하다. 이 둘은 서로 다른 파일 디스크립터이긴하지만, 똑같은 열린 파일 디스크립터를 의미하기 때문에 같은 file offset과 status flags 를 공유하고 있다. 예를 들어 만약에 다른 디스크립터가 seek 을 사용해서 file offset이 수정되었다면, 다른 스크립터에서도 이 값은 똑같이 수정된다.

# 과제 목표
> - stdin에 대한 fd를 닫아주면, 그 어떤 input도 읽어들이지 않는다.
> - stdout에 대한 fd를 닫아주면, 그 어떤 output도 프로세스에 의해 출력되지 않는다.
> - dup2 를 구현해 주어진 stdin/stdout/file 에 대한 fd를 fd_table 내에 복사해준다.

# 테스트 방법
> - userprog/Make.var를 열어보면, 아래 세 줄이 주석처리 되어있는데, 주석을 해제해주면 dup2 TC 두개가 추가된다.
>  
> ![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/e8b4a1ab-6ed4-402d-87fa-ef31cac4f1c8)
> 
> ![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/52bd64e0-0ed4-478a-a5ae-0f634e190908)

# 구현
## 1. 자료구조 추가 및 변수 추가
> 1.1. STDIN, STDOUT, STDERR 정의
```c
// process.h

#define STDIN 1
#define STDOUT 2
#define STDERR 3
```

> 1.2. dup_count 선언 및 초기화
>   - filesys/file.c 파일에 있던 file 구조체 -> include/filesys/file.h로 이동
>   - dup_count 선언
>   - stdbool.h include
>   - file_open() 함수에 dup_count 초기화

```c
// file.h
#include <stdbool.h>

struct inode;

/* An open file. */
struct file {
	struct inode *inode;        /* File's inode. */
	off_t pos;                  /* Current position. */
	bool deny_write;            /* Has file_deny_write() been called? */

    /** Project 2-Extend File Descriptor */
    int dup_count;
};
```

```c
// file.c

struct file *file_open (struct inode *inode) {
	struct file *file = calloc (1, sizeof *file);
	if (inode != NULL && file != NULL) {
		file->inode = inode;
		file->pos = 0;
		file->deny_write = false;

		/** Project 2-Extend File Descriptor */
		file->dup_count = 0;
		
		return file;
	} else {
		inode_close (inode);
		free (file);
		return NULL;
	}
}
```

> 1.3. fdt에 stdin, stdout, stderr 설정
```c
// thread.c

thread_create(){
  // ...

  t->fdt[0] = STDIN;   
  t->fdt[1] = STDOUT; 
  t->fdt[2] = STDERR;

  // ...
}
```

## 2. dup2() 구현
> 2.1. 시스템 콜 핸들러에 dup2 추가
```c
// syscall.c

void syscall_init (void) {
	write_msr(MSR_STAR, ((uint64_t)SEL_UCSEG - 0x10) << 48  |
			((uint64_t)SEL_KCSEG) << 32);
	write_msr(MSR_LSTAR, (uint64_t) syscall_entry);

	/* The interrupt service rountine should not serve any interrupts
	 * until the syscall_entry swaps the userland stack to the kernel
	 * mode stack. Therefore, we masked the FLAG_FL. */
	write_msr(MSR_SYSCALL_MASK,
			FLAG_IF | FLAG_TF | FLAG_DF | FLAG_IOPL | FLAG_AC | FLAG_NT);
    
    /** project2-System Call */
    // read & write 용 lock 초기화
    lock_init(&filesys_lock);
}

/* The main system call interface */
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
        case SYS_DUP2:
            f->R.rax = dup2(f->R.rdi, f->R.rsi);
            break;
        default:
            exit(-1);
    }
}
```

> 2.2. dup2 선언 및 구현
```c
// syscall.h

/** Project 2-Extend File Descriptor */
int dup2(int oldfd, int newfd);
userprog/syscall.c
/** Project 2-Extend File Descriptor */
int dup2(int oldfd, int newfd) {
    if (oldfd < 0 || newfd < 0)
        return -1;

    struct file *oldfile = process_get_file(oldfd);

    if (oldfile == NULL)
        return -1;

    if (oldfd == newfd)
        return newfd;

    struct file *newfile = process_get_file(newfd);

    if (oldfile == newfile)
        return newfd;

    close(newfd);

    newfd = process_insert_file(newfd, oldfile);

    return newfd;
}
```

> 2.3. process_insert_file() 선언 및 구현
```c
// process.h

/** Project 2-Extend File Descriptor */
process_insert_file(int fd, struct file *f);
userprog/process.c
/** Project 2-Extend File Descriptor */
process_insert_file(int fd, struct file *f) {
    struct thread *curr = thread_current();
    struct file **fdt = curr->fdt;

    if (fd < 0 || fd >= FDCOUNT_LIMIT)
        return -1;

    if (f > STDERR)
        f->dup_count++;

    fdt[fd] = f;

    return fd;
}
```

## 3. read() 추가 구현

```c
// syscall.c
/** Project 2-Extend File Descriptor */
int read(int fd, void *buffer, unsigned length) {
    struct thread *curr = thread_current();
    check_address(buffer);

    struct file *file = process_get_file(fd);

    if (file == STDIN) { 
        int i = 0; 
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

    if (file == NULL || file == STDOUT || file == STDERR)  // 빈 파일, stdout, stderr를 읽으려고 할 경우
        return -1;

    off_t bytes = -1;

    lock_acquire(&filesys_lock);
    bytes = file_read(file, buffer, length);
    lock_release(&filesys_lock);

    return bytes;
}
```

## 4. write() 추가 구현

```c
// syscall.c

/** Project 2-Extend File Descriptor */
int write(int fd, const void *buffer, unsigned length) {
    check_address(buffer);

    struct thread *curr = thread_current();
    off_t bytes = -1;

    struct file *file = process_get_file(fd);

    if (file == STDIN || file == NULL)  
        return -1;

    if (file == STDOUT) { 

        putbuf(buffer, length);
        return length;
    }

    if (file == STDERR) { 

        putbuf(buffer, length);
        return length;
    }

    lock_acquire(&filesys_lock);
    bytes = file_write(file, buffer, length);
    lock_release(&filesys_lock);

    return bytes;
}
```

## 5. close() 추가 구현

```c
// syscall.c

/** Project 2-Extend File Descriptor */
void close(int fd) {
    struct thread *curr = thread_current();
    struct file *file = process_get_file(fd);

    if (file == NULL)
        return;

    process_close_file(fd);

    if (file == STDIN) {
        file = 0;
        return;
    }

    if (file == STDOUT) {
        file = 0;
        return;
    }

    if (file == STDERR) {
        file = 0;
        return;
    }

    if (file->dup_count == 0)
        file_close(file);
    else
        file->dup_count--;
}
```

## 6. do_fork() 추가 구현

```c
// process.c

static void __do_fork (void *aux) {
	struct intr_frame if_;
	struct thread *parent = (struct thread *) aux;
	struct thread *current = thread_current ();
	
	/* TODO: somehow pass the parent_if. (i.e. process_fork()'s if_) */
	struct intr_frame *parent_if = &parent->parent_if;
	bool succ = true;

	/* 1. Read the cpu context to local stack. */
	memcpy (&if_, parent_if, sizeof (struct intr_frame));
    if_.R.rax = 0;  // 자식 프로세스의 return값 (0)

	/* 2. Duplicate PT */
	current->pml4 = pml4_create();
	if (current->pml4 == NULL)
		goto error;

	process_activate (current);
#ifdef VM
	supplemental_page_table_init (&current->spt);
	if (!supplemental_page_table_copy (&current->spt, &parent->spt))
		goto error;
#else
	if (!pml4_for_each (parent->pml4, duplicate_pte, parent))
		goto error;
#endif

	/* TODO: Your code goes here.
	 * TODO: Hint) To duplicate the file object, use `file_duplicate`
	 * TODO:       in include/filesys/file.h. Note that parent should not return
	 * TODO:       from the fork() until this function successfully duplicates
	 * TODO:       the resources of parent.*/

    if (parent->fd_idx >= FDCOUNT_LIMIT)
        goto error;

	/** Project 2-Extend File Descriptor */

    struct file *file;

    for (int fd = 0; fd < FDCOUNT_LIMIT; fd++) {
        file = parent->fdt[fd];
        if (file == NULL)
            continue;

        if (file > STDERR)
            current->fdt[fd] = file_duplicate(file);
        else
            current->fdt[fd] = file;
	}

    current->fd_idx = parent->fd_idx;
    sema_up(&current->fork_sema);  

    process_init();

    /* Finally, switch to the newly created process. */
    if (succ)
        do_iret(&if_);  // 정상 종료 시 자식 Process를 수행하러 감

error:
	
    sema_up(&current->fork_sema);  // 복제에 실패했으므로 현재 fork용 sema unblock
    exit(TID_ERROR);
}
```

테스트 결과

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/356275fc-dafc-43a5-bdec-e6a4c7c849c8)
