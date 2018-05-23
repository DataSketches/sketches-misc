/*
 * Copyright 2018, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc;

import org.testng.annotations.Test;

/**
 * @author Lee Rhodes
 */
public abstract class Grandparent {
  int size;

  Grandparent(int size) {
    this.size = size;
  }

  void print() {
    System.out.println("Grandparent");
  }
}


abstract class Parent extends Grandparent{

  Parent(int size) {
    super (size);
  }

//  @Override
//  void print() {
//    super.print();
//    System.out.println("Parent");
//  }
}


class Child extends Parent {

  Child(int size) {
    super(size);
  }

  @Override
  void print() {
    super.print();
    System.out.println("Child");
  }
}

class ChildPrint {

  @Test
  public void test() {
    Child c = new Child(1);
    c.print();
    System.out.println("Size: " + c.size);
  }
}
