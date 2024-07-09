# 과제 설명
- 핀토스는 프로세스간에 부모와 자식관계를 명시하는 정보가 없음
  - 부모와 자식의 구분이 없고 자식 프로세스의 정보를 알지 못하기 때문에 자식의 시작/종료 전에 부모 프로세스가 종료되는 현상이 발생가능 → 프로그램이 실행되지 않음
  - ex) init 프로세스는 유저 프로세스의 정보를 알지 못하기 때문에 사용자 프로그램이 실행 되기 전에 핀토스를 종료
- 프로세스의 정보에 부모와 자식필드를 추가, 이를 관리하는 함수를 제작
  - 자식 프로세스 : 리스트로 구현
  - 자식 리스트에서 원하는 프로세스를 검색, 삭제하는 함수 구현
- 프로세스 디스크립터(struct thread)에 부모 프로세스와 자식 프로세스들을 가리키는 자료구조 추가
- exec(), wait(), fork() 구현 (세마포어를 이용)

# 구현
## 1. 프로세스 계층 구조 구현
> 1.1. 헤더 파일 선언
> ```c
> // thread.h
>
> #include "threads/synch.h" /** project2-System Call */
> ```

> 1.2. thread 자료구조 수정
> ```c
> // thread.h
> 
> #ifdef USERPROG
>	/* Owned by userprog/process.c. */
>	uint64_t *pml4;                     /* Page map level 4 */
>	/** project2-System Call */
>	int exit_status;
>
>	int fd_idx;              // 파일 디스크립터 인덱스
>   struct file **fdt;       // 파일 디스크립터 테이블
>   struct file *runn_file;  // 실행중인 파일
>
>   struct intr_frame parent_if;  // 부모 프로세스 if
>   struct list child_list;
>   struct list_elem child_elem;
>
>	struct semaphore fork_sema;  // fork가 완료될 때 signal
>   struct semaphore exit_sema;  // 자식 프로세스 종료 signal
>   struct semaphore wait_sema;  // exit_sema를 기다릴 때 사용
> #endif
> ```

> 1.3. init_thread() 함수 수정
> ```c
> //thread.c
> static void init_thread (struct thread *t, const char *name, int priority) {
>	ASSERT (t != NULL);
>	ASSERT (PRI_MIN <= priority && priority <= PRI_MAX);
>	ASSERT (name != NULL);
>
>	memset (t, 0, sizeof *t);
>	t->status = THREAD_BLOCKED;
>	strlcpy (t->name, name, sizeof t->name);
>	t->tf.rsp = (uint64_t) t + PGSIZE - sizeof (void *);
>
>	/** project1-Advanced Scheduler */
>    if (thread_mlfqs) {
>        mlfqs_priority(t);
>        list_push_back(&all_list, &t->all_elem);
>    } else {
>        t->priority = priority;
>    }
>
>    t->wait_lock = NULL;
>    list_init(&t->donations);
>
>    t->magic = THREAD_MAGIC;
>
>    /** #Advanced Scheduler */
>    t->original_priority = t->priority;
>    t->niceness = NICE_DEFAULT;
>    t->recent_cpu = RECENT_CPU_DEFAULT;
>
>	/** project2-System Call */
>    t->runn_file = NULL;
>
>    list_init(&t->child_list);
>    sema_init(&t->fork_sema, 0);
>    sema_init(&t->exit_sema, 0);
>    sema_init(&t->wait_sema, 0);
> }
> ```

