# 과제 설명

> 현재 핀토스의 스케줄러는 라운드 로빈으로 구현되어 있다. 이를 우선순위를 고려하여 스케줄링 하도록 수정한다.

* 핀토스의 thread lifecycle

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/99a3d58e-635a-4287-9cf6-84a6dcf45467)

CPU는 여러 Thread를 번갈아가며 수행한다. 모든 Thread는 Ready List에 들어가 자신의 순서가 오길 기다리는데, 기존 pintOS는 단순히 새로 들어온 Thread를 줄의 맨 뒤에 세우고, 먼저 온 순서대로 처리하는 FCFS(First Come First Served) 방식으로 줄을 세운다.

> 💡 우선순위가 높은 스레드가 먼저 CPU를 점유할 수 있도록 Priority Scheduling을 구현한다.

## 구현 목표

1️⃣ Ready list에 새로 추가된 thread의 우선순위가 현재 CPU를 점유중인 thread의 우선순위보다 높으면, 기존 thread를 밀어내고 CPU를 점유하도록 한다.
2️⃣ 여러 thread가 lock, semaphore 를 얻기 위해 기다릴 경우 우선순위가 가장 높은 thread가 CPU를 점유한다.

#### 추가 함수
* `void test_max_priority(void)`: 현재 수행중인 스레드와 가장 높은 우선순위의 스레드의 우선순위를 비교하여 스케줄링
* `bool cmp_priority(const struct list_elem *a, const struct list_elem *b, void *aux UNUSED)`:인자로 주어진 스레드들의 우선순위를 비교

#### 수정 함수

* `tid_t thread_create (const char *name, int priority, thread_func *function, void *aux)`: 새로운 스레드를 생성 (스레드를 생성할 때, 우선순위도 지정하도록 바꾼다.)
* `void thread_unblock (struct thread *t)`: block된 스레드를 unblock
* `void thread_yield (void)`: 현재 수행중인 스레드가 사용중인 CPU를 양보
* `void thread_set_priority (int new_priority)`: 현재 수행중인 스레드의 우선순위를 변경

## 구현

### 구현할 함수 선언

```
// thread.h

/** project1-Priority Scheduling */
void test_max_priority(void);
bool cmp_priority (const struct list_elem *a, const struct list_elem *b, void *aux UNUSED);
```

### thread_create() 함수 수정

> ▶️ Thread의 unblock 후, 현재 실행중인 thread와 우선순위를 비교하여, 새로 생성된 thread의 우선순위가 높다면 thread_yield()를 통해 CPU를 양보

```
// thread.c

tid_t thread_create (const char *name, int priority,
		thread_func *function, void *aux) {
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

	/* Call the kernel_thread if it scheduled.
	 * Note) rdi is 1st argument, and rsi is 2nd argument. */
	t->tf.rip = (uintptr_t) kernel_thread;
	t->tf.R.rdi = (uint64_t) function;
	t->tf.R.rsi = (uint64_t) aux;
	t->tf.ds = SEL_KDSEG;
	t->tf.es = SEL_KDSEG;
	t->tf.ss = SEL_KDSEG;
	t->tf.cs = SEL_KCSEG;
	t->tf.eflags = FLAG_IF;

	/* Add to run queue. */
	thread_unblock (t);

	/** project1-Priority Scheduling */
	if(t->priority > thread_current()->priority)
		thread_yield();

	return tid;
}
```

### thread_unblock() 함수 수정

> ▶️ Thread가 unblock 될때 우선순위 순으로 정렬 되어 ready_list에 삽입되도록 수정

```
// thread.c

void thread_unblock (struct thread *t) {
	enum intr_level old_level;

	ASSERT (is_thread (t));

	old_level = intr_disable ();
	ASSERT (t->status == THREAD_BLOCKED);
	/** project1-Priority Scheduling */
	list_insert_ordered(&ready_list, &t->elem, cmp_priority, NULL);
	//list_push_back (&ready_list, &t->elem);
	t->status = THREAD_READY;
	intr_set_level (old_level);
}
```

### thread_yield() 함수 수정

> ▶️ 현재 thread가 CPU를 양보하여 ready_list에 삽입 될 때 우선순위 순서로 정렬되어 삽입 되도록 수정

```
// thread.c

void thread_yield (void) {
	struct thread *curr = thread_current ();
	enum intr_level old_level;

	ASSERT (!intr_context ());

	old_level = intr_disable ();
	if (curr != idle_thread)
		//list_push_back (&ready_list, &curr->elem);
		/** project1-Priority Scheduling */
		list_insert_ordered(&ready_list, &curr->elem, cmp_priority, NULL);
	do_schedule (THREAD_READY);
	intr_set_level (old_level);
}
```

### thread_set_priority() 함수 수정

> ▶️ 현재 쓰레드의 우선 순위와 ready_list에서 가장 높은 우선 순위를 비교하여 스케쥴링 하는 함수 호출

```
//thread.c

void thread_set_priority (int new_priority) {
	thread_current ()->priority = new_priority;
	/** project1-Priority Scheduling */
	test_max_priority();
}
```

### test_max_priority() 함수 추가

> ▶️ ready_list에서 우선 순위가 가장 높은 쓰레드와 현재 쓰레드의 우선 순위를 비교.
> 
> → 현재 쓰레드의 우선수위가 더 작다면 thread_yield()

```
// thread.c

/** project1-Priority Scheduling */
void test_max_priority (void) 
{
    if (list_empty(&ready_list))
        return;

    struct thread *th = list_entry(list_front(&ready_list), struct thread, elem);

    if (thread_get_priority() < th->priority)
        thread_yield();
}
```

### cmp_priority() 함수 추가

> ▶️ 첫 번째 인자의 우선순위가 높으면 1을 반환, 두 번째 인자의 우선순위가 높으면 0을 반환
> 
> ▶️ list_insert_ordered() 함수에서 사용

```
// thread.c

/** project1-Priority Scheduling */
bool cmp_priority (const struct list_elem *a, const struct list_elem *b, void *aux UNUSED) 
{
    struct thread*thread_a = list_entry(a, struct thread, elem);
    struct thread*thread_b = list_entry(b, struct thread, elem);

	if (thread_a == NULL || thread_b == NULL)
		return false;

    return thread_a->priority > thread_b->priority;
}
```
## 테스트 결과

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/3d977d39-8eef-444f-a2e5-9f0bf5cb102a)

### 통과되어야하는 테스트

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/0fcd1e55-2202-4fb1-9d7b-e9b5f997261d)

- alarm-priority
- priority-change
- priority-fifo
- priority-preempt

