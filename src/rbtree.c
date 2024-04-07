#include "rbtree.h"
#include <stdio.h>
#include <stdlib.h>

rbtree *new_rbtree(void) {
    rbtree *p = (rbtree *)calloc(1, sizeof(rbtree));
    // TODO: initialize struct if needed
    node_t *NIL = (node_t *)calloc(1, sizeof(node_t));

    NIL->color = RBTREE_BLACK;

    p->root = p->nil = NIL;

    printf("SUCEESS: new_rbtree(%x)\n", p);
    return p;
}

void delete(rbtree *t, node_t *p){
    if (p->left != t->nil)
        delete(t, p->left);
    if (p->right != t->nil)
        delete(t, p->right);

    printf("SUCEESS: delete(%x)\n", p);
    free(p);
    p = NULL;
}

void delete_rbtree(rbtree *t) {
    // TODO: reclaim the tree nodes's memory
    if (t->root != t->nil)
        delete(t, t->root);

    printf("SUCEESS: delete_rbtree(%x)\n", t);
    free(t->nil);
    t->nil = NULL;
    free(t);
    t = NULL;
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

    printf("SUCEESS: rbtree_insert(%x)->%d\n",temp, temp->key);
    return t->root;
}

node_t *rbtree_find(const rbtree *t, const key_t key) {
    // TODO: implement find
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
    printf("FAILED: rbtree_find(NULL)\n");
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
    node_t *node;
    node_t *node_parent, *replace;
    int is_remove_black, is_remove_left;

    if (p->left != t->nil && p->right != t->nil)
    {
        if (node == t->nil){
            node = p;
            while (1){
                if (node->parent->right == node)
                    node = node->parent;
                else
                    node = node->parent;
                break;
            }
        }
        else{
            node = p->right;
            while (node->left != t->nil)
                node = node->left;
        }

        replace = node->right;
        p->key = node->key;
    }
    else
    {
        node = p;
        replace = (node->right != t->nil) ? node->right : node->left;
    }
    node_parent = node->parent;

    if (node == t->root)
    {
        t->root = replace;
        t->root->color = RBTREE_BLACK;
        printf("SUCCESS: rbtree_erase(%x)->%d\n", node, node->key);
        free(node);
        node=NULL;
        return 0;
    }

    is_remove_black = node->color;
    is_remove_left = node_parent->left == node;

    if (is_remove_left)
        node_parent->left = replace;
    else
        node_parent->right = replace;

    replace->parent = node_parent;

    printf("SUCCESS: rbtree_remove(%x)->%d\n", replace, replace->key);
    free(node);
    node=NULL;

    return 0;
}

int rbtree_to_array(const rbtree *t, key_t *arr, const size_t n) {
    // TODO: implement to_array
    return 0;
}
