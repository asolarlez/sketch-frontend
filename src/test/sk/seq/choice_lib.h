/**
 * Usage example:
 * CHOICE_INT(x1)
 * mysynthfunc() { choice(x1); }
 */

#pragma once
#define CHOICE_INT(VAR) \
    int[100] choiceValues_ ## VAR = ??; \
    int choiceCtr_ ## VAR = 0; \
    int choice_ ## VAR() { \
        int oldctr = choiceCtr_ ## VAR; \
        choiceCtr_ ## VAR++; \
        return choiceValues_ ## VAR[oldctr]; \
    } \

#define choice(var) (choice_ ## var())
#define reset(var) { choiceCtr_ ## var = 0; }
#define setChoiceArr(var, arr, N) \
    for (int a = 0; a < N; a++) { arr[a] = choice(var); }
