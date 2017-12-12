Feature: Test of Gradle handler

  Scenario: Succesfuly test build.gradle file
    Given We have mercator installed
    When Scanning the build.gradle file
    Then We have correct output
