/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.sun.org.apache.bcel.internal.classfile;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.org.apache.bcel.internal.Const;

/**
 * This class is derived from the abstract {@link Constant}
 * and represents a reference to a package.
 *
 * <p>Note: Early access Java 9 support- currently subject to change</p>
 *
 * @see     Constant
 * @since 6.1
 */
public final class ConstantPackage extends Constant implements ConstantObject {

    private int name_index;


    /**
     * Initialize from another object.
     */
    public ConstantPackage(final ConstantPackage c) {
        this(c.getNameIndex());
    }


    /**
     * Initialize instance from file data.
     *
     * @param file Input stream
     * @throws IOException
     */
    ConstantPackage(final DataInput file) throws IOException {
        this(file.readUnsignedShort());
    }


    /**
     * @param name_index Name index in constant pool.  Should refer to a
     * ConstantUtf8.
     */
    public ConstantPackage(final int name_index) {
        super(Const.CONSTANT_Package);
        this.name_index = name_index;
    }


    /**
     * Called by objects that are traversing the nodes of the tree implicitely
     * defined by the contents of a Java class. I.e., the hierarchy of methods,
     * fields, attributes, etc. spawns a tree of objects.
     *
     * @param v Visitor object
     */
    @Override
    public void accept( final Visitor v ) {
        v.visitConstantPackage(this);
    }


    /**
     * Dump constant package to file stream in binary format.
     *
     * @param file Output file stream
     * @throws IOException
     */
    @Override
    public final void dump( final DataOutputStream file ) throws IOException {
        file.writeByte(super.getTag());
        file.writeShort(name_index);
    }


    /**
     * @return Name index in constant pool of package name.
     */
    public final int getNameIndex() {
        return name_index;
    }


    /**
     * @param name_index the name index in the constant pool of this Constant Package
     */
    public final void setNameIndex( final int name_index ) {
        this.name_index = name_index;
    }


    /** @return String object
     */
    @Override
    public Object getConstantValue( final ConstantPool cp ) {
        final Constant c = cp.getConstant(name_index, Const.CONSTANT_Utf8);
        return ((ConstantUtf8) c).getBytes();
    }


    /** @return dereferenced string
     */
    public String getBytes( final ConstantPool cp ) {
        return (String) getConstantValue(cp);
    }


    /**
     * @return String representation.
     */
    @Override
    public final String toString() {
        return super.toString() + "(name_index = " + name_index + ")";
    }
}
