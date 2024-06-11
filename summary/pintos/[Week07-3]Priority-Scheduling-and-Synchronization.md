# 과제 설명

> 여러 스레드가 lock, semaphore, condition variable 을 얻기 위해 기다릴 경우 우선순위가 가장 높은 thread가 CPU를 점유 하도록 구현

#### 현재 핀토스

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/8137cb86-b4c1-48db-9577-13d8848449d9)

현재 핀토스는 semaphore를 대기 하고 있는 스레드들의 list인 waiters가 FIFO로 구현되어있다.

> #### 💡 Semaphore를 요청 할때 대기 리스트를 우선순위로 정렬
> sema_down()에서 waiters list를 우선순위로 정렬 하도록 수정한다.

## 구현 목표

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/76242178-784a-4efb-9e96-f25bda8aed02)

1️⃣ sema_down() 함수 수정
  - Semaphore를 얻고 waiters 리스트 삽입 시, 우선순위대로 삽입되도록 수정
2️⃣ sema_up() 함수 수정
  - waiter list에 있는 쓰레드의 우선순위가 변경 되었을 경우를 고려하여 waiter list를 정렬 (list_sort)
  - 세마포어 해제 후 priority preemption 기능 추가

### 수정 및 추가 함수

`void sema_down (struct semaphore *sema)`
`void sema_up (struct semaphore *sema)`
`void cond_wait (struct condition *cond, struct lock *lock)`
`void cond_signal (struct condition *cond, struct lock *lock UNUSED)`
`bool cmp_sem_priority(const struct list_elem *a, const struct list_elem *b, void *aux UNUSED)`: 첫번째 인자로 주어진 세마포어를 위해 대기 중인 가장 높은 우선순위의 스레드와 두번째 인자로 주어진 세마포어를 위해 대기 중인 가장 높은 우선순위의 스레드와 비교

## 구현

### sema_down() 함수 수정

> ▶ Semaphore를 얻고 waiters 리스트 삽입 시, 우선순위대로 삽입되도록 수정

```
// synch.c

void sema_down (struct semaphore *sema) {
	enum intr_level old_level;

	ASSERT (sema != NULL);
	ASSERT (!intr_context ());

	old_level = intr_disable ();
	while (sema->value == 0) {
		/** project1-Synchronizatio */
		// list_push_back (&sema->waiters, &thread_current ()->elem);
		list_insert_ordered(&sema->waiters, &thread_current()->elem, cmp_priority, NULL);
		thread_block ();
	}
	sema->value--;
	intr_set_level (old_level);
}
```

### sema_up() 함수 수정

> ▶waiter list에 있는 쓰레드의 우선순위가 변경 되었을 경우를 고려하여 waiter list를 정렬 (list_sort)
> 
> 세마포어 해제 후 priority preemption 기능 추가
```
// synch.c

void sema_up (struct semaphore *sema) {
	enum intr_level old_level;

	ASSERT (sema != NULL);

	old_level = intr_disable ();
	if (!list_empty (&sema->waiters)){
		/** project1-Synchronization */
        list_sort(&sema->waiters, cmp_priority, NULL);
		thread_unblock (list_entry (list_pop_front (&sema->waiters),
					struct thread, elem));
	}
	sema->value++;

	/** project1-Synchronization */
	test_max_priority();

	intr_set_level (old_level);
}
```

### 구현할 함수 선언

```
// synch.h

/** project1-Synchronization */
bool cmp_sem_priority(const struct list_elem *a, const struct list_elem *b, void *aux);

```

### cmp_sem_priority() 함수 추가

> ▶ semaphore_elem으로부터 각 semaphore_elem의 쓰레드 디스크립터를 획득.
> 
> ▶ 첫 번째 인자의 우선순위가 두 번째 인자의 우선순위보다 높으면 1을 반환 낮으면 0을 반환

```
// synch.c

/** project1-Synchronization */
bool cmp_sem_priority (const struct list_elem *a, const struct list_elem *b, void *aux UNUSED) {
    struct semaphore_elem *sema_a = list_entry(a, struct semaphore_elem, elem);
    struct semaphore_elem *sema_b = list_entry(b, struct semaphore_elem, elem);

    if (sema_a == NULL || sema_b == NULL)
        return false;

    struct list *list_a = &(sema_a->semaphore.waiters);
    struct list *list_b = &(sema_b->semaphore.waiters);

    if (list_a == NULL || list_b == NULL)
        return false;

    struct thread *thread_a = list_entry(list_begin(list_a), struct thread, elem);
    struct thread *thread_b = list_entry(list_begin(list_b), struct thread, elem);

    if (thread_a == NULL || thread_b == NULL)
        return false;

    return thread_a->priority > thread_b->priority;
}
```

### cond_wait() 함수 수정

> ▶ condition variable의 waiters list에 우선순위 순서로 삽입되도록 수정

```
// synch.c

void cond_wait (struct condition *cond, struct lock *lock) {
	struct semaphore_elem waiter;

	ASSERT (cond != NULL);
	ASSERT (lock != NULL);
	ASSERT (!intr_context ());
	ASSERT (lock_held_by_current_thread (lock));

	sema_init (&waiter.semaphore, 0);
	/** project1-Synchronization */
	//list_push_back (&cond->waiters, &waiter.elem);
	list_insert_ordered(&cond->waiters, &waiter.elem, cmp_sem_priority, NULL);
	lock_release (lock);
	sema_down (&waiter.semaphore);
	lock_acquire (lock);
}
```
### cond_signal() 함수 수정

> ▶ condition variable의 waiters list를 우선순위로 재 정렬
> 
> ▶ 대기 중에 우선순위가 변경되었을 가능성이 있음

```
// synch.c

void cond_signal (struct condition *cond, struct lock *lock UNUSED) {
	ASSERT (cond != NULL);
	ASSERT (lock != NULL);
	ASSERT (!intr_context ());
	ASSERT (lock_held_by_current_thread (lock));

	if (!list_empty (&cond->waiters))
		/** project1-Synchronization */	
	    list_sort(&cond->waiters, cmp_sem_priority, NULL);
		sema_up (&list_entry (list_pop_front (&cond->waiters),
					struct semaphore_elem, elem)->semaphore);
}
```
## 테스트 결과

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/201d0d94-a804-46af-ae50-813303429967)

### 통과되어야하는 테스트

![image](https://github.com/Arklimits/swjungle-archive/assets/74225157/0c13b6f9-0385-4b91-8af6-7255483f0137)

- priority-sema
- priority-condvar
