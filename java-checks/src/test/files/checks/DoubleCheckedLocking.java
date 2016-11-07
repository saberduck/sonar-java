
class Foo {
  private Helper helper = null;

  public Helper classicCase() {
    if (helper == null) // Noncompliant [[sc=5;ec=23]] {{Remove this dangerous instance of double-checked locking.}}
      synchronized (this) {
        if (helper == null)
          helper = new Helper();
      }
    return helper;
  }

  public Helper fieldIsNotActuallyInitializedHere() {
    if (helper == null) // Compliant - field is not initialized in this method actually
      synchronized (this) {
        if (helper == null) {
          System.out.println("haha!"); // Nelson
        }
      }
    return helper;
  }

  public Helper memberSelectCondition() {
    if (this.helper == null) // Noncompliant [[sc=5;ec=28]] {{Remove this dangerous instance of double-checked locking.}}
      synchronized (this) {
        if (helper == null)
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper memberSelectCondition2() {
    if (helper == null) // Noncompliant [[sc=5;ec=23]] {{Remove this dangerous instance of double-checked locking.}}
      synchronized (this) {
        if (this.helper == null)
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper memberSelectCondition3() {
    if (this.helper == null) // Noncompliant [[sc=5;ec=28]] {{Remove this dangerous instance of double-checked locking.}}
      synchronized (this) {
        if (this.helper == null)
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper invertedConditions() {
    if (null == this.helper) // Noncompliant [[sc=5;ec=28]] {{Remove this dangerous instance of double-checked locking.}}
      synchronized (this) {
        if (null == helper)
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper intializationViaMemberSelect2() {
    if (helper == null) // Noncompliant [[sc=5;ec=23]] {{Remove this dangerous instance of double-checked locking.}}
      synchronized (this) {
        if (helper == null)
          this.helper = new Helper();
      }
    return helper;
  }

}

//after java 5 volatile keyword will guarantee correct read/write ordering with memory barriers
class VolatileFoo {
  private volatile Helper helper = null;

  public Helper classicCase() {
    if (helper == null) // Compliant because field is volatile
      synchronized (this) {
        if (helper == null)
          helper = new Helper();
      }
    return helper;
  }
}

class ImmutableFoo {
  private ImmutableHelper helper = null;

  public ImmutableHelper classicCase() {
    if (helper == null) // Compliant since Helper is effectively immutable
      synchronized (this) {
        if (helper == null)
          helper = new ImmutableHelper();
      }
    return helper;
  }
}

class Helper {

  int mutableField;

}

class ImmutableHelper {
  final int field;
}

class ColorSpace {

  public static final int CS_sRGB = 1000;

  public static ColorSpace getInstance(int colorspace) {
    ColorSpace theColorSpace;

    switch (colorspace) {
      case CS_sRGB:
        synchronized (ColorSpace.class) {
          if (sRGBspace == null) {
            ICC_Profile theProfile = ICC_Profile.getInstance(CS_sRGB);
            sRGBspace = new ICC_ColorSpace(theProfile);
          }

          theColorSpace = sRGBspace;
        }
        break;
    }
  }
}


