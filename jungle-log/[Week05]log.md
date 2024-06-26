# [WEEK05] 탐험준비 - Malloc Lab
  정신 없는 5주차가 지나갔다. malloc lab은 OS에서 malloc이 작동하는 방식을 학습하기 위해 모형을 만들어보고 이해하는 과정이었다.

  중간에 EC2가 터지는 불상사가 일어났다. 그래서 Docker로 Ubuntu 환경을 만드는 것에도 도전해봤다. 처음에는 진짜 뭐가 뭔지 Docker를 어떻게 다뤄야 하는지도 몰라서 오랜 시간동안 버벅였다. 그러다 Container를 내가 직접 Docker에서 만드는 것이 아닌VS Code에 내장되어 있는 기능을 이용해서 이미지를 만들고 devcontainer를 구성하는 것이 훨씬 효율적이고 편하다는 것을 알게 됐다. 심지어 devcontainer를 폴더 안에 넣어놓고 VS Code로 실행하기만 해도 알아서 드라이브와 bind 해줄 뿐만 아니라 폴더 내의 환경 변수에 따라 플러그인까지 설치해준다! 아직은 Docker를 완전히 깊게 파고들만한 시간은 없어서 (이번 주는 시간이 너무 부족해서 CS공부를 가장 못하기도 했다.) 다음에 다시 제대로 해보고 싶다는 생각이 들었다.

  이번 주는 그 간에 학습한 것 중에서 양이 가장 많은 주차였다. 일단 동적 메모리 할당에 대해 CS 내용이 생각보다 엄청나게 많았고, (어떻게 보면 당연한 것 같기도 하다.)많은 것도 많은 건데 프로그램을 구현하는 것은 CSAPP책의 코드를 보고 따라 친다고 하더라도 이를 Explicit이나 Segregated Fit으로 구현하는 등 기존 내용 또한 생각을 많이 해보고 진입해도 시행착오를 꽤 거쳤다. 심지어 CMU에서 만든 자동 채점 프로그램이 있는 데, 학습하는 것과 별개로 코드가 얼마나 공간을 절약해서 사용하는 지에 대한 채점 시스템도 들어있었다. 이번 주차의 학습 목표는 Implicit, Explicit, Segregated, Buddy System을 구현해보는 것이 목표였다. 처음에는 그냥 학습하라고 한 모형들만 학습해야겠다고 생각하면서 진행을 했는 데, 내가 만든 프로그램을 디버그할 때 마다 나오는 점수가 생각보다 신경쓰였다. 그러다보니 정신차려보면 최적화만 하고 있고 구현하라고 한 모델은 뒷전..이 돼버렸다. 결국 정글에 입소 후 처음으로 학습 목표에 도달하지 못했다. 최적화하면서 시간을 보내다보니 정신차렸을 땐 이미 화요일 저녁이었고, 버디 시스템을 도저히 구현할 수 없을 것 같아 코드 답습만 하고 끝냈다.

  이번 주는 후회도 가장 많이 되는 주차였다. 최적화하는 데에 너무 많은 시간을 쏟아 붓다보니 결국 학습 목표도 도달하지 못했고 이론 공부도 너무 못했다는 생각이 들었다. 6주차에는 민방위 훈련까지 끼어있는 주여서 집으로 올라갔다 올 예정인데 이런 식으로 하다간 밑도 끝도 없을 것 같다는 생각이 들었다. 그래서 6주차에는 일단 무조건 학습 목표부터 도달하고, 이론 공부까지 다 끝내고도 시간이 남으면 코드를 최적화를 해야겠다고 마음 먹었다.
