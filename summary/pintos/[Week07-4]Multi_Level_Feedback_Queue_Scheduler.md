# 과제 설명

## MLFQS

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/7237216d-5206-492c-8a0b-1a6aa96daef2)

이전까지 구현한 priority scheduler 는 그 실행을 오직 priority 에만 맡기기 때문에 priority 가 낮은 스레드들은 CPU 를 점유하기가 매우 어렵고 이로 인하여 평균반응시간(Average response time) 은 급격히 커질 가능성이 있다. 물론 priority donation 을 통하여 priority 가 높은 스레드들이 실행되는 데 필요한 스레드는 priority 의 보정을 받지만, 이마저도 받지 못하는 스레드가 존재할 가능성도 매우 크다. Advanced Scheduler 는 이러한 문제를 해결하고 average response time 을 줄이기 위해 multi-level feedback queue 스케줄링 방식을 도입한다. 이 스케줄링 방식의 특징은 크게 두 가지가 있다. 이 프로젝트에서는 2번을 이용해서 구현한다.

> 1. Priority 에 따라 여러 개의 Ready Queue 가 존재한다. (Multi-level)
> 2. Priority 는 실시간으로 조절한다. (Feedback)

### KEYWORD

구현을 위해 새로 추가되는 자료구조들이 있다.

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/aab968bc-9d3c-4192-9ee9-0cf86d184951)

1️⃣ NICENESS
  - 스레드의 젠틀함을 나타내 준다.
  - 수치가 높으면 Priority가 줄어들어 CPU를 더 적게 점유하게 된다.
2️⃣ PRIORITY
  - 기존의 고정되거나 Donation된 태생적인 Priority가 아닌 CPU 사용량과 Niceness에 의해 결정되게 된다.
3️⃣ RECENT_CPU
  - 말 그대로 최근 CPU의 점유 TICK을 나타낸다.
  - 이 수치가 높으면 역시 Priority가 줄어들게 된다.
4️⃣ LOAD AVG
  - 시스템의 현재 부하 상태를 나타내는 지표이다.
PRIORITY, RECENT_CPU, LOAD_AVG를 구하는 식은 다음과 같다.

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/6eb7fb86-72d1-457d-8f68-a290ad9a2a77)

PRIORITY는 제시된 가중치로 결정되게 되고, RECENT CPU의 경우 CPU 점유시간의 값을 지수 가중 평균을 사용하여 계산하게 된다. LOAD AVERAGE는 1분 동안 수행 가능한 스레드의 평균 개수를 추정하는 계산식이다.

NICENESS가 없는데 이는 Test Program에서 임의로 지정해 준다.

> #### 💡 이 때 주의해야 할 점이 있다. 바로 실수형이라는 것!
> **그러나 핀토스에서는 실수를 지원하지 않음!!!** 이를 위해 **고정소수점** 표현을 사용하게 됨..!!!

고정소수점

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/4c10080a-4958-4d83-ac2f-5b672928262c)

고정소수점 표현이란 32비트짜리 정수형을 갖고 14번째 자리를 기준으로 제일 앞 1자리는 부호, 왼쪽 17자리는 정수, 오른쪽 14자리는 소수로 사용하는 기법이다.

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/515b3f2d-e8c0-47e8-a5c2-1014ff3e21da)

그래서 F라는 2의14승짜리 숫자를 만들어 실수와 정수를 전환하게 되고 이에 따라 특수한 매크로를 만들어 연산을 수행해야 한다. 예를 들면 덧셈을 할때는 정수에 F를 곱해 실수로 만들어 줄 수 있다. 실수끼리 곱셈을 할 때는 둘 다 F가 곱해진 상태이므로 중복된 F를 삭제하기 위해 F로 나누어 준다.

## 구현 목표
> Multi-Level Feedback Queue 스케쥴러( BSD 스케줄러와 유사) 구현

### 수정 함수
`void thread_set_priority(int new_priority)`: 현재 수행중인 스레드의 우선순위를 변경
`static void timer_interrupt(struct intr_frame *args UNUSED)`: timer interrupt 핸들러

### 구현해야할 함수

- `int thread_get_nice (void)`
- `void thread_set_nice (int)`
- `int thread_get_recent_cpu (void)`
- `int thread_get_load_avg (void)`

### 추가 함수

`void mlfqs_priority (struct thread *t)`: 인자로 주어진 스레드의 priority를 업데이트
`void mlfqs_recent_cpu (struct thread *t)`: 인자로 주어진 스레드의 recent_cpu를 업데이트
`void mlfqs_load_avg (void)`: 시스템의 load_avg를 업데이트
`void mlfqs_increment (void)`: 현재 수행중인 스레드의 recent_cpu를 1증가 시킴
`void mlfqs_recalc (void)`: 모든 스레드의 priority, recent_cpu를 업데이트

