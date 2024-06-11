# ⏰ Alarm Clock
> 운영체제에는 실행중인 스레드를 잠시 재웠다가 일정 시간이 지나면 다시 깨우도록 하는 기능이 있는데, 이 기능을 Alarm Clock 이라고 한다.

현재 핀토스에 구현되어 있는 Alarm Clock 기능은 busy-waiting 방식으로 구현되어 있다. 이는 매우 비효율적으로 많은 CPU 시간을 낭비하게 한다. 따라서 이 방식을 개선하는 것이 과제의 목표이다.

🏃 busy-waiting
busy-waiting 방식에서 sleep 명령을 받은 스레드의 진행 흐름은 아래와 같이 진행된다.
잠이듬 -> 깨어남 -> 시간확인 -> 다시잠 -> 깨어남 -> 시간확인 -> ... -> 깨어남 -> 시간확인(일어날시간) -> 깨어남

> Ex) Thread A가 5tick 뒤에 특정 작업을 실행하길 원한다고 하자. 매 Tick마다 Thread A가 실행되어 5tick이 되었는지를 확인한다.

스레드의 상태가 running state 와 ready state 를 반복함을 볼 수 있다. running state 에서 sleep 명령을 받은 스레드는 ready state 가 되어 ready queue 에 추가되고 ready queue 에 있는 스레드들은 자신의 차례가 되면 일어날 시간이 되었는지에 상관없이 깨워져 running state 가 된다. 이렇게 running state 가 된 스레드는 자신이 일어날 시간이 되었는지 확인하고 아직 일어날 시간이 안 됐다면 다시 ready state 로 전환한다.

#### => 이러한 방식은 CPU 자원을 낭비하고, 다른 스레드가 실행되는 기회를 줄여 성능 저하를 야기할 수 있다.

* busy-waiting 에서 thread 의 상태

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/e2445978-7d55-435f-8fcd-35b89d8f8b30)


😴 sleep-awake
변경 방안
현재 => busy-waiting
스레드를 깨운 후 일어날 시간인지 확인한다. 그리고 아직 일어날 시간이 아니라면 다시 ready state로 보낸다.

변경안 => sleep-awake
이러한 비효율을 해결하려면 잠이 든 스레드를 ready state 가 아닌 block state 로 두어서 깨어날 시간이 되기 전까지는 스케줄링에 포함되지 않도록 하고, 깨어날 시간이 되었을 때 ready state 로 바꾸어 주면 된다.

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/fbaf87f7-0fc7-4fd1-955c-29af03cf5e72)

구현 목표
* loop 기반 wait() -> sleep-awake 로 변경
* timer_sleep() 호출시 thread를 ready_list에서 제거, sleep queue에 추가
* wake up 수행
  - timer interrupt가 발생시 tick 체크
  - 시간이 다 된 thread는 sleep queue에서 삭제하고, ready_list에 추가

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/62094539-4c01-4486-b995-9741b877e51d)

### 수정 및 추가 함수
`void timer_sleep (int64_t ticks)`: 인자로 주어진 ticks 동안 스레드를 block

`void thread_sleep(int64_t ticks)`: Thread를 blocked 상태로 만들고 sleep queue에 삽입하여 대기

`void thread_awake(int64_t ticks)`: Sleep queue에서 깨워야 할 thread를 찾아서 wake

`void update_next_tick_to_awake(int64_t ticks)`: Thread들이 가진 tick 값에서 최소 값을 저장

`int64_t get_next_tick_to_awake(void)`: 최소 tick값을 반환

### 수정 및 추가 자료 구조
struct thread
static struct list sleep_list;
int64_t next_tick_to_awake = INT64_MAX;

## 구현
### 스레드 디스크립터 필드 추가

> ▶️ Thread 자신이 깨어나야 할 tick을 저장하는 wakeup_tick 변수를 추가

```
// thread.h
struct thread {
/* Owned by thread.c. */
tid_t tid;                          /* Thread identifier. */
enum thread_status status;          /* Thread state. */
char name[16];                      /* Name (for debugging purposes). */
int priority;                       /* Priority. */
int64_t wakeup_tick;				/** project1-Alarm Clock */
    
// ...

  ```
    
### 전역변수 추가

> ▶️ Sleep queue 자료구조 추가
> 
> ▶️ next_tick_to_awake 전역 변수 추가
> 
> → sleep_list에서 대기중인 스레드들의 wakeup_tick 값 중 최소값을 저장

```
// thread.c

// ...

/* Random value for basic thread
   Do not modify this value. */
#define THREAD_BASIC 0xd42df210

/** project1-Alarm Clock */
static struct list sleep_list;
static int64_t next_tick_to_awake;

/* List of processes in THREAD_READY state, that is, processes
   that are ready to run but not actually running. */
static struct list ready_list;

// ...

```

### 구현할 함수 선언

