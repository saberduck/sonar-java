package org.sonar.java.se;

import org.sonar.java.se.symbolicvalues.SymbolicValue;

public class ProgramStateRenderer {

  public static void main(String[] args) {
    System.out.println(render(ProgramState.EMPTY_STATE));
  }

  public static String render(ProgramState programState) {
    StringBuilder sb = new StringBuilder();
    programState.constraints.forEach((sv, cst) -> {
      if (!SymbolicValue.PROTECTED_SYMBOLIC_VALUES.contains(sv)) {
        sb.append(sv).append("->[");
        cst.forEach((d, c) -> sb.append(c).append(" "));
        sb.append("] ");
      }
    });
    return sb.toString();
  }

}
