# Pintos의 Test Case는 완벽하지 않다.

Pintos의 test를 맹신하지 마라.

Test를 실행하다가 이상한 부분을 발견했는데 2번째 msg에서 실제로 값에 접근하지 않았는 데 `VA`가 정확히 `ACTUAL`로 부터 4096만큼 떨어진 곳에 위치한 곳에 `ACTUAL + 4096`이 있는 것을 발견했다.

확인해 보니 실제로 프로그램은 `ACTUAL + 4096`을 할당해주긴 하지만 접근을 하지않아 `page fault`조차 뜨지 않았다.

이는 말이 안된다고 판단해서 내 코드를 계속 뜯어보다가 나중에 Test Case를 열어보게 되었다.

```c
#define ACTUAL ((void *) 0x10000000)


void
test_main (void)
{
  int handle;
  void *map;

  CHECK ((handle = open ("sample.txt")) > 1, "open \"sample.txt\"");
  CHECK ((map = mmap (ACTUAL, 0x2000, 0, handle, 0)) != MAP_FAILED, "mmap \"sample.txt\"");
  msg ("memory is readable %d", *(int *) ACTUAL);
  msg ("memory is readable %d", *(int *) ACTUAL + 0x1000);

  munmap (map);

  fail ("unmapped memory is readable (%d)", *(int *) (ACTUAL + 0x1000));
  fail ("unmapped memory is readable (%d)", *(int *) (ACTUAL));
}
```

그렇다. 2번째 줄 `msg`에 괄호처리가 안돼있다. 이를

```c
  msg ("memory is readable %d", *(int *) (ACTUAL + 0x1000));
```

로 수정해주면 제대로 동작하는 것을 확인할 수 있다...