
class Foo {
  private Helper helper = null;

  public Helper unrelatedNestedIfs() {
    if (helper == null) { // Noncompliant [[sc=5;ec=7;secondary=13]] {{Remove this dangerous instance of double-checked locking.}}
      if (sunIsUp) {
        doSomething();
      }
      synchronized (this) {
        if (sunIsDown) {
          doSomethingElse();
          if (helper == null)
            helper = new Helper();
        }
      }
    }
    return helper;
  }

  static class Helper {
    int field;
  }
}
