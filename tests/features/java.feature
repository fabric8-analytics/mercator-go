Feature: Test of Java handler

  Scenario: Succesfuly test jar file
    Given We have mercator installed
    When Scanning the jar file
    Then We have correct output
