#include "rbtree.h"
#include <stdio.h>
#include <stdlib.h>

rbtree *new_rbtree(void) {
  rbtree *p = (rbtree *)calloc(1, sizeof(rbtree));

  node_t *NIL = (node_t *)calloc(1, sizeof(node_t));
  NIL->color = RBTREE_BLACK;
  
  p->root = p->nil = NIL;

  printf("SUCEESS: new_rbtree(%x)\n", p);
  return p;
}

void delete(rbtree *t, node_t *p) {
  if (p->left != t->nil)
      delete(t, p->left);
  if (p->right != t->nil)
      delete(t, p->right);

  printf("SUCEESS: delete_node(%x)\n", p);
  free(p);
  p = NULL;
}

void delete_rbtree(rbtree *t) {
  if (t->root != t->nil)
    delete(t, t->root);

  printf("SUCEESS: delete_rbtree(%x)\n", t);
  free(t->nil);
  t->nil = NULL;
  free(t);
  t = NULL;
}

node_t *rbtree_travel(const rbtree *t, node_t *p) {
  node_t *node = p->right;                        // 노드 오른쪽 자식부터 시작

  if (node == t->nil){                            // 노드가 오른쪽 자식이 없을 경우
    node = p;                                     // 노드에서 다시 시작
    while (node != t->nil){
      if (node->parent->right == node)            // 노드가 오른쪽 자식일 경우
        node = node->parent;                      // 노드가 왼쪽 자식일 때까지 부모로 거슬러 올라감
      else
        return node->parent;                      
    }
  }
  while (node->left != t->nil)                    // 그 외에는 가장 작은 값을 반환
    node = node->left;

  return node;
}

void left_rotate(rbtree *t, node_t *node) {
  node_t *parent = p->parent;
  node_t *grand_parent = parent->parent;

  node->parent = grand_parent;                    // 노드와 조부모를 양방향 연결
  if (parent == t->root)                          // 부모가 루트일 경우 노드를 루트로 변경
    t->root = node;
  else if (grand_parent->left == parent)
    grand_parent->left = node;
  else
    grand_parent->right = node;
  
  parent->parent = node;                          // 노드를 부모의 부모로 변경
  parent->right = node->left;                     // 부모의 오른쪽 자식을 노드 왼쪽 자식으로
  node->left->parent = parent;                    // 노드 왼쪽 자식에게 부모 양도
  node->left = parent;                            // 부모를 노드 오른쪽 자식으로 변경
}

void right_rotate(rbtree *t, node_t *node) {
  node_t *parent = p->parent;
  node_t *grand_parent = parent->parent;

  node->parent = grand_parent;                    // 노드와 조부모를 양방향 연결
  if (parent == t->root)                          // 부모가 루트일 경우 노드를 루트로 변경
    t->root = node;
  else if (grand_parent->left == parent)
    grand_parent->left = node;
  else
    grand_parent->right = node;

  parent->parent = node;                          // 노드를 부모의 부모로 변경
  parent->left = node->right;                     // 부모의 왼쪽 자식을 노드 오른쪽 자식으로
  node->right->parent = parent;                   // 노드 오른쪽 자식에게 부모 양도
  node->right = parent;                           // 부모를 노드 오른쪽 자식으로 변경
}

node_t *rbtree_insert_fixup(rbtree *t, node_t *node){
  node_t *parent = node->parent;
  node_t *parent_parent = node->parent->parent;
  node_t *uncle;
  int is_left = (node == parent->left);           // 노드가 오른쪽 자식인지 판별
  int is_parent_is_left;                          // 부모가 오른쪽 자식인지 판별

}

node_t *rbtree_insert(rbtree *t, const key_t key) {
  node_t *temp = (node_t *)calloc(1, sizeof(node_t));
  node_t *node = t->root;

  while (node != t->nil){
    if (key < node->key){
      if (node->left == t->nil){
        node->left = temp;
        break;
      }
      node = node->left;
    }
    else{
      if (node->right == t->nil){
        node->right = temp;
        break;
      }
      node = node->right;
    }
  }

  temp->color = RBTREE_RED;                       // 노드 생성 시 항상 red로 설정
  temp->key = key;
  temp->parent = node;
  temp->left = temp->right = t->nil;              // 노드 생성 시 leaf를 nil 노드로 설정

  if (node == t->nil)                             // 비어있는 루트일 경우 노드를 루트로 설정
    t->root = temp;

  rbtree_insert_fixup(t, temp);
  
  printf("SUCEESS: rbtree_insert(%x)->%d\n",temp, temp->key);
  return t->root;
}

node_t *rbtree_find(const rbtree *t, const key_t key) {
  node_t *node = t->root;

  while (node != t->nil){
    if (key == node->key){
      printf("SUCCESS: rbtree_find(%x)->%d\n", node, node->key);
      return node;
    }
    else if (key < node->key)
      node = node->left;
    else if (key > node->key)
      node = node->right;
  }

  printf("FAILED: rbtree_find->%d\n", key);
  return NULL;
}

node_t *rbtree_min(const rbtree *t) {
  node_t *node = t->root;

  while (node->left != t->nil)
    node = node->left;

  printf("SUCCES: rbtree_min(%x)->%d\n", node, node->key);
  return node;
}

node_t *rbtree_max(const rbtree *t) {
  node_t *node = t->root;

  while (node->right != t->nil)
    node = node->right;

  printf("SUCCES: rbtree_max(%x)->%d\n", node, node->key);
  return node;
}

int rbtree_erase(rbtree *t, node_t *p) {
  node_t *node;
  node_t *node_parent, *replace;
  int is_remove_left;

  if (p->left != t->nil && p->right != t->nil) {
    node = rbtree_travel(t, p);
    replace = node->right;
    p->key = node->key;
  }
  else {
    node = p;
    replace = (node->right != t->nil) ? node->right : node->left;
  }
  node_parent = node->parent;

  if (node == t->root) {
    t->root = replace;
    t->root->color = RBTREE_BLACK;
    printf("SUCCESS: rbtree_erase(%x)->%d\n", node, node->key);
    free(node);
    node=NULL;
    return 0;
  }

  is_remove_left = node_parent->left == node;

  if (is_remove_left)
    node_parent->left = replace;
  else
    node_parent->right = replace;

  replace->parent = node_parent;

  printf("SUCCESS: rbtree_erase(%x)->%d\n", node, node->key);
  free(node);
  node=NULL;

  return 0;
}

int rbtree_to_array(const rbtree *t, key_t *arr, const size_t n) {
  node_t *node = rbtree_min(t);
  arr[0] = node->key;

  for (int i=1;i<n;i++) {
    node = rbtree_travel(t, node);
    if (node == t->nil)
      break;

    arr[i] = node->key;
  }
  
  return 0;
}