> 1.4. thread_create() 함수 수정
> ```c
> // thread.c
> 
> tid_t thread_create (const char *name, int priority, thread_func *function, void *aux) {
>	struct thread *t;
>	tid_t tid;
>
>	ASSERT (function != NULL);
>
>	/* Allocate thread. */
>	t = palloc_get_page (PAL_ZERO);
>	if (t == NULL)
>		return TID_ERROR;
>
>	/* Initialize thread. */
>	init_thread (t, name, priority);
>	tid = t->tid = allocate_tid ();
>
>	#ifdef USERPROG
>	/** project2-System Call */
>    t->fdt = palloc_get_multiple(PAL_ZERO, FDT_PAGES);
>    if (t->fdt == NULL)
>        return TID_ERROR;
>
>    t->exit_status = 0;  // exit_status 초기화
>
>    t->fd_idx = 3;
>    t->fdt[0] = 0;  // stdin 예약된 자리 (dummy)
>    t->fdt[1] = 1;  // stdout 예약된 자리 (dummy)
>    t->fdt[2] = 2;  // stderr 예약된 자리 (dummy)
>
>    list_push_back(&thread_current()->child_list, &t->child_elem);
> #endif
>
>	/* Call the kernel_thread if it scheduled.
>	 * Note) rdi is 1st argument, and rsi is 2nd argument. */
>	t->tf.rip = (uintptr_t) kernel_thread;
>	t->tf.R.rdi = (uint64_t) function;
>	t->tf.R.rsi = (uint64_t) aux;
>	t->tf.ds = SEL_KDSEG;
>	t->tf.es = SEL_KDSEG;
>	t->tf.ss = SEL_KDSEG;
>	t->tf.cs = SEL_KCSEG;
>	t->tf.eflags = FLAG_IF;
>
>	/* Add to run queue. */
>	thread_unblock (t);
>
>	/** project1-Priority Scheduling */
>	if(t->priority > thread_current()->priority)
>		thread_yield();
>
>	return tid;
> }
> ```

## 2. get_child_process() 함수 구현

> - 현재 프로세스의 자식 리스트를 검색하여 해당 pid에 맞는 프로세스 디스크립터를 반환
> - struct thread *thread_current(void) : 현재 프로세스의 디스크립터 반환
> - pid를 갖는 프로세스 디스크립터가 존재하지 않을 경우 NULL 반환
> ```c
> // process.c
> /** project2-System Call */
> struct thread *get_child_process(int pid);
> int process_add_file(struct file *f);
> struct file *process_get_file(int fd);
> int process_close_file(int fd);
> → process.h에 함수 선언부터!
> 
> /** project2-System Call */
> struct thread *get_child_process(int pid) {
>    struct thread *curr = thread_current();
>    struct thread *t;
>
>    for (struct list_elem *e = list_begin(&curr->child_list); e != list_end(&curr->child_list); e = list_next(e)) {
>        t = list_entry(e, struct thread, child_elem);
>
>        if (pid == t->tid)
>            return t;
>    }
>
>    return NULL;
> }

## 3. fork 구현 및 관련 함수 구현
> 3.1. fork() 구현
> ```c
> // syscall.c
>
> pid_t fork(const char *thread_name) {
>    check_address(thread_name);
>
>    return process_fork(thread_name, NULL);
> }

> 3.2. process_fork() 함수 구현
> ```c
> // process.c
> tid_t process_fork (const char *name, struct intr_frame *if_ UNUSED) {
>	struct thread *curr = thread_current();
>
>    struct intr_frame *f = (pg_round_up(rrsp()) - sizeof(struct intr_frame));  // 현재 쓰레드의 if_는 페이지 마지막에 붙어있다.
>    memcpy(&curr->parent_if, f, sizeof(struct intr_frame));                    // 1. 부모를 찾기 위해서 2. do_fork에 전달해주기 위해서
>
>    /* 현재 스레드를 새 스레드로 복제합니다.*/
>    tid_t tid = thread_create(name, PRI_DEFAULT, __do_fork, curr);
>
>    if (tid == TID_ERROR)
>        return TID_ERROR;
>
>    struct thread *child = get_child_process(tid);
>
>    sema_down(&child->fork_sema);  // 생성만 해놓고 자식 프로세스가 __do_fork에서 fork_sema를 sema_up 해줄 때까지 대기
>
>    if (child->exit_status == TID_ERROR)
>        return TID_ERROR;
>
>    return tid;  // 부모 프로세스의 리턴값 : 생성한 자식 프로세스의 tid
> }
> ```

