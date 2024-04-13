#include <stdio.h>
#include <stdlib.h>

typedef struct Node {
    int data;
    struct Node *left;
    struct Node *right;
} Node;


void init(Node *s, int data) {
    s->data = data;
    s->left = NULL;
    s->right = NULL;
}

void push(Node *s, char data) {
    Node *temp = (Node *) malloc(sizeof(Node));

    init(temp, data);

    while (1) {
        if (data < s->data) {
            if (s->left == NULL) {
                printf("Insert: %d -> left\n", s->data);
                s->left = temp;
                break;
            }
            printf("Check: %d -> left\n", s->data);
            s = s->left;
        } else {
            if (s->right == NULL) {
                printf("Insert: %d -> right\n", s->data);
                s->right = temp;
                break;
            }
            printf("Check: %d -> right\n", s->data);
            s = s->right;
        }
    }
}

void delete(Node *s) {
    if (s->left != NULL)
        delete(s->left);
    if (s->right != NULL)
        delete(s->right);

    printf("Removed : %d\n", s->data);
    free(s);
    s = NULL;
}

int main() {
    int n, i, num;
    Node* root = (Node *)malloc(sizeof (Node));

    scanf_s("%d", &n);
    scanf_s("%d", &num);

    init(root, num);

    for (i = 0; i < n-1; i++) {
        scanf_s("%d", &num);
        push(root, num);
    }

    delete(root);

    return 0;
}
