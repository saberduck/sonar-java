#!/bin/bash

set -euo pipefail

rule_key=$1
rule_name=$2

java -Dline.separator=$'\n' -jar /c/local/rule-api-1.21.1.1127.jar generate -rule ${rule_key}

cat <<EOF > java-checks/src/main/java/org/sonar/java/checks/${rule_name}Check.java
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree;
import java.util.Collections;
import java.util.List;

@Rule(key = "${rule_key}")
public class ${rule_name}Check extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

}
EOF

cat <<EOF > java-checks/src/test/java/org/sonar/java/checks/${rule_name}CheckTest.java
package org.sonar.java.checks;

import org.junit.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

public class ${rule_name}CheckTest {

  @Test
  public void test() {
    JavaCheckVerifier.verify("src/test/files/checks/${rule_name}.java", new ${rule_name}Check());
  }

}
EOF


cat <<EOF > java-checks/src/test/files/checks/${rule_name}.java
package test;

public class ${rule_name} {

  public void foo() {

  }

}
EOF

perl -0777 -p -i -e "s/(getJavaChecks\(\) \{[^;]*\.)/\1add(${rule_name}Check\.class)\n      ./" java-checks/src/main/java/org/sonar/java/checks/CheckList.java

rm java-checks/src/main/java/org/sonar/java/checks/CheckList.java.bak

mvn license:format