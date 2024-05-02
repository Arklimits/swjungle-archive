********************************************
# Extra Questions
📢 이번 주 과정은 Red-black tree만 익히는 게 아닙니다.
Red-black tree라는 data structure의 작동 구조에만 집중하고 있는 듯 한데, 아시다시피 이번 과정은 data structure만 익히는 게 아닙니다.
C언어와 컴퓨터의 좀 더 낮은 구조를 이해하고 그것을 이용할 수 있는 능력을 익히는 것도 있고,
기본적으로는 C 언어로 짠 프로그램이 제대로 돌아가는지, 문제가 없는지 파악하고 문제를 해결하는 방법을 익히는 것도 중요합니다.
********************************************
#### 1. Segmentation Fault (core dumped) 라는 메시지를 받고 무엇이 잘못되었는지 파악할 수 있는지

#### 2. core dumped 라고 하는데 core라는 녀석은 어디에 dump되며 core가 있으면 뭘 할 수 있는지

#### 3. valgrind는 뭐고 gdb는 뭔지

#### 4. char *s1 ="abc"; 와 char *s2 = malloc(4); 라는 문장에서 s1과 s2가 저장된 주소, malloc이 return 하는 주소는 어떤 메모리 세그먼트에 할당되는지

#### 5. C에서는 return "abc"; 같은 짓을 하면 왜 쌍욕을 먹는지

#### 6. malloc을 해 두고 free를 안하면 어떻게 되는지

#### 7. free 한 이후에도 pointer 변수로 값을 바꾸면 어떻게 되는지

#### 8. 왜 sizeof(struct node { char color; struct node *next; }) 가 9가 아니고 16이 되는지