> 3.3. duplicate_pte() 함수 구현
> ```c
> // process.c
> 
> static bool duplicate_pte (uint64_t *pte, void *va, void *aux) {
>	  struct thread *current = thread_current ();
>	  struct thread *parent = (struct thread *) aux;
>  	void *parent_page;
>  	void *newpage;
>  	bool writable;
>
>  	/* 1. TODO: If the parent_page is kernel page, then return immediately. */
>  	if (is_kernel_vaddr(va))
>        return true;
>
>	  /* 2. Resolve VA from the parent's page map level 4. */
>   parent_page = pml4_get_page(parent->pml4, va);
>   if (parent_page == NULL)
>       return false;
>
>  	/* 3. TODO: Allocate new PAL_USER page for the child and set result to
>	   *    TODO: NEWPAGE. */
>  	newpage = palloc_get_page(PAL_ZERO);
>   if (newpage == NULL)
>       return false;
>
>	  /* 4. TODO: Duplicate parent's page to the new page and
>  	 *    TODO: check whether parent's page is writable or not (set WRITABLE
>  	 *    TODO: according to the result). */
>  	memcpy(newpage, parent_page, PGSIZE);
>   writable = is_writable(pte);
>
>  	/* 5. Add new page to child's page table at address VA with WRITABLE
>  	 *    permission. */
>  	if (!pml4_set_page (current->pml4, va, newpage, writable)) {
>  		/* 6. TODO: if fail to insert page, do error handling. */
>  		return false;
>  	}
>  return true;
> }
> ```

> 3.4. __do_fork() 함수 구현
> ```c
> // process.c
> 
> static void __do_fork (void *aux) {
>  	struct intr_frame if_;
>  	struct thread *parent = (struct thread *) aux;
>  	struct thread *current = thread_current ();
>  	/* TODO: somehow pass the parent_if. (i.e. process_fork()'s if_) */
>  	struct intr_frame *parent_if = &parent->parent_if;
>  	bool succ = true;
>  
>  	/* 1. Read the cpu context to local stack. */
>  	memcpy (&if_, parent_if, sizeof (struct intr_frame));
>     if_.R.rax = 0;  // 자식 프로세스의 return값 (0)
>  
>  	/* 2. Duplicate PT */
>  	current->pml4 = pml4_create();
>  	if (current->pml4 == NULL)
>  		goto error;
>  
>  	process_activate (current);
> #ifdef VM
>  	supplemental_page_table_init (&current->spt);
>  	if (!supplemental_page_table_copy (&current->spt, &parent->spt))
>  		goto error;
> #else
>  	if (!pml4_for_each (parent->pml4, duplicate_pte, parent))
>  		goto error;
> #endif
>  
>  	/* TODO: Your code goes here.
>  	 * TODO: Hint) To duplicate the file object, use `file_duplicate`
>  	 * TODO:       in include/filesys/file.h. Note that parent should not return
>  	 * TODO:       from the fork() until this function successfully duplicates
>  	 * TODO:       the resources of parent.*/
>  
>     if (parent->fd_idx >= FDCOUNT_LIMIT)
>         goto error;
>  
>     current->fd_idx = parent->fd_idx;  // fdt 및 idx 복제
>     for (int fd = 3; fd < parent->fd_idx; fd++) {
>         if (parent->fdt[fd] == NULL)
>             continue;
>         current->fdt[fd] = file_duplicate(parent->fdt[fd]);
>     }
>  
>     sema_up(&current->fork_sema);  // fork 프로세스가 정상적으로 완료됐으므로 현재 fork용 sema unblock
>  
>     process_init();
>  
>     /* Finally, switch to the newly created process. */
>     if (succ)
>         do_iret(&if_);  // 정상 종료 시 자식 Process를 수행하러 감
>  
> error:
>     sema_up(&current->fork_sema);  // 복제에 실패했으므로 현재 fork용 sema unblock
>     exit(TID_ERROR);
> }
> ```

