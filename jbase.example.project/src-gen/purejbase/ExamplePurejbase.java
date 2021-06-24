package purejbase;

import jbase.example.ExampleHelper;
import jbasetestlanguage.ExampleJbasetestlanguage;

@SuppressWarnings("all")
public class ExamplePurejbase {
  public static void main(String[] args) throws Throwable {
    ExampleHelper helper = new ExampleHelper();
    System.out.println(helper.toLower("Hello World!"));
    ExampleJbasetestlanguage.print("Hello World!");
  }
}