## 구현

### 스케줄러를 위한 자료구조 추가

```c
// thread.h

/** project1-Advanced Scheduler */
#define NICE_DEFAULT 0
#define RECENT_CPU_DEFAULT 0
#define LOAD_AVG_DEFAULT 0

/** project1-Advanced Scheduler */
int niceness;
int recent_cpu;
```

### 스케줄러를 위해 추가로 구현할 함수 선언

```c
// thread.h

/** project1-Advanced Scheduler */
void mlfqs_priority(struct thread *t);
void mlfqs_recent_cpu(struct thread *t);
void mlfqs_load_avg(void);
void mlfqs_increment(void);
void mlfqs_recalc_recent_cpu(void);
void mlfqs_recalc_priority(void);
```

### all_list, all_elem 자료 구조 추가

```c
// thread.h

int niceness;
int recent_cpu;
struct list_elem all_elem; /** project1-Advanced Scheduler */
```
```c
// thread.c

static struct list ready_list;

/** project1-Advanced Scheduler */
static struct list all_list;

/* Idle thread. */
static struct thread *idle_thread;
```


### thread_init() 함수 수정

> all list 초기화

```c
// thread.c

void thread_init (void) {
	ASSERT (intr_get_level () == INTR_OFF);

	/* Reload the temporal gdt for the kernel
	 * This gdt does not include the user context.
	 * The kernel will rebuild the gdt with user context, in gdt_init (). */
	struct desc_ptr gdt_ds = {
		.size = sizeof (gdt) - 1,
		.address = (uint64_t) gdt
	};
	lgdt (&gdt_ds);

	/* Init the globla thread context */
	lock_init (&tid_lock);
	list_init (&ready_list);
	list_init (&destruction_req);
	list_init (&sleep_list); /** project1-Alarm Clock */
	list_init(&all_list); /** project1-Advanced Scheduler */

	/* Set up a thread structure for the running thread. */
	initial_thread = running_thread ();
	init_thread (initial_thread, "main", PRI_DEFAULT);

	/** project1-Advanced Scheduler */
	if (thread_mlfqs)
		list_push_back(&all_list, &(initial_thread->all_elem));

	initial_thread->status = THREAD_RUNNING;
	initial_thread->tid = allocate_tid ();
}
```

### init_thread() 함수 수정

```c
// thread.c

static void init_thread (struct thread *t, const char *name, int priority) {
	ASSERT (t != NULL);
	ASSERT (PRI_MIN <= priority && priority <= PRI_MAX);
	ASSERT (name != NULL);

	memset (t, 0, sizeof *t);
	t->status = THREAD_BLOCKED;
	strlcpy (t->name, name, sizeof t->name);
	t->tf.rsp = (uint64_t) t + PGSIZE - sizeof (void *);

	/** project1-Advanced Scheduler */
    if (thread_mlfqs) {
        mlfqs_priority(t);
        list_push_back(&all_list, &t->all_elem);
    } else {
        t->priority = priority;
    }

    t->wait_lock = NULL;
    list_init(&t->donations);

    t->magic = THREAD_MAGIC;

    /** #Advanced Scheduler */
    t->original_priority = t->priority;
    t->niceness = NICE_DEFAULT;
    t->recent_cpu = RECENT_CPU_DEFAULT;
}
```

### fixed point 연산 함수 구현

