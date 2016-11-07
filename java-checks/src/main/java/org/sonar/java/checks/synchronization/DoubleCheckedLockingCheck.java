/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks.synchronization;

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.sonar.plugins.java.api.tree.Tree.Kind.EQUAL_TO;
import static org.sonar.plugins.java.api.tree.Tree.Kind.IDENTIFIER;
import static org.sonar.plugins.java.api.tree.Tree.Kind.IF_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.MEMBER_SELECT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.NULL_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.SYNCHRONIZED_STATEMENT;

@Rule(key = "S2168")
public class DoubleCheckedLockingCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Remove this dangerous instance of double-checked locking.";

  private Deque<ExpressionTree> ifConditionStack = new LinkedList<>();
  private Deque<Tree> synchronizedStmtStack = new LinkedList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(IF_STATEMENT, SYNCHRONIZED_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(IF_STATEMENT)) {
      visitIfStatement((IfStatementTree) tree);
    }
    if (tree.is(SYNCHRONIZED_STATEMENT)) {
      synchronizedStmtStack.add(tree);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(IF_STATEMENT) && fieldNotInitializedTest(((IfStatementTree) tree).condition())) {
      ifConditionStack.pollLast();
    }
    if (tree.is(SYNCHRONIZED_STATEMENT)) {
      synchronizedStmtStack.pollLast();
    }
  }

  private void visitIfStatement(IfStatementTree ifTree) {
    if (!fieldNotInitializedTest(ifTree.condition())) {
      return;
    }
    ifConditionStack.add(ifTree.condition());
    if (!insideCriticalSection() || !isNestedIfStatement()) {
      return;
    }
    patternDetected(ifTree);
  }

  private boolean isNestedIfStatement() {
    return ifConditionStack.size() > 1;
  }

  /**
   * {@code
   * if (fieldInitializationTest) {
   *   synchronized {
   *     if (fieldInitializationTest) {
   *       ...
   *     }
   *   }
   * }
   * }
   *
   * @param nestedIf - if inside synchronized section
   */
  private void patternDetected(IfStatementTree nestedIf) {
    ExpressionTree parentIf = identicalConditionAlreadyOnStack(nestedIf.condition());
    if (parentIf == null) {
      return;
    }
    Symbol field = fieldFromEqCondition(nestedIf.condition());
    if (field == null) {
      return;
    }
    if (thenStmtInitializeField(nestedIf.thenStatement(), field) && !field.isVolatile() && !isImmutable(field)) {
      reportIssue(parentIf.parent(), parentIf, MESSAGE);
    }
  }

  /**
   * This is naive, however can avoid the FP in the simplest cases
   */
  private static boolean isImmutable(Symbol field) {
    Type fieldType = field.type();
    if (!fieldType.isClass()) {
      return false;
    }
    Collection<Symbol> members = field.type().symbol().memberSymbols();
    Optional<Symbol> nonFinalField = members.stream()
      .filter(symbol -> symbol.isVariableSymbol() && symbol.type().isPrimitive() && !symbol.isFinal())
      .findAny();
    return !nonFinalField.isPresent();
  }

  private static boolean thenStmtInitializeField(StatementTree statementTree, Symbol field) {
    AssignmentVisitor visitor = new AssignmentVisitor(field);
    statementTree.accept(visitor);
    return visitor.assignmentToField;
  }

  private static Symbol fieldFromEqCondition(ExpressionTree condition) {
    if (!condition.is(EQUAL_TO)) {
      return null;
    }
    BinaryExpressionTree eqRelation = (BinaryExpressionTree) condition;
    if (isField(eqRelation.leftOperand())) {
      return symbolFromVariable(eqRelation.leftOperand());
    }
    if (isField(eqRelation.rightOperand())) {
      return symbolFromVariable(eqRelation.rightOperand());
    }
    return null;
  }

  @CheckForNull
  private ExpressionTree identicalConditionAlreadyOnStack(ExpressionTree nestedCondition) {

    for (ExpressionTree parentCondition : ifConditionStack) {
      Symbol symbol1 = fieldFromEqCondition(parentCondition);
      Symbol symbol2 = fieldFromEqCondition(nestedCondition);
      if (symbol1 == symbol2) {
        return parentCondition;
      }
    }
    return null;
  }

  private boolean insideCriticalSection() {
    return !synchronizedStmtStack.isEmpty();
  }

  private static boolean fieldNotInitializedTest(ExpressionTree condition) {
    if (!condition.is(EQUAL_TO)) {
      return false;
    }
    BinaryExpressionTree eqRelation = (BinaryExpressionTree) condition;
    if (isField(eqRelation.leftOperand()) && eqRelation.rightOperand().is(NULL_LITERAL)) {
      return true;
    }
    if (isField(eqRelation.rightOperand()) && eqRelation.leftOperand().is(NULL_LITERAL)) {
      return true;
    }
    return false;
  }

  private static boolean isField(ExpressionTree expressionTree) {
    Symbol symbol = symbolFromVariable(expressionTree);
    if (symbol == null) {
      return false;
    }
    return symbol.isVariableSymbol() && symbol.owner().isTypeSymbol();
  }

  private static class AssignmentVisitor extends BaseTreeVisitor {

    private boolean assignmentToField;
    private Symbol field;

    AssignmentVisitor(Symbol field) {
      this.field = field;
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree assignmentTree) {
      ExpressionTree variable = assignmentTree.variable();
      Symbol symbol = symbolFromVariable(variable);
      if (field == symbol) {
        assignmentToField = true;
      }
    }
  }

  @CheckForNull
  private static Symbol symbolFromVariable(ExpressionTree variable) {
    if (variable.is(IDENTIFIER)) {
      return ((IdentifierTree) variable).symbol();
    }
    if (variable.is(MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) variable).identifier().symbol();
    }
    return null;
  }

}