```
// thread.h

// ...

/** project1-Alarm Clock */
void thread_sleep (int64_t ticks);
void thread_awake (int64_t ticks);
void update_next_tick_to_awake (int64_t ticks);
int64_t get_next_tick_to_awake (void);

void thread_init (void);
void thread_start (void);

void thread_tick (void);
void thread_print_stats (void);

// ...

```
### thread_init() 함수 수정

> ▶️ main() 함수에서 호출되는 쓰레드 관련 초기화 함수
> 
> ▶️ Sleep queue 자료구조 초기화 코드 추가

```
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

	/* Set up a thread structure for the running thread. */
	initial_thread = running_thread ();
	init_thread (initial_thread, "main", PRI_DEFAULT);
	initial_thread->status = THREAD_RUNNING;
	initial_thread->tid = allocate_tid ();
}
```

### timer_sleep() 함수 수정

> ▶️ 기존의 busy waiting을 유발하는 코드 삭제
> 
> ▶️ Sleep queue를 이용하도록 함수 수정
> 
> → 밑에서 구현하는 thread_sleep() 함수 사용

```
// timer.c

void timer_sleep (int64_t ticks) {
	int64_t start = timer_ticks ();

	ASSERT (intr_get_level () == INTR_ON);
	/** project1-Alarm Clock 
	while (timer_elapsed (start) < ticks)
		thread_yield (); */
	thread_sleep (start + ticks);
}
```

### thread_sleep() 함수 구현

> ▶️ thread를 sleep queue에 삽입하고 blocked 상태로 만들어 대기
> 
> ▶️ 해당 과정중에는 인터럽트를 받아들이지 않는다.
> 
> ▶️ timer_sleep() 함수에 의해 호출

```
// thread.c

/** project1-Alarm Clock */
void thread_sleep (int64_t ticks) 
{
    struct thread *this;
    this = thread_current();

    if (this == idle_thread) // idle -> stop
	{  
        ASSERT(0);
    } else 
	{
        enum intr_level old_level;
        old_level = intr_disable();  // pause interrupt

        update_next_tick_to_awake(this->wakeup_tick = ticks);  // update awake ticks

        list_push_back(&sleep_list, &this->elem);  // push to sleep_list

        thread_block();  // block this thread

        intr_set_level(old_level);  // continue interrupt
    }
}
```

### timer_interrupt() 함수 수정

> ▶️ 매 tick마다 timer 인터럽트 시 호출되는 함수
> 
> ▶️ sleep queue에서 깨어날 thread가 있는지 확인
>
>> → sleep queue에서 가장 빨리 깨어날 쓰레드의 tick값 확인
>> 
>> → 있다면 sleep queue를 순회하며 쓰레드 깨움 ( 밑에서 구현하는 thread_awake() 함수 사용 )

```
// timer.c

static void timer_interrupt (struct intr_frame *args UNUSED) {
	ticks++;
	thread_tick ();

	/** project1-Alarm Clock */
	if (get_next_tick_to_awake() <= ticks)
	{
	thread_awake(ticks);
	}
}
```

### thread_awake() 함수 구현

> ▶️ wakeup_tick값이 인자로 받은 ticks보다 크거나 같은 스레드를 깨움
> 
> ▶️ 현재 대기중인 스레드들의 wakeup_tick변수 중 가장작은 값을 next_tick_to_awake 전역 변수에 저장

```
// timer.c

/** project1-Alarm Clock */
void thread_awake (int64_t wakeup_tick) 
{
    next_tick_to_awake = INT64_MAX;

    struct list_elem *sleeping;
    sleeping = list_begin(&sleep_list);  // take sleeping thread

    while (sleeping != list_end(&sleep_list)) {  // for all sleeping threads
        struct thread *th = list_entry(sleeping, struct thread, elem);

        if (wakeup_tick >= th->wakeup_tick) 
		{
            sleeping = list_remove(&th->elem);  // delete thread
            thread_unblock(th);                 // unblock thread
        } 
		else 
		{
            sleeping = list_next(sleeping);              // move to next sleeping thread
            update_next_tick_to_awake(th->wakeup_tick);  // update wakeup_tick
        }
    }
}
```

### upadate_next_tick_to_awake() 함수 추가

> ▶️ next_tick_to_awake 변수를 업데이트

```
// timer.c

/** project1-Alarm Clock */
void update_next_tick_to_awake (int64_t ticks) 
{
	// find smallest tick
    next_tick_to_awake = (next_tick_to_awake > ticks) ? ticks : next_tick_to_awake;
}
get_next_tick_to_awake() 함수 추가
threads/thread.c
/** project1-Alarm Clock */
int64_t
get_next_tick_to_awake(void)
{
	return next_tick_to_awake;
}
```

## 테스트 결과

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/942ed4e0-d4d6-4836-a7dc-ec2d4365f606)

idle tick이 550으로 증가