```c
// fixed_point.h
/** project1-Advanced Scheduler */
#define F (1 << 14)
#define INT_MAX ((1 << 31) - 1)
#define INT_MIN (-(1 << 31))

/*
 * 고정 소수점을 위한 산술 연산
 * n: int ; x, y: 고정 소수점 숫자 ; F: 17.14로 표현한 1
 */

int int_to_fp(int n);         /* integer를 fixed point로 전환 */
int fp_to_int_round(int x);   /* FP를 int로 전환(반올림) */
int fp_to_int(int x);         /* FP를 int로 전환(버림) */
int add_fp(int x, int y);     /* FP의 덧셈 */
int add_mixed(int x, int n);  /* FP와 int의 덧셈 */
int sub_fp(int x, int y);     /* FP의 뺄셈(x-y) */
int sub_mixed(int x, int n);  /* FP와 int의 뺄셈(x-n) */
int mult_fp(int x, int y);    /* FP의 곱셈 */
int mult_mixed(int x, int y); /* FP와 int의 곱셈 */
int div_fp(int x, int y);     /* FP의 나눗셈(x/y) */
int div_mixed(int x, int n);  /* FP와 int 나눗셈(x/n) */


/* 함수 본체 */

/* integer를 fixed point로 전환 */
int int_to_fp(int n) {
    return n * F;
}

/* FP를 int로 전환(반올림) */
int fp_to_int_round(int x) {
    if (x >= 0)
        return (x + F / 2) / F;
    else
        return (x - F / 2) / F;
}

/* FP를 int로 전환(버림) */
int fp_to_int(int x) {
    return x / F;
}

/* FP의 덧셈 */
int add_fp(int x, int y) {
    return x + y;
}

/* FP와 int의 덧셈 */
int add_mixed(int x, int n) {
    return x + n * F;
}

/* FP의 뺄셈(x-y) */
int sub_fp(int x, int y) {
    return x - y;
}

/* FP와 int의 뺄셈(x-n) */
int sub_mixed(int x, int n) {
    return x - n * F;
}

/* FP의 곱셈 */
int mult_fp(int x, int y) {
    return ((int64_t)x) * y / F;
}

/* FP와 int의 곱셈 */
int mult_mixed(int x, int n) {
    return x * n;
}

/* FP의 나눗셈(x/y) */
int div_fp(int x, int y) {
    return ((int64_t)x) * F / y;
}

/* FP와 int 나눗셈(x/n) */
int div_mixed(int x, int n) {
    return x / n;
}
```

### fixed_point.h 인클루드, 스케줄러 관련 상수 정의, 변수 선언 및 초기화

```
// thread.c

bool thread_mlfqs;

/** project1-Advanced Scheduler */
int load_avg;
struct semaphore idle_started;
sema_init (&idle_started, 0);
thread_create ("idle", PRI_MIN, idle, &idle_started);

/** project1-Advanced Scheduler */
load_avg = LOAD_AVG_DEFAULT;

/* Start preemptive thread scheduling. */
intr_enable ();
```

### mlfqs_priority() 함수 구현

> recent_cpu와 nice값을 이용하여 priority를 계산

```c
// thread.c

/** project1-Advanced Scheduler */
void mlfqs_priority (struct thread *t) {
    if (t == idle_thread)
        return;

    t->priority = fp_to_int(add_mixed(div_mixed(t->recent_cpu, -4), PRI_MAX - t->niceness * 2));
}
```

### mlfqs_recent_cpu() 함수 구현

> recent_cpu 값 계산

```c
// thread.c

/** project1-Advanced Scheduler */
void mlfqs_recent_cpu (struct thread *t) {
    if (t == idle_thread)
        return;

    t->recent_cpu = add_mixed(mult_fp(div_fp(mult_mixed(load_avg, 2), add_mixed(mult_mixed(load_avg, 2), 1)), t->recent_cpu), t->niceness);
}
```

### mlfqs_load_avg() 함수 구현

> load_avg 값 계산

```c
// thread.c

/** project1-Advanced Scheduler */
void mlfqs_load_avg (void) {
    int ready_threads;

    ready_threads = list_size(&ready_list);

    if (thread_current() != idle_thread)
        ready_threads++;

    load_avg = add_fp(mult_fp(div_fp(int_to_fp(59), int_to_fp(60)), load_avg), mult_mixed(div_fp(int_to_fp(1), int_to_fp(60)), ready_threads));
}
```

### mlfqs_increment() 함수 구현

> recent_cpu 값 1증가
```c
// thread.c

/** project1-Advanced Scheduler */
void mlfqs_increment (void) {
    if (thread_current() == idle_thread)
        return;

    thread_current()->recent_cpu = add_mixed(thread_current()->recent_cpu, 1);
}
```

### mlfqs_recal() 함수 구현

> 모든 thread의 recent_cpu와 priority값 재계산

```c
// thread.c

/** project1-Advanced Scheduler */
void mlfqs_recalc_recent_cpu (void) {
    struct list_elem *e = list_begin(&all_list);
    struct thread *t = NULL;

    while (e != list_end(&all_list)) {
        t = list_entry(e, struct thread, all_elem);
        mlfqs_recent_cpu(t);

        e = list_next(e);
    }
}

/** project1-Advanced Scheduler */
void mlfqs_recalc_priority (void) {
    struct list_elem *e = list_begin(&all_list);
    struct thread *t = NULL;

    while (e != list_end(&all_list)) {
        t = list_entry(e, struct thread, all_elem);
        mlfqs_priority(t);

        e = list_next(e);
    }
}
```

### thread_set_priority() 함수 수정

> mlfqs 스케줄러를 활성 하면 thread_mlfqs 변수는 ture로 설정된다.
> 
> Advanced scheduler에서는 우선순위를 임의로 변경할 수 없다.

