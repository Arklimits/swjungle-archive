# [Week 07] 정글 끝까지 TEAM 7


**********

### 1페이지 WEEK07 WIL

안녕하십니까? 7팀의 발표를 맡게 된 정재혁입니다.
발표에 앞서 Advanced Scheduler의 내용을 안보신 분들도 이해할 수 있도록 설명하고자 해서, 알아야 하는 내용은 많고 시간은 한정되어 있어서 슬라이드가 빠르게 넘어갈 수도 있으니 양해 부탁드립니다.

***********

### 2페이지 ADVANCED SCHEDULER 과제 설명

저희 팀은 1주차 option 과제인 Advanced Scheduler를 구현해봤습니다.

***********

### 3페이지 문제 및 해결

기존의 Priority Scheduler의 경우에는 우선 순위가 낮은 스레드가 오랫동안 CPU를 점유하지 못하는 문제가 발생할 수 있습니다. 이로 인해 스레드의 평균 반응 시간이 너무 길어지는 문제가 발생합니다. 이를 해결 하기 위한 방법으로 multi-level feedback queue scheduler가 제시 되었습니다.

***********

### 4페이지 과제 목표

MLFQS는 Priority에 따라 여러 개의 Ready Queue가 존재하고, Priority에 영향을 주는 변수가 있어서 Feedback으로 Priority를 조절할 수 있습니다.
저희 조는 Multi Level까지는 구현하기 힘들 것 같아서 Feedback만 구성을 해봤습니다.

************

### 5페이지 ADVANCED SCHEDULER KEYWORD

그래서 저희가 Advanced Scheduler에서 중요한 키워드들을 정리해 봤습니다.

************

### 6페이지 TOP KEYWORDS

첫번째로 NICENESS가 있습니다. 이는 스레드의 젠틀함을 나타내 줍니다. 이 수치가 높으면 Priority가 줄어들어 CPU를 더 적게 점유하게 됩니다.
두번째로 PRIORITY는 기존의 고정되거나 Donation된 태생적인 Priority가 아닌 CPU 사용량과 Niceness에 의해 결정되게 됩니다.
세번째로 RECENT_CPU는 말 그대로 최근 CPU의 점유 TICK을 나타냅니다. 이 수치가 높으면 역시 Priority가 줄어들게 됩니다.
네번째로 LOAD AVG가 있는 데 시스템의 현재 부하 상태를 나타내는 지표입니다.

************

### 7페이지 KEYWORDS FORMULA

PRIORITY, RECENT_CPU, LOAD_AVG를 구하는 식은 다음과 같습니다.
PRIORITY는 제시된 가중치로 결정되게 되고, RECENT CPU의 경우 CPU 점유시간의 값을 지수 가중 이동 평균을 사용하여 계산하게 됩니다. LOAD AVERAGE는 1분 동안 수행 가능한 스레드의 평균 개수를 추정하는 계산식입니다.

NICENESS가 없는데 이는 Test Program에서 임의로 지정해줍니다.

이 때 주의해야 할 점이 있습니다. 바로 실수형이라는 것입니다. 그러나 핀토스에서는 실수를 지원하지 않습니다. 이를 위해 17.14 고정소수점 표현을 사용하게 됩니다.

************

### 8페이지 FIXED-POINT ARITHMETIC

17.14 고정소수점 표현이란 32비트짜리 정수형을 갖고 14번째 자리를 기준으로 제일 앞 1자리는 부호, 왼쪽 17자리는 정수, 오른쪽 14자리는 소수로 사용하는 기법입니다. 

************

### 9페이지 FIXED-POINT MACRO

그래서 F라는 2의14승짜리 숫자를 만들어 실수와 정수를 전환하게 되고 이에 따라 특수한 매크로를 만들어 연산을 수행해야 합니다. 예를 들면 덧셈을 할때는 정수에 F를 곱해 실수로 만들어 줄 수 있습니다. 실수끼리 곱셈을 할 때는 둘 다 F가 곱해진 상태이므로 중복된 F를 삭제하기 위해 F로 나누어 줍니다.

************

### 10페이지 FEEDBACK 계산 함수

그래서 Advanced Scheduler를 구현하면서 이전의 키워드들을 계산 하는 데에 이 매크로들을 사용했습니다.

************

### 11페이지 구현

이제 주요 구현 함수에 대해서 설명해 드리겠습니다.

************

### 12페이지 THREAD SET PRIORITY

먼저 Priority 또한 새롭게 계산을 하였는데, 기존의 함수가 Priority를 조절할 수 있기 때문에 thread_mlfqs인자가 선언이 되어있으면 동작하지 않고 return하게 수정하였습니다.

************

### 13페이지 LOCK ACQUIRE & LOCK RELEASE

또한, Priority가 Donate될 경우 mlfqs의 Priority와 충돌할 것을 염려해 Lock Acquire와 Release 시에 Priority Donation을 작동하지 않도록 수정하였습니다.

************

### 14페이지 TIMER INTERRUPT

Advanced Scheduler에서 가장 중요한 Timer Interrupt 함수 입니다. 결국 Priority 계산은 Timer가 Interrupt할 때마다, 즉 1틱마다 현재 thread의 CPU 점유시간을 1씩 증가시켜주는 함수가 들어 있습니다. 또한, 4틱마다 CPU 점유시간, Niceness에 의해 모든 Thread의 Priority를 재계산합니다. 그리고 100틱마다 Load Average와 모든 Thread의 Recent CPU를 재조정하게 됩니다. 이 때의 재조정이 1씩 증가된 Recent CPU 값을 지수 가중 이동 평균으로 재조정하는 것입니다.

************

### 15페이지 RECALCULATE RECENT CPU & PRIORITY

그런데 기존에 사용하던 Ready List는 대기중인 Thread만 들어있기 때문에 모든 활성화 된 list를 새로 만들어 모든 thread, 즉 sleep 중인 thread까지 priority와 recent cpu를 계산합니다.
그 외에 수정해야 하는 함수가 더 있으나 시간 관계상 설명을 생략하고 테스트 결과를 보여 드리겠습니다.

************

### 16페이지 결과

테스트 프로그램의 실행 결과는 0초 부터 60초 까지 활성화 되는 스레드가 점점 많아지기 때문에 60초까지는 load avg가 증가하고 180초 까지는 다시 감소하는 것을 볼 수 있었습니다.

************

### 17페이지 감사합니다.

이상으로 발표를 마치겠습니다. 감사합니다. 질문 있으신가요?
