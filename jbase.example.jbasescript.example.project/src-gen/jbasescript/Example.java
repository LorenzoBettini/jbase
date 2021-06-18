package jbasescript;

import java.io.File;
import jbase.example.jbasescript.example.project.ExampleHelper;

@SuppressWarnings("all")
public class Example {
  public static void printFileNames(String path) {
    File folder = new File(path);
    File[] listOfFiles = folder.listFiles();
    ExampleHelper helper = new ExampleHelper();
    for (int i = 0; (i < listOfFiles.length); i++) {
      boolean _isFile = listOfFiles[i].isFile();
      if (_isFile) {
        String _aFile = helper.aFile();
        String _name = listOfFiles[i].getName();
        String _plus = (_aFile + _name);
        System.out.println(_plus);
      } else {
        boolean _isDirectory = listOfFiles[i].isDirectory();
        if (_isDirectory) {
          String _aDir = helper.aDir();
          String _name_1 = listOfFiles[i].getName();
          String _plus_1 = (_aDir + _name_1);
          System.out.println(_plus_1);
        }
      }
    }
  }
  
  public static void main(String[] args) throws Throwable {
    int _length = args.length;
    boolean _greaterThan = (_length > 0);
    if (_greaterThan) {
      for (int i = 0; (i < args.length); ++i) {
        Example.printFileNames(args[i]);
      }
    } else {
      Example.printFileNames(".");
    }
  }
}
