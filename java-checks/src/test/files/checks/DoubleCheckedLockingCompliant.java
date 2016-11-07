class Compliant {

  private Helper helper = null;

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

}
