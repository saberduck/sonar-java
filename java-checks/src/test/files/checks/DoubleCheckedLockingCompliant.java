class Compliant {

  private Helper helper = null;
  private int primitiveField = 0;

  public void notTheSameTest() {
    if (sunIsUp) {
      synchronized (this) {
        if (helper == null)
          helper = new Helper();
      }
    }
  }

  public void notTheField() {
    Helper helper = null;
    if (helper == null) {
      synchronized (this) {
        if (helper == null) {
          helper = new Helper();
        }
      }
    }
  }

  public void primitiveField() {
    if (primitiveField == null) {
      synchronized (this) {
        if (primitiveField == null) {
          primitiveField = 42;
        }
      }
    }
  }

  public void otherField() {
    if (helper == null) {
      synchronized (this) {
        if (primitiveField == null) {
          primitiveField = 42;
        }
      }
    }
  }


}


class StringResource {
  final String field;
}
