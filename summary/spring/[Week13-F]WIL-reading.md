# [Week 13] 나만의 무기를 준비하기


**********

### 1페이지 WEEK13 WIL

안녕하십니까? 저는 금주에 새롭게 배운 내용에 대해 발표하려고 합니다.
저는 JWT 토큰 관련해서 발생한 문제들을 글로벌 핸들러에서 처리하도록 코드를 짰습니다.
그런데 이와 관련해서 문제가 발생했었고, 이를 해결한 방법에 대해 발표하겠습니다.

***********

### 2페이지 Global Exception Handler

저는 먼저 Global Exception Handler를 만들어 모든 Exception들을 한 곳에서 처리하도록 만들었습니다.
Null Pointer, Access Denied, Username Not Found 등의 Exception을 Client로 보내도록 만들었습니다.

***********

### 3페이지 Null Pointer Exception

그래서 존재하지 않는 포스트를 수정하라고 요청할 시
정상적인 Response가 돌아오는 것을 확인할 수 있었습니다.

***********

### 4페이지 Expired Token

그런데 Postman에서 만료된 Token으로 서버에 요청을 보내 Token이 유효한 지 확인하는 코드에서 Expired JWT Exception을 발생시켰는데

***********

### 5페이지 Expired Token

이 Exception이 Client로 메시지를 보내지 못하는 것을 발견했습니다.

?

***********

### 6페이지 Token Valid Check

코드에서 internal하게 exception이 발생해서 그런 것 같아 try catch로 새로운 exception을 발생시켜봤습니다.

똑같네요

혹시 코드내에서 exception이 발생해서 그런가 싶어 parse를 직접하여 Exception을 만들어봤습니다.

똑같네요

그래서 이것저것 해보다가 알아낸 것은 바로 Exception이 일어나는 Scope가 다르다는 것이었습니다.

***********

### 7페이지 Exception Handler

현재 Global Exception Handler는 Spring Context내에서만 동작하기 때문에
내부에서 일어난 Exception을 Global Exception Handler가 Intercept 합니다.

그러나 Token 검증은 Filter에서 일어나기 때문에
Global Exception Handler가 Intercept 할 수 없었던 것이었습니다.

***********

### 8페이지 Exception Handler

그래서 Security Config에서 exception handling을 위한 entry point를 만들어줬고

***********

### 9페이지 Success

Client에서 정상적으로 Error Message를 수신할 수 있게 됐습니다.

***********

### 10페이지 감사합니다

이상으로 발표 마치겠습니다. 감사합니다.
