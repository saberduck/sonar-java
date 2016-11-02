enum TooManyFieldsEnum { // Noncompliant [[sc=6;ec=23]] {{Refactor this class so it has no more than 2 fields, rather than the 3 it currently has.}}
  ENUM_CONSTANT_TREE { // Noncompliant [[sc=3;ec=4]] {{Refactor this class so it has no more than 2 fields, rather than the 3 it currently has.}}
     int field1;
     int field2;
     int field3;
  };
   int field1;
   int field2;
   int field3;
}

interface TooManyFieldsInterface { // Noncompliant [[sc=11;ec=33]] {{Refactor this class so it has no more than 2 fields, rather than the 3 it currently has.}}
   int field1;
   int field2;
   int field3;
}

class TooManyFieldsAnonymous {
  {
    new java.util.ArrayList<Integer>() { // Noncompliant [[sc=5]] {{Refactor this class so it has no more than 2 fields, rather than the 3 it currently has.}}
       int field1;
       int field2;
       int field3;
    };
  }
}
