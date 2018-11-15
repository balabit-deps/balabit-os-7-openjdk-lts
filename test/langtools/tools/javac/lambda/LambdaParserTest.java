/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 7115050 8003280 8005852 8006694 8129962
 * @summary Add lambda tests
 *  Add parser support for lambda expressions
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper

 * @run main LambdaParserTest
 */

import java.io.IOException;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

public class LambdaParserTest extends ComboInstance<LambdaParserTest> {

    enum LambdaKind implements ComboParameter {
        NILARY_EXPR("()->x"),
        NILARY_STMT("()->{ return x; }"),
        ONEARY_SHORT_EXPR("#{NAME}->x"),
        ONEARY_SHORT_STMT("#{NAME}->{ return x; }"),
        ONEARY_EXPR("(#{MOD[0]} #{TYPE[0]} #{NAME})->x"),
        ONEARY_STMT("(#{MOD[0]} #{TYPE[0]} #{NAME})->{ return x; }"),
        TWOARY_EXPR("(#{MOD[0]} #{TYPE[0]} #{NAME}, #{MOD[1]} #{TYPE[1]} y)->x"),
        TWOARY_STMT("(#{MOD[0]} #{TYPE[0]} #{NAME}, #{MOD[1]} #{TYPE[1]} y)->{ return x; }");

        String lambdaTemplate;

        LambdaKind(String lambdaTemplate) {
            this.lambdaTemplate = lambdaTemplate;
        }

        @Override
        public String expand(String optParameter) {
            return lambdaTemplate;
        }

        int arity() {
            switch (this) {
                case NILARY_EXPR:
                case NILARY_STMT: return 0;
                case ONEARY_SHORT_EXPR:
                case ONEARY_SHORT_STMT:
                case ONEARY_EXPR:
                case ONEARY_STMT: return 1;
                case TWOARY_EXPR:
                case TWOARY_STMT: return 2;
                default: throw new AssertionError("Invalid lambda kind " + this);
            }
        }

        boolean isShort() {
            return this == ONEARY_SHORT_EXPR ||
                    this == ONEARY_SHORT_STMT;
        }
    }

    enum LambdaParameterName implements ComboParameter {
        IDENT("x"),
        UNDERSCORE("_");

        String nameStr;

        LambdaParameterName(String nameStr) {
            this.nameStr = nameStr;
        }

        @Override
        public String expand(String optParameter) {
            return nameStr;
        }
    }

    enum LambdaParameterKind implements ComboParameter {
        IMPLICIT(""),
        EXPLIICT_SIMPLE("A"),
        EXPLIICT_SIMPLE_ARR1("A[]"),
        EXPLIICT_SIMPLE_ARR2("A[][]"),
        EXPLICIT_VARARGS("A..."),
        EXPLICIT_GENERIC1("A<X>"),
        EXPLICIT_GENERIC2("A<? extends X, ? super Y>"),
        EXPLICIT_GENERIC2_VARARGS("A<? extends X, ? super Y>..."),
        EXPLICIT_GENERIC2_ARR1("A<? extends X, ? super Y>[]"),
        EXPLICIT_GENERIC2_ARR2("A<? extends X, ? super Y>[][]");

        String parameterType;

        LambdaParameterKind(String parameterType) {
            this.parameterType = parameterType;
        }

        boolean explicit() {
            return this != IMPLICIT;
        }

        boolean isVarargs() {
            return this == EXPLICIT_VARARGS ||
                    this == EXPLICIT_GENERIC2_VARARGS;
        }

        @Override
        public String expand(String optParameter) {
            return parameterType;
        }
    }

    enum ModifierKind implements ComboParameter {
        NONE(""),
        FINAL("final"),
        PUBLIC("public");

        String modifier;

        ModifierKind(String modifier) {
            this.modifier = modifier;
        }