```c
// thread.c

void thread_set_priority (int new_priority) {

	/** project1-Advanced Scheduler */
	if (thread_mlfqs)
        return;

	/** project1-Priority Inversion Problem */
    thread_current()->original_priority = new_priority;
```

### thread_set_nice() 함수 구현

> 현재 thread의 nice값을 nice로 설정

```c
// thread.c

void thread_set_nice (int nice UNUSED) {
	/* TODO: Your implementation goes here */
	/** project1-Advanced Scheduler */
    struct thread *t = thread_current();

    enum intr_level old_level = intr_disable();
    t->niceness = nice;
    mlfqs_priority(t);
    test_max_priority();
    intr_set_level(old_level);
}
```

### thread_get_nice() 함수 구현

> 현재 thread의 nice 값 반환

```c
// thread.c

int thread_get_nice (void) {
	/* TODO: Your implementation goes here */
	/** project1-Advanced Scheduler */
    struct thread *t = thread_current();

    enum intr_level old_level = intr_disable();
    int nice = t->niceness;
    intr_set_level(old_level);

    return nice;
}
```

### thread_get_load_avg() 함수 구현

> load_avg에 100을 곱해서 반환

```c
// thread.c

int thread_get_load_avg (void) {
	/* TODO: Your implementation goes here */
	/** project1-Advanced Scheduler */
    enum intr_level old_level = intr_disable();
    int load_avg_val = fp_to_int_round(mult_mixed(load_avg, 100));  
    intr_set_level(old_level);

    return load_avg_val;
}
```

### thread_get_recent_cpu() 함수 구현

> recent_cpu에 100을 곱해서 반환

```c
// thread.c

int thread_get_recent_cpu (void) {
	/* TODO: Your implementation goes here *
	/** project1-Advanced Scheduler */
    struct thread *t = thread_current();

    enum intr_level old_level = intr_disable();
    int recent_cpu = fp_to_int_round(mult_mixed(t->recent_cpu, 100)); 
    intr_set_level(old_level);

    return recent_cpu;
}
```

### timer_interrupt() 함수 수정

> 1초 마다 load_avg, 모든 스레드의 recent_cpu, priority 재계산
> 
> 4tick 마다 현재 스레드의 priority 재계산

```c
// timer.c

static void timer_interrupt (struct intr_frame *args UNUSED) {
	ticks++;
	thread_tick ();

	if (thread_mlfqs) {
		mlfqs_increment();

		if (!(ticks % 4))	{
			mlfqs_recalc_priority();

			if (!(ticks % TIMER_FREQ))	{
				mlfqs_load_avg();
				mlfqs_recalc_recent_cpu();
			}
		}
  }

	/** project1-Alarm Clock */
	if (get_next_tick_to_awake() <= ticks) {
	  thread_awake(ticks);
	}
}
```

### lock_acquire() 함수 수정

> mlfqs 스케줄러 활성화시 priority donation 관련 코드 비활성화

```c
// synch.c

void lock_acquire (struct lock *lock) {
  ASSERT (lock != NULL);
  ASSERT (!intr_context ());
  ASSERT (!lock_held_by_current_thread (lock));
  /* project1-Priority Inversion Problem */
  struct thread *t = thread_current();
  if (lock->holder != NULL) {
    t->wait_lock = lock;
    list_push_back(&lock->holder->donations, &t->donation_elem);
    donate_priority();
  }
  sema_down (&lock->semaphore);
  /* project1-Priority Inversion Problem /
  t->wait_lock = NULL;
  lock->holder = t;
}
```

### lock_release() 함수 수정

>  mlfqs 스케줄러 활성화시 priority donation 관련 코드 비활성화 

```c
// synch.c

void lock_release (struct lock *lock) {
	ASSERT (lock != NULL);
	ASSERT (lock_held_by_current_thread (lock));

	lock->holder = NULL;

   /** project1-Priority Inversion Problem */
   remove_with_lock(lock);
   refresh_priority();

	sema_up (&lock->semaphore);
}
```

### thread_exit() 함수 수정

> 스레드 종료 시 all_list에서 제거

```c
// thread.c

void thread_exit (void) {
	ASSERT (!intr_context ());

#ifdef USERPROG
	process_exit ();
#endif
	/** project1-Advanced Scheduler */
	if (thread_mlfqs)
        list_remove(&thread_current()->all_elem);
	/* Just set our status to dying and schedule another process.
	   We will be destroyed during the call to schedule_tail(). */
	intr_disable ();
	do_schedule (THREAD_DYING);
	NOT_REACHED ();
}
```

## 테스트 결과

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/6c30cb30-ce23-47ef-bea6-63d3332188fd)

