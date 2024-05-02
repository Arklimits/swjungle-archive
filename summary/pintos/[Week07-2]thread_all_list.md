# MLFQS를 구현함에 있어 `All List`에 관하여 (feat. dangling pointer)

먼저 사전에 알아야 하는 점이 있다. 바로 Pintos 학습자료가 32bit와 64bit로 나뉘어져 있다는 것이다. 아직까지 일부분만 학습하여 32bit와 64bit Pintos의 차이를 전부 알지는 못하지만 `thread`부분에서 체감할 수 있는 부분이 있었다.

**바로 `all_list`가 없다는 것.**

이 페이지는 내가 학습한 내용이 너무 방대하여 만들었지만, 현재의 나도 정글의 선배 기수들의 자료들을 많이 참고하며 공부하고 있는데, 언젠가 다른 정글러들이 `mlfqs`를 구현하고자 하다가 헤메면서 꽁꽁 숨겨져있는 이 page까지 왔다면 아마 이러한 연유로 찾아오지 않았을까 하는 생각이 든다.

만약 `MLFQS`가 `mlfqs-load-1` 테스트는 성공했는데 `mlfqs-load-60`에서 잘 돌다가 **120초에서 뜬금없이 에러가 난다면 아주 잘 찾아왔다!**

`Project 1`의 메인 학습 목표인 `Alarm Clock`, `Prioirty Scheduling`을 구현할 때에는 전혀 막힘이 없다. 그러나 추가적인 목표인 `Advanced Scheduler`를 구현하고자 할 때, **대기 상태의 쓰레드**들만 들어있는 `ready_list`가 아닌 **활성화 된 모든 쓰레드**가 들어있는 list의 필요성을 느끼게 되는데 그것이 바로 `all_list`이다.

결론부터 말하자면, 결국 문제가 생긴다 함은 64bit로 넘어올 때 삭제 된 이 list를 다시 만들어 쓰게 되면서 무언가를 놓치게 됨으로 인해 발생하게 된다.

***바로 `thread exit` 함수와 `Dangling Pointer`다.***

아마 `all_list`와 `all_list_elem`를 `thread structure`에 삽입하고 사용하는 것은 잘 했으리라고 믿는다.

```c
// thread.c
/** #Advanced Scheduler */
static struct list all_list;
```
```c
// thread.h
typedef struct thread {
    // ...

    /** #Advanced Scheduler */
    int niceness;              /* Niceness. */
    int recent_cpu;            /* 최근 CPU 점유 시간 */
    struct list_elem all_elem; /* 살아있는 모든 Thread 연결 */

    // ...
} thread_t;
```
```c
// thread.c
static void init_thread(struct thread *t, const char *name, int priority) {
    // ...

    if (thread_mlfqs) {
        /** #Advanced Scheduler 자료구조 초기화 */
        mlfqs_priority(t);
        list_push_back(&all_list, &t->all_elem);
    } else {
        /** #Priority Donation 자료구조 초기화 */
        t->priority = priority;
    }

    // ...

    /** #Advanced Scheduler */
    t->original_priority = t->priority;
    t->niceness = NICE_DEFAULT;
    t->recent_cpu = RECENT_CPU_DEFAULT;
}

```

그런데 `thread`는 생성 후에 `loop`를 돌다가 `sleep`상태로 들어간 후 만료된다. 만료될 때 `thread_exit` 함수를 호출하고, `do_schedule` 함수 내에서 `alloc`되어있던 `page`를 통째로 `free`시킨다.

```c
// thread.c
void thread_exit(void) {
    ASSERT(!intr_context());

#ifdef USERPROG
    process_exit();
#endif

    intr_disable();
    do_schedule(THREAD_DYING);
    NOT_REACHED();
}
```
```c
// thread.c
static void do_schedule(int status) {
    ASSERT(intr_get_level() == INTR_OFF);
    ASSERT(thread_current()->status == THREAD_RUNNING);
    while (!list_empty(&destruction_req)) {
        struct thread *victim = list_entry(list_pop_front(&destruction_req), struct thread, elem);
        palloc_free_page(victim);
    }
    thread_current()->status = status;
    schedule();
}
```
그렇다면 우리가 설정해 놓은 `all_elem`은 어떻게 되는 것일까? 그렇다 댕글링 포인터가 되어버린다.

그럼 그 시점이 언제일까?

그렇다. 바로 **시작 후 120초가 지난 시점에서 첫번째 댕글링 포인터가 발생**한다.

```c
// mlfqs-load-60.c
static void load_thread(void *aux UNUSED) {
    int64_t sleep_time = 10 * TIMER_FREQ;
    int64_t spin_time = sleep_time + 60 * TIMER_FREQ;
    int64_t exit_time = spin_time + 60 * TIMER_FREQ;

    thread_set_nice(20);
    timer_sleep(sleep_time - timer_elapsed(start_time));
    while (timer_elapsed(start_time) < spin_time)
        continue;
    timer_sleep(exit_time - timer_elapsed(start_time));
}
```