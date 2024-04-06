#include "rbtree.h"
#include <stdio.h>
#include <stdlib.h>

rbtree *new_rbtree(void) {
  rbtree *p = (rbtree *)calloc(1, sizeof(rbtree));
  // TODO: initialize struct if needed
  node_t *NIL = (node_t *)calloc(1, sizeof(node_t));

  NIL->color = RBTREE_BLACK;

  p->root = p->nil = NIL;

  printf("SUCEESS: new_rbtree\n");
  return p;
}

void delete(rbtree *t, node_t *p){
  if (p->left != t->nil)
    delete(t, p->left);
  if (p->right != t->nil)
    delete(t, p->right);

  free(p);
  p = NULL;
}

void delete_rbtree(rbtree *t) {
  // TODO: reclaim the tree nodes's memory
  if (t->root != t->nil)
    delete(t, t->root);

  free(t->nil);
  t->nil = NULL;
  free(t);
  t = NULL;

  printf("SUCEESS: delete_rbtree\n");
}

node_t *rbtree_insert(rbtree *t, const key_t key) {
  // TODO: implement insert
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

  temp->color = RBTREE_RED;
  temp->key = key;
  temp->parent = node;
  temp->left = temp->right = t->nil;

  if (node == t->nil)
    t->root = temp;

  printf("SUCEESS: rbtree_insert\n");
  return t->root;
}

node_t *rbtree_find(const rbtree *t, const key_t key) {
  // TODO: implement find
  node_t *node = t->root;
  while (node != t->nil){
    if (key == node->key)
      return node;
    else if (key < node->key)
      node = node->left;
    else if (key > node->key)
      node = node->right;
  }

  return NULL;
}

node_t *rbtree_min(const rbtree *t) {
  // TODO: implement find
  return t->root;
}

node_t *rbtree_max(const rbtree *t) {
  // TODO: implement find
  return t->root;
}

int rbtree_erase(rbtree *t, node_t *p) {
  // TODO: implement erase
  printf("SUCCESS: rbtree_erase\n");
  return 0;
}

int rbtree_to_array(const rbtree *t, key_t *arr, const size_t n) {
  // TODO: implement to_array
  return 0;
}
