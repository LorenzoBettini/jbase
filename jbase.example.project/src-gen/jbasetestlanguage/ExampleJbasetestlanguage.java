package jbasetestlanguage;

import jbase.example.ExampleHelper;

@SuppressWarnings("all")
public class ExampleJbasetestlanguage {
  public static void print(String s) {
    ExampleHelper helper = new ExampleHelper();
    System.out.println(helper.toUpper(s));
  }
  
  public static void main(String[] args) throws Throwable {
    ExampleJbasetestlanguage.print("Hello World!");
  }
}