## 4. exec 구현 및 관련 함수 구현
> - 자식 프로세스를 생성하고 프로그램을 실행시키는 시스템 콜
> - 프로세스를 생성하는 함수 이용 (Command Line Parsing 참조)
> - 프로세스 생성에 성공 시 생성된 프로세스에 pid 값을 반환, 실패 시-1 반환
> - 부모 프로세스는 자식 프로세스의 응용 프로그램이 메모리에 탑재 될 때까지 대기
> - 세마포어 이용
> - cmd_line : 새로운 프로세스에 실행할 프로그램 명령어
> - pid_t는 tid_t와 동일한 int 자료형

> 4.1. exec() 구현
> ```c
> // syscall.c
> 
> int exec(const char *cmd_line) {
>    check_address(cmd_line);
>
>    off_t size = strlen(cmd_line) + 1;
>    char *cmd_copy = palloc_get_page(PAL_ZERO);
>
>    if (cmd_copy == NULL)
>        return -1;
>
>    memcpy(cmd_copy, cmd_line, size);
>
>    if (process_exec(cmd_copy) == -1)
>        return -1;
>
>    return 0;  // process_exec 성공시 리턴 값 없음 (do_iret)
> }
> ```

> 4.2. process_exec() 함수 수정
> ```c
> // process.c
>
> int process_exec (void *f_name) {
>    char *file_name = f_name;
>    bool success;
>
>    /* 스레드 구조에서는 intr_frame을 사용할 수 없습니다.
>     * 현재 쓰레드가 재스케줄 되면 실행 정보를 멤버에게 저장하기 때문입니다. */
>    struct intr_frame if_;
>    if_.ds = if_.es = if_.ss = SEL_UDSEG;
>    if_.cs = SEL_UCSEG;
>    if_.eflags = FLAG_IF | FLAG_MBS;
>
>    /* We first kill the current context */
>    process_cleanup();
>
>    /** #Project 2: Command Line Parsing - 문자열 분리 */
>    char *ptr, *arg;
>    int argc = 0;
>    char *argv[64];
>
>    for (arg = strtok_r(file_name, " ", &ptr); arg != NULL; arg = strtok_r(NULL, " ", &ptr))
>        argv[argc++] = arg;
>
>    /* And then load the binary */
>    success = load(file_name, &if_);
>
>    /* If load failed, quit. */
>    if (!success)
>        return -1;
>
>    argument_stack(argv, argc, &if_);
>
>    palloc_free_page(file_name);
>
>    /** #Project 2: Command Line Parsing - 디버깅용 툴 */
>    // hex_dump(if_.rsp, if_.rsp, USER_STACK - if_.rsp, true);
>
>    /* Start switched process. */
>    do_iret(&if_);
>    NOT_REACHED();
> }
> ```

## 5. wait 구현 및 관련 함수 구현
> - 현재 process_wait()는 `-1`을 리턴
> - init Process는 유저 프로세스가 종료될 때까지 대기하지 않고 핀토스 종료
> - process_wait() 기능을 구현
> - 자식프로세스가 모두 종료될 때까지 대기(sleep state)
> - 자식 프로세스가 올바르게 종료가 됐는지 확인
> - wait() 시스템 콜 구현
> - process_wait() 함수와 동일한 기능을 수행

> 5.1. wait() 구현
> ```c
> // syscall.c
>
> int wait(pid_t tid) {
>    return process_wait(tid);
> }
> ```

> 5.2. process_wait() 함수 수정
> ```c
> //process.c
>
> int process_wait (tid_t child_tid UNUSED) {
>   /* XXX: Hint) The pintos exit if process_wait (initd), we recommend you
>  	 * XXX:       to add infinite loop here before
>  	 * XXX:       implementing the process_wait. */
>
>   struct thread *child = get_child_process(child_tid);
>   if (child == NULL)
>       return -1;
>
>   sema_down(&child->wait_sema);  // 자식 프로세스가 종료될 때 까지 대기.
>
>   int exit_status = child->exit_status;
>   list_remove(&child->child_elem);
>
>   sema_up(&child->exit_sema);  // 자식 프로세스가 죽을 수 있도록 signal
>
>   return exit_status;
> }

# 테스트 결과

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/891214dc-ced7-4ac6-b97c-cbed45c0bae2)