        boolean compatibleWith(LambdaParameterKind pk) {
            switch (this) {
                case PUBLIC: return false;
                case FINAL: return pk != LambdaParameterKind.IMPLICIT;
                case NONE: return true;
                default: throw new AssertionError("Invalid modifier kind " + this);
            }
        }

        @Override
        public String expand(String optParameter) {
            return modifier;
        }
    }

    enum ExprKind implements ComboParameter {
        NONE("#{LAMBDA}#{SUBEXPR}"),
        SINGLE_PAREN1("(#{LAMBDA}#{SUBEXPR})"),
        SINGLE_PAREN2("(#{LAMBDA})#{SUBEXPR}"),
        DOUBLE_PAREN1("((#{LAMBDA}#{SUBEXPR}))"),
        DOUBLE_PAREN2("((#{LAMBDA})#{SUBEXPR})"),
        DOUBLE_PAREN3("((#{LAMBDA}))#{SUBEXPR}");

        String expressionTemplate;

        ExprKind(String expressionTemplate) {
            this.expressionTemplate = expressionTemplate;
        }

        @Override
        public String expand(String optParameter) {
            return expressionTemplate;
        }
    }

    enum SubExprKind implements ComboParameter {
        NONE(""),
        SELECT_FIELD(".f"),
        SELECT_METHOD(".f()"),
        SELECT_NEW(".new Foo()"),
        POSTINC("++"),
        POSTDEC("--");

        String subExpression;

        SubExprKind(String subExpression) {
            this.subExpression = subExpression;
        }

        @Override
        public String expand(String optParameter) {
            return subExpression;
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<LambdaParserTest>()
                .withFilter(LambdaParserTest::redundantTestFilter)
                .withFilter(LambdaParserTest::badImplicitFilter)
                .withDimension("LAMBDA", (x, lk) -> x.lk = lk, LambdaKind.values())
                .withDimension("NAME", (x, name) -> x.pn = name, LambdaParameterName.values())
                .withArrayDimension("TYPE", (x, type, idx) -> x.pks[idx] = type, 2, LambdaParameterKind.values())
                .withArrayDimension("MOD", (x, mod, idx) -> x.mks[idx] = mod, 2, ModifierKind.values())
                .withDimension("EXPR", ExprKind.values())
                .withDimension("SUBEXPR", SubExprKind.values())
                .run(LambdaParserTest::new);
    }

    LambdaParameterKind[] pks = new LambdaParameterKind[2];
    ModifierKind[] mks = new ModifierKind[2];
    LambdaKind lk;
    LambdaParameterName pn;

    boolean badImplicitFilter() {
        return !(mks[0] != ModifierKind.NONE && lk.isShort());
    }

    boolean redundantTestFilter() {
        for (int i = lk.arity(); i < mks.length ; i++) {
            if (mks[i].ordinal() != 0) {
                return false;
            }
        }
        for (int i = lk.arity(); i < pks.length ; i++) {
            if (pks[i].ordinal() != 0) {
                return false;
            }
        }
        return true;
    }

    String template = "class Test {\n" +
                      "   SAM s = #{EXPR};\n" +
                      "}";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withSourceFromTemplate(template)
                .parse(this::check);
    }

    void check(Result<?> res) {
        boolean errorExpected = (lk.arity() > 0 && !mks[0].compatibleWith(pks[0])) ||
                (lk.arity() > 1 && !mks[1].compatibleWith(pks[1]));

        if (lk.arity() == 2 &&
                (pks[0].explicit() != pks[1].explicit() ||
                pks[0].isVarargs())) {
            errorExpected = true;
        }

        errorExpected |= pn == LambdaParameterName.UNDERSCORE &&
                lk.arity() > 0;

        if (errorExpected != res.hasErrors()) {
            fail("invalid diagnostics for source:\n" +
                res.compilationInfo() +
                "\nFound error: " + res.hasErrors() +
                "\nExpected error: " + errorExpected);
        }
    }
}