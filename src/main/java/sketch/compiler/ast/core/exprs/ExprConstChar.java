/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package sketch.compiler.ast.core.exprs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.cmdline.BoundOptions;
import sketch.compiler.main.cmdline.SketchOptions;

/**
 * A single-character literal, as appears inside single quotes in Java.
 * 
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class ExprConstChar extends ExprConstant
{
    private final char val;
    private int id;
    public static final ExprConstChar zero = new ExprConstChar(0, '\0');
    
    static Map<Character, ExprConstChar> initMap() {
        Map<Character, ExprConstChar> m = new HashMap<Character, ExprConstChar>();
        m.put('\0', zero);
        return m;
    }

    static List<ExprConstChar> initList() {
        List<ExprConstChar> l = new ArrayList<ExprConstChar>();
        l.add(zero);
        return l;
    }

    private static Map<Character, ExprConstChar> charMap = initMap();
    private static List<ExprConstChar> charList = initList();

    public static void addMore() {
        int sz = charList.size();
        int q = 1;
        int bits = 0;
        BoundOptions bo = SketchOptions.getSingleton().bndOpts;
        int inbits = bo.inbits;
        int cbits = bo.cbits;
        while (q < sz || bits < inbits || bits < cbits) {
            q = q * 2;
            ++bits;
        }
        Random r = new Random();
        for (int i = sz; i < q; ++i) {
            char c1 = (char) ('a' + (char) r.nextInt(('z' - 'a') + 1));
            char c2 = (char) ('A' + (char) r.nextInt(('z' - 'a') + 1));
            if (!charMap.containsKey(c1)) {
                create(c1);
                continue;
            }
            if (!charMap.containsKey(c2)) {
                create(c2);
                continue;
            }
            for (int qq = 33; qq < 127; ++qq) {
                c2 = (char) qq;
                if (!charMap.containsKey(c2)) {
                    create(c2);
                    break;
                }
            }
        }
    }

    public static void renumber() {
        int sz = charList.size();
        System.out.println("Size = " + sz);
        if (sz < 127) {
            addMore();
        }
        if(sz < 32){
            assert charList.size() <= 32 : "This is strange; it should never happen!! " +
                    charList.size();
        }
        System.out.println("Size After= " + charList.size());
        Map<Character, ExprConstChar> tMap =
                new TreeMap<Character, ExprConstChar>(charMap);
        charList.clear();
        for (Entry<Character, ExprConstChar> ent : tMap.entrySet()) {
            int i = charList.size();
            ent.getValue().id = i;
            charList.add(ent.getValue());
        }
    }

    public static List<Expression> createFromString(String s) {
        assert s.charAt(0) == '\"';
        List<Expression> ecl = new ArrayList<Expression>(s.length() - 2);
        for (int i = 1; i < s.length() - 1; ++i) {
            char cc = s.charAt(i);
            if (cc != '\\') {
                ecl.add(create(cc));
            } else {
                i = i + 1;
                String ts = "\"\\" + s.charAt(i) + "\"";
                ecl.add(create(ts));
            }
        }
        ecl.add(zero);
        return ecl;
    }

    public static ExprConstChar createFromInt(int i) {
        return charList.get(i % charList.size());
    }

    public static ExprConstChar create(char c){
        if(charMap.containsKey(c)){
            return charMap.get(c);
        }else{
            int id = charList.size();
            ExprConstChar ecc = new ExprConstChar(id, c);
            charList.add(ecc);
            charMap.put(c, ecc);
            return ecc;
        }
    }

    public static ExprConstChar create(String s) {
        return create(readChar(s));
    }

    /** Create a new ExprConstChar for a particular character. */

    private ExprConstChar(int id, char val)
    {
        super((FENode) null);
        this.id = id;
        this.val = val;
    }

    public static char readChar(String str) {
        if (str.charAt(1) == '\\') {
            switch (str.charAt(2)) {
                case 'n':
                    return '\n';
                case 'r':
                    return '\r';
                case '0':
                    return 0;
                case '\\':
                    return '\\';
                case '\'':
                    return '\'';
            }
            return 0;
        } else {
            return str.charAt(1);
        }
    }




    /** Returns the value of this. */
    public int getId() {
        return id;
    }

    public String toString() {
        if (val == 0) {
            return "\'\\0\'";
        }
        if (val == '\n') {
            return "\'\\n\'";
        }
        if (val == '\r') {
            return "\'\\r\'";
        }
        if (val == '\'') {
            return "\'\\\'\'";
        }
        if (val == '\\') {
            return "\'\\\\\'";
        }
        String tmp = "\'" + val + "\'";

        return tmp;
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprConstChar(this);
    }
}
