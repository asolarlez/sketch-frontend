
struct Node {
    int obj;
    Node next;
    bit taken;
}

struct Queue {
    Node prevHead;
    Node tail;
}

void enqueue(Queue Q, int obj) {
    Node tmp = null;
    Node n = new Node ();
    n.obj = obj;
    n.taken = 0;
    n.next = null;

    Node tail = Q.tail;

    reorder {





        if (??) {
            Q.tail = tail;
        } else if (??) {
            Q.tail = tmp;
        } else if (??) {
            Q.tail = n;
        } else if (??) {
            Q.tail = null;
        } else if (??) {
            Q.tail = tail.next;
        } else if (??) {
            Q.tail = tmp.next;
        } else if (??) {
            Q.tail = n.next;
        }

        else if (??) {
            tail.next = tail;
        } else if (??) {
            tail.next = tmp;
        } else if (??) {
            tail.next = n;
        } else if (??) {
            tail.next = null;
        } else if (??) {
            tail.next = tail.next;
        } else if (??) {
            tail.next = tmp.next;
        } else if (??) {
            tail.next = n.next;
        }

        else if (??) {
            tmp = tail;
        } else if (??) {
            tmp = tmp;
        } else if (??) {
            tmp = n;
        } else if (??) {
            tmp = null;
        } else if (??) {
            tmp = tail.next;
        } else if (??) {
            tmp = tmp.next;
        } else if (??) {
            tmp = n.next;
        }

        else if (??) {
            tmp.next = tail;
        } else if (??) {
            tmp.next = tmp;
        } else if (??) {
            tmp.next = n;
        } else if (??) {
            tmp.next = null;
        } else if (??) {
            tmp.next = tail.next;
        } else if (??) {
            tmp.next = tmp.next;
        } else if (??) {
            tmp.next = n.next;
        }

        else if (??) {
            n = tail;
        } else if (??) {
            n = tmp;
        } else if (??) {
            n = n;
        } else if (??) {
            n = null;
        } else if (??) {
            n = tail.next;
        } else if (??) {
            n = tmp.next;
        } else if (??) {
            n = n.next;
        }

        else if (??) {
            n.next = tail;
        } else if (??) {
            n.next = tmp;
        } else if (??) {
            n.next = n;
        } else if (??) {
            n.next = null;
        } else if (??) {
            n.next = tail.next;
        } else if (??) {
            n.next = tmp.next;
        } else if (??) {
            n.next = n.next;
        }





        if (??) {
            atomic { tmp = Q.tail; Q.tail = tail; };
        } else if (??) {
            atomic { tmp = Q.tail; Q.tail = tmp; };
        } else if (??) {
            atomic { tmp = Q.tail; Q.tail = n; };
        } else if (??) {
            atomic { tmp = Q.tail; Q.tail = null; };
        } else if (??) {
            atomic { tmp = Q.tail; Q.tail = tail.next; };
        } else if (??) {
            atomic { tmp = Q.tail; Q.tail = tmp.next; };
        } else if (??) {
            atomic { tmp = Q.tail; Q.tail = n.next; };
        }

        else if (??) {
            atomic { tmp = tail.next; tail.next = tail; };
        } else if (??) {
            atomic { tmp = tail.next; tail.next = tmp; };
        } else if (??) {
            atomic { tmp = tail.next; tail.next = n; };
        } else if (??) {
            atomic { tmp = tail.next; tail.next = null; };
        } else if (??) {
            atomic { tmp = tail.next; tail.next = tail.next; };
        } else if (??) {
            atomic { tmp = tail.next; tail.next = tmp.next; };
        } else if (??) {
            atomic { tmp = tail.next; tail.next = n.next; };
        }

        else if (??) {
            atomic { tmp = tmp; tmp = tail; };
        } else if (??) {
            atomic { tmp = tmp; tmp = tmp; };
        } else if (??) {
            atomic { tmp = tmp; tmp = n; };
        } else if (??) {
            atomic { tmp = tmp; tmp = null; };
        } else if (??) {
            atomic { tmp = tmp; tmp = tail.next; };
        } else if (??) {
            atomic { tmp = tmp; tmp = tmp.next; };
        } else if (??) {
            atomic { tmp = tmp; tmp = n.next; };
        }

        else if (??) {
            atomic { tmp = tmp.next; tmp.next = tail; };
        } else if (??) {
            atomic { tmp = tmp.next; tmp.next = tmp; };
        } else if (??) {
            atomic { tmp = tmp.next; tmp.next = n; };
        } else if (??) {
            atomic { tmp = tmp.next; tmp.next = null; };
        } else if (??) {
            atomic { tmp = tmp.next; tmp.next = tail.next; };
        } else if (??) {
            atomic { tmp = tmp.next; tmp.next = tmp.next; };
        } else if (??) {
            atomic { tmp = tmp.next; tmp.next = n.next; };
        }

        else if (??) {
            atomic { tmp = n; n = tail; };
        } else if (??) {
            atomic { tmp = n; n = tmp; };
        } else if (??) {
            atomic { tmp = n; n = n; };
        } else if (??) {
            atomic { tmp = n; n = null; };
        } else if (??) {
            atomic { tmp = n; n = tail.next; };
        } else if (??) {
            atomic { tmp = n; n = tmp.next; };
        } else if (??) {
            atomic { tmp = n; n = n.next; };
        }

        else if (??) {
            atomic { tmp = n.next; n.next = tail; };
        } else if (??) {
            atomic { tmp = n.next; n.next = tmp; };
        } else if (??) {
            atomic { tmp = n.next; n.next = n; };
        } else if (??) {
            atomic { tmp = n.next; n.next = null; };
        } else if (??) {
            atomic { tmp = n.next; n.next = tail.next; };
        } else if (??) {
            atomic { tmp = n.next; n.next = tmp.next; };
        } else if (??) {
            atomic { tmp = n.next; n.next = n.next; };
        }






        {
            bit cond = 0;
            if (??) {
                if (??) {
                    cond = (tmp == tail);
                } else if (??) {
                    cond = (tmp == tmp);
                } else if (??) {
                    cond = (tmp == n);
                } else if (??) {
                    cond = (tmp == null);
                } else if (??) {
                    cond = (tmp == tail.next);
                } else if (??) {
                    cond = (tmp == tmp.next);
                } else if (??) {
                    cond = (tmp == n.next);
                }
            } else if (??) {
                if (??) {
                    cond = (tmp != tail);
                } else if (??) {
                    cond = (tmp != tmp);
                } else if (??) {
                    cond = (tmp != n);
                } else if (??) {
                    cond = (tmp != null);
                } else if (??) {
                    cond = (tmp != tail.next);
                } else if (??) {
                    cond = (tmp != tmp.next);
                } else if (??) {
                    cond = (tmp != n.next);
                }
            }
            if (cond) {
                if (??) {
                    Q.tail = tail;
                } else if (??) {
                    Q.tail = tmp;
                } else if (??) {
                    Q.tail = n;
                } else if (??) {
                    Q.tail = null;
                } else if (??) {
                    Q.tail = tail.next;
                } else if (??) {
                    Q.tail = tmp.next;
                } else if (??) {
                    Q.tail = n.next;
                }

                else if (??) {
                    tail.next = tail;
                } else if (??) {
                    tail.next = tmp;
                } else if (??) {
                    tail.next = n;
                } else if (??) {
                    tail.next = null;
                } else if (??) {
                    tail.next = tail.next;
                } else if (??) {
                    tail.next = tmp.next;
                } else if (??) {
                    tail.next = n.next;
                }

                else if (??) {
                    tmp = tail;
                } else if (??) {
                    tmp = tmp;
                } else if (??) {
                    tmp = n;
                } else if (??) {
                    tmp = null;
                } else if (??) {
                    tmp = tail.next;
                } else if (??) {
                    tmp = tmp.next;
                } else if (??) {
                    tmp = n.next;
                }

                else if (??) {
                    tmp.next = tail;
                } else if (??) {
                    tmp.next = tmp;
                } else if (??) {
                    tmp.next = n;
                } else if (??) {
                    tmp.next = null;
                } else if (??) {
                    tmp.next = tail.next;
                } else if (??) {
                    tmp.next = tmp.next;
                } else if (??) {
                    tmp.next = n.next;
                }

                else if (??) {
                    n = tail;
                } else if (??) {
                    n = tmp;
                } else if (??) {
                    n = n;
                } else if (??) {
                    n = null;
                } else if (??) {
                    n = tail.next;
                } else if (??) {
                    n = tmp.next;
                } else if (??) {
                    n = n.next;
                }

                else if (??) {
                    n.next = tail;
                } else if (??) {
                    n.next = tmp;
                } else if (??) {
                    n.next = n;
                } else if (??) {
                    n.next = null;
                } else if (??) {
                    n.next = tail.next;
                } else if (??) {
                    n.next = tmp.next;
                } else if (??) {
                    n.next = n.next;
                }
            }
        }
    }
}

