package jbasescript;

import java.io.File;
import jbase.example.ExampleHelper;
import jbasetestlanguage.ExampleJbasetestlanguage;

@SuppressWarnings("all")
public class ExampleJbasescript {
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
        ExampleJbasetestlanguage.print(_plus);
      } else {
        boolean _isDirectory = listOfFiles[i].isDirectory();
        if (_isDirectory) {
          String _aDir = helper.aDir();
          String _name_1 = listOfFiles[i].getName();
          String _plus_1 = (_aDir + _name_1);
          ExampleJbasetestlanguage.print(_plus_1);
        }
      }
    }
  }

  public static void main(String[] args) throws Throwable {
    int _length = args.length;
    boolean _greaterThan = (_length > 0);
    if (_greaterThan) {
      for (int i = 0; (i < args.length); ++i) {
        ExampleJbasescript.printFileNames(args[i]);
      }
    } else {
      ExampleJbasescript.printFileNames(".");
    }
  }
}
