package jbasescript;

import jbase.example.client.maven.Helper;

@SuppressWarnings("all")
public class JbasescriptExample {
  public static String testMe(String s) {
    return JbasescriptExample.process(s);
  }
  
  public static String process(String s) {
    String _xjconditionalexpression = null;
    if ((s == null)) {
      _xjconditionalexpression = "null";
    } else {
      _xjconditionalexpression = Helper.helpMe(s);
    }
    return ("passed: " + _xjconditionalexpression);
  }
  
  public static void main(String[] args) throws Throwable {
  }
}