int dequeue (Queue Q) {
    Node n = Q.prevHead.next;
    bit took = 0;

    bit more = 1;
    for (int i = 0; i < 4; ++i) {
        if (more) {
            if (n == null) {
                more = 0;
            } else {
                atomic { took = n.taken; n.taken = 1; };
                if (took == 0) {
                    more = 0;
                } else {
                    n = n.next;
                }
            }
        }
    }

    if (n == null) {
        return -1;
    } else {
        Node p = ?? ? Q.prevHead : n;
        bit more2 = 1;
        for (int i = 0; i < 4; ++i) {
            if (more2) {
                if (p == null) {
                    more2 = 0;
                } else {
                    if (??) {
                        if (p.taken) {
                            Q.prevHead = p;
                            p = p.next;
                        } else {
                            more2 = 0;
                        }
                    } else {
                        if (p.next.taken) {
                            Q.prevHead = p;
                            p = p.next;
                        } else {
                            more2 = 0;
                        }
                    }
                }
            }
        }
    }
    return n.obj;
}

Queue newQueue () {
    Queue q = new Queue ();
    q.prevHead = new Node ();
    q.prevHead.obj = -1;
    q.prevHead.taken = 1;
    q.prevHead.next = null;
    q.tail = q.prevHead;
    return q;
}



bit verify (Queue q, int[2] popped) {
    Node next = q.prevHead;

    bit more = 1;
    for (int i = 0; i < 4; ++i) {
        if (more && next != null) {
            assert next.taken == 1;
            next = next.next;
        } else {
            more = 0;
        }
    }

    assert (popped[0] == 1 && popped[1] == 2)
           || (popped[0] == 2 && popped[1] == 1);

    return 1;
}

bit alwaysTrue () { return 1; }
bit dequeueTest () implements alwaysTrue {
    bit ret = 1;


    Queue seq = newQueue ();
    int[2] seqPopped = 0;
    enqueue (seq, 1);
    enqueue (seq, 2);
    seqPopped[0] = dequeue (seq);
    seqPopped[1] = dequeue (seq);
    ret = ret && verify (seq, seqPopped);

    Queue par = newQueue ();
    int[2] parPopped = 0;
    enqueue (par, 1);
    enqueue (par, 2);
    fork (int i; 2) {
        parPopped[i] = dequeue (par);
    }
    ret = ret && verify (par, parPopped);

    return ret;
}
