# 왜 환경 별로 Test 결과가 다를까?

같은 `Pintos Code`를 갖고 `EC2`와 `Docker`, `WSL`에서 각각 다른 `Result` 가 나오는 경우가 있다. 이는 기본적으로 환경 별로 처리 속도가 다르기 때문인데 단적인 예시로 내 `Docker` 환경에서는 1초에 2.1억번 정도 loop가 수행된다.

```
// Docker
Calibrating timer...  209,715,200 loops/s.
```

그런데 같은 팀원의 환경에서 돌려본 결과 `EC2`는 1.3억번, `WSL`에서 돌려본 결과는 3.1억번 정도가 수행된다.

```
// EC2
Calibrating timer...  129,638,400 loops/s

// WSL
Calibrating timer...  314,163,200 loops/s.
```

이 때문에 보통 프로그램이 허술하면 `EC2`에서는 아무런 문제없이 도는데 `Docker`나 `WSL`에서는 `fault`가 발생한다.

결론부터 말하자면 `EC2`는 **성능이 구려서** 사이사이에 일어날 수 있는 잔 버그나 예외 상황이 잘 발생을 하지 않는다.

단적인 예로 `Project 2` 시작할 때 `EC2`에서는 아무런 설정을 안해도 그냥 `Test Case`가 정상적으로 동작하는데 `Docker`나 `WSL`에서는 `intr_yield_on_return` 함수를 추가해주지 않으면 동작하지 않는다.

```c
void test_max_priority(void) {
    if (list_empty(&ready_list))
        return;

    thread_t *th = list_entry(list_front(&ready_list), thread_t, elem);

    if (thread_current()->priority < th->priority) {
#ifdef USERPROG /** Project 2: 외부 인터럽트에 의한 thread yield 방지 */
        if (intr_context())
            intr_yield_on_return();
        else
#endif
            thread_yield();
    }
}
```

그리고 `Project 3` 에서 무엇인가 허술하게 Program을 만들었다면 `page merge test`에서 `EC2`는 아무런 문제없이 `PASS`가 뜨는데 다른 환경에서는 `FAIL`밖에 안뜬다.

이를 확인해보니 프로세스가 `lock_acquire`을 하러 갔다가 뜬금없이 `error code`를 뱉으며 사망했다.

```
exit(-1)
```

 이는 `lock`에 관련 된 문제인데, `EC2`에서는 `fork`된 `thread`가 죽는 속도가 느려서 `lock holder`가 죽는 일이 잘 발생하지 않는데, 다른 환경에서는 이미 죽어있어서 `lock holder`가 무조건 `NULL` 포인터를 반환하게 되고 이 때문에 `Test Result`가 달라지는 것이다.

우리는 이를 막기 위해 `wait_on_lock`만 검사하던 `donate_priority` 함수에 `holder`를 검사하는 부분을 추가하여 `PASS`할 수 있었다.

```c
void donate_priority() {
    thread_t *t = thread_current();
    int priority = t->priority;

    for (int depth = 0; depth < 8; depth++) {
        /** Project 3 에서 child가 먼저 삭제되면 holder가 NULL이 되는 경우가 발생 */
        if (t->wait_lock == NULL || t->wait_lock->holder == NULL)
            break;

        t = t->wait_lock->holder;
        t->priority = priority;
    }
}
```

이렇게 했는 데도 불구하고 `Test`가 `Fail`이 뜬다면 아마 `load`에 `race`가 발생했을 경우 일 것이다. `Error` 코드는 다음과 같이 뜬다.

```
load: buf0: open failed
```

 나는 `open` 만 하는 것은 서로에게 영향이 없는 일인데 왜 `race`가 발생하지? 라는 생각을 했었지만 `process`의 `load`함수 전체에 `lock`을 걸어주면서 해결이 됐다. 
 
 ```c
 static bool load(const char *file_name, struct intr_frame *if_) {
    struct thread *t = thread_current();
    struct ELF ehdr;
    struct file *file = NULL;
    off_t file_ofs;
    bool success = false;
    int i;

    /* Allocate and activate page directory. */
    t->pml4 = pml4_create();
    if (t->pml4 == NULL)
        goto done;
    process_activate(thread_current());

    /** #Project 3: Memory Management - Load Race 방지 */
    lock_acquire(&filesys_lock);

    /* Open executable file. */
    file = filesys_open(file_name);

    if (file == NULL) {
        printf("load: %s: open failed\n", file_name);
        goto done;
    }

    // ...

done:
    /* We arrive here whether the load is successful or not. */
    // file_close(file);
    lock_release(&filesys_lock);

    return success;
}
 ```

 나중에 알고보니 디스크상의 `inode`는 종류, 권한, 타임스탬프 등과 같은 파일의 지속적인 속성을 기록하기 때문에 `close`를 제외한 `filesystem` 접근은 `lock`을 걸어주는게 좋다고 한다...