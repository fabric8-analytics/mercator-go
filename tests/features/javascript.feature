Feature: Test of Javascript handler

  Scenario: Succesfuly test package.json file
    Given We have mercator installed
    When Scanning the package.json file
    Then We have correct output
