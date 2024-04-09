.PHONY: test

CFLAGS=-I ../src -Wall -g -DSENTINEL

test: test-rbtree-check
	./test-rbtree-check
	valgrind ./test-rbtree-check

test-rbtree-check: test-rbtree-check.o ../src/rbtree.o

../src/rbtree.o:
	$(MAKE) -C ../src rbtree.o

clean:
	rm -f test-rbtree-check *.o